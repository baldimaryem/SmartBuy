package tn.smartbuy.ia;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.auth.Utilisateur;
import tn.smartbuy.auth.UtilisateurRepository;
import tn.smartbuy.budget.Budget;
import tn.smartbuy.budget.BudgetRepository;
import tn.smartbuy.budget.CalculsBudget;
import tn.smartbuy.budget.Operation;
import tn.smartbuy.budget.OperationRepository;
import tn.smartbuy.budget.TypeOperation;
import tn.smartbuy.commun.Montants;
import tn.smartbuy.commun.RessourceIntrouvableException;
import tn.smartbuy.ia.ResumeMois.DepenseCategorie;
import tn.smartbuy.prix.PrixActuels;
import tn.smartbuy.prix.PrixActuels.MagasinMoinsCher;
import tn.smartbuy.prix.PrixService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Construit le {@link ResumeMois} utilisé par les deux conseillers (règles et IA). */
@Service
public class ResumeMoisService {

    /** Nombre de mois précédents utilisés pour la moyenne de comparaison. */
    static final int MOIS_HISTORIQUE = 3;

    private final OperationRepository operations;
    private final BudgetRepository budgets;
    private final UtilisateurRepository utilisateurs;
    private final PrixService prixService;

    public ResumeMoisService(OperationRepository operations, BudgetRepository budgets,
                             UtilisateurRepository utilisateurs, PrixService prixService) {
        this.operations = operations;
        this.budgets = budgets;
        this.utilisateurs = utilisateurs;
        this.prixService = prixService;
    }

    @Transactional(readOnly = true)
    public ResumeMois construire(Long utilisateurId, YearMonth mois) {
        Utilisateur u = utilisateurs.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));

        YearMonth debutHistorique = mois.minusMonths(MOIS_HISTORIQUE);
        List<Operation> toutes = operations.findByUtilisateurIdAndDateOperationBetweenOrderByDateOperationDescIdDesc(
                utilisateurId, debutHistorique.atDay(1), mois.atEndOfMonth());
        List<Operation> duMois = toutes.stream().filter(o -> YearMonth.from(o.getDateOperation()).equals(mois)).toList();
        List<Operation> avant = toutes.stream().filter(o -> YearMonth.from(o.getDateOperation()).isBefore(mois)).toList();

        BigDecimal depenses = CalculsBudget.total(duMois, TypeOperation.DEPENSE);
        List<Budget> budgetsDuMois = budgets.findByUtilisateurIdAndMois(utilisateurId, mois);
        BigDecimal budgetGlobal = budgetsDuMois.stream().filter(Budget::estGlobal)
                .map(Budget::getMontantPlafond).findFirst().orElse(null);
        var prevision = CalculsBudget.prevoir(depenses, mois, LocalDate.now(), budgetGlobal);
        MagasinMoinsCher magasin = PrixActuels.magasinLePlusSouventMoinsCher(prixService.tousLesPrixActuels())
                .orElse(null);

        return new ResumeMois(
                mois.toString(),
                u.getDevise(),
                mois.equals(YearMonth.now()),
                Montants.versDouble(CalculsBudget.total(duMois, TypeOperation.REVENU)),
                Montants.versDouble(depenses),
                Montants.versDouble(u.getRevenuMensuel()),
                Montants.versDouble(budgetGlobal),
                prevision.depenseEstimeeFinMois(),
                Montants.versDouble(CalculsBudget.economies(duMois)),
                parCategorie(duMois, avant, budgetsDuMois),
                magasin == null ? null : magasin.magasinNom(),
                magasin == null ? 0 : magasin.nbProduits(),
                magasin == null ? 0 : magasin.nbProduitsCompares());
    }

    /** Dépenses du mois par catégorie + moyenne des 3 mois précédents + budget éventuel. */
    private static List<DepenseCategorie> parCategorie(List<Operation> duMois, List<Operation> avant,
                                                       List<Budget> budgetsDuMois) {
        Map<String, BigDecimal> moisCourant = sommeParCategorie(duMois);
        Map<String, BigDecimal> historique = sommeParCategorie(avant);
        Map<String, BigDecimal> plafonds = budgetsDuMois.stream()
                .filter(b -> !b.estGlobal())
                .collect(Collectors.toMap(b -> b.getCategorie().getNom(), Budget::getMontantPlafond, (a, b) -> a));

        // On prend toutes les catégories qui ont une dépense ce mois-ci OU un budget
        Map<String, Boolean> noms = new LinkedHashMap<>();
        moisCourant.keySet().forEach(n -> noms.put(n, true));
        plafonds.keySet().forEach(n -> noms.put(n, true));

        return noms.keySet().stream()
                .map(nom -> new DepenseCategorie(
                        nom,
                        Montants.versDouble(moisCourant.getOrDefault(nom, BigDecimal.ZERO)),
                        Montants.versDouble(historique.getOrDefault(nom, BigDecimal.ZERO)
                                .divide(BigDecimal.valueOf(MOIS_HISTORIQUE), Montants.DECIMALES, RoundingMode.HALF_UP)),
                        Montants.versDouble(plafonds.get(nom))))
                .sorted(Comparator.comparingDouble(DepenseCategorie::montant).reversed())
                .toList();
    }

    private static Map<String, BigDecimal> sommeParCategorie(List<Operation> ops) {
        return ops.stream()
                .filter(o -> o.getType() == TypeOperation.DEPENSE)
                .filter(o -> Objects.nonNull(o.getCategorie()))
                .collect(Collectors.toMap(o -> o.getCategorie().getNom(), Operation::getMontant, BigDecimal::add,
                        LinkedHashMap::new));
    }
}
