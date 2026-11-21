package tn.smartbuy.budget;

import tn.smartbuy.budget.BudgetDtos.EvolutionDto;
import tn.smartbuy.budget.BudgetDtos.PrevisionDto;
import tn.smartbuy.budget.BudgetDtos.RepartitionDto;
import tn.smartbuy.commun.Montants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculs du tableau de bord, sans base de données : on leur donne des opérations,
 * ils renvoient des chiffres. C'est ce qui permet de les tester facilement (CalculsBudgetTest).
 */
public final class CalculsBudget {

    private CalculsBudget() {
    }

    /** Somme des montants d'un type (DEPENSE ou REVENU). */
    public static BigDecimal total(Collection<Operation> operations, TypeOperation type) {
        return operations.stream()
                .filter(o -> o.getType() == type)
                .map(Operation::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** LNK-03 : somme des économies réalisées grâce à la comparaison des prix. */
    public static BigDecimal economies(Collection<Operation> operations) {
        return operations.stream()
                .map(o -> Montants.nonNul(o.getEconomie()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * BUD-15 : prévision des dépenses de fin de mois.
     * <ul>
     *   <li>mois en cours : dépensé ÷ jours écoulés × nombre de jours du mois (extrapolation du rythme actuel) ;</li>
     *   <li>mois passé (ou futur) : le total réel, il n'y a rien à extrapoler.</li>
     * </ul>
     * depassementProbable = prévision &gt; budget global (faux s'il n'y a pas de budget global).
     */
    public static PrevisionDto prevoir(BigDecimal depense, YearMonth mois, LocalDate aujourdhui, BigDecimal budgetGlobal) {
        BigDecimal estimation = depense;
        if (mois.equals(YearMonth.from(aujourdhui))) {
            int joursEcoules = aujourdhui.getDayOfMonth();
            int joursDuMois = mois.lengthOfMonth();
            estimation = depense.multiply(BigDecimal.valueOf(joursDuMois))
                    .divide(BigDecimal.valueOf(joursEcoules), Montants.DECIMALES, RoundingMode.HALF_UP);
        }
        boolean depassement = budgetGlobal != null && estimation.compareTo(budgetGlobal) > 0;
        return new PrevisionDto(Montants.versDouble(estimation), depassement);
    }

    /** BUD-13 : dépenses du mois regroupées par catégorie, de la plus grosse à la plus petite. */
    public static List<RepartitionDto> repartition(Collection<Operation> operationsDuMois) {
        Map<Long, BigDecimal> montants = new LinkedHashMap<>();
        Map<Long, String> noms = new LinkedHashMap<>();
        for (Operation o : operationsDuMois) {
            if (o.getType() != TypeOperation.DEPENSE) {
                continue;
            }
            Long id = o.getCategorie().getId();
            montants.merge(id, o.getMontant(), BigDecimal::add);
            noms.put(id, o.getCategorie().getNom());
        }
        return montants.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .map(e -> new RepartitionDto(e.getKey(), noms.get(e.getKey()), Montants.versDouble(e.getValue())))
                .toList();
    }

    /**
     * BUD-14 : dépenses et revenus des {@code nbMois} derniers mois (jusqu'à {@code dernierMois} inclus),
     * du plus ancien au plus récent. Un mois sans opération vaut 0.
     */
    public static List<EvolutionDto> evolution(Collection<Operation> operations, YearMonth dernierMois, int nbMois) {
        List<EvolutionDto> points = new ArrayList<>();
        for (int i = nbMois - 1; i >= 0; i--) {
            YearMonth m = dernierMois.minusMonths(i);
            List<Operation> duMois = operations.stream()
                    .filter(o -> YearMonth.from(o.getDateOperation()).equals(m))
                    .toList();
            points.add(new EvolutionDto(m.toString(),
                    Montants.versDouble(total(duMois, TypeOperation.DEPENSE)),
                    Montants.versDouble(total(duMois, TypeOperation.REVENU))));
        }
        return points;
    }
}
