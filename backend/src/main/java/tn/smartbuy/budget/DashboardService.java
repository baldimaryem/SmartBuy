package tn.smartbuy.budget;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.alerte.AlerteService;
import tn.smartbuy.budget.BudgetDtos.DashboardDto;
import tn.smartbuy.budget.BudgetDtos.PrevisionDto;
import tn.smartbuy.commun.Montants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Tableau de bord de l'accueil (BUD-10, BUD-13, BUD-14, BUD-15, LNK-03). */
@Service
public class DashboardService {

    /** BUD-14 : nombre de mois affichés dans le graphique d'évolution. */
    static final int MOIS_EVOLUTION = 6;

    private final OperationRepository operations;
    private final BudgetService budgetService;
    private final AlerteService alerteService;

    public DashboardService(OperationRepository operations, BudgetService budgetService, AlerteService alerteService) {
        this.operations = operations;
        this.budgetService = budgetService;
        this.alerteService = alerteService;
    }

    @Transactional(readOnly = true)
    public DashboardDto tableauDeBord(Long utilisateurId, YearMonth mois) {
        // Une seule requête pour les 6 derniers mois ; le mois demandé en est extrait en mémoire
        YearMonth premierMois = mois.minusMonths(MOIS_EVOLUTION - 1L);
        List<Operation> sixMois = operations.findByUtilisateurIdAndDateOperationBetweenOrderByDateOperationDescIdDesc(
                utilisateurId, premierMois.atDay(1), mois.atEndOfMonth());
        List<Operation> duMois = sixMois.stream()
                .filter(o -> YearMonth.from(o.getDateOperation()).equals(mois))
                .toList();

        BigDecimal revenus = CalculsBudget.total(duMois, TypeOperation.REVENU);
        BigDecimal depenses = CalculsBudget.total(duMois, TypeOperation.DEPENSE);
        BigDecimal budgetGlobal = budgetService.budgetGlobal(utilisateurId, mois)
                .map(Budget::getMontantPlafond)
                .orElse(null);
        BigDecimal reste = budgetGlobal == null ? null : budgetGlobal.subtract(depenses);
        PrevisionDto prevision = CalculsBudget.prevoir(depenses, mois, LocalDate.now(), budgetGlobal);

        return new DashboardDto(
                mois.toString(),
                Montants.versDouble(revenus),
                Montants.versDouble(depenses),
                Montants.versDouble(revenus.subtract(depenses)),
                Montants.versDouble(budgetGlobal),
                Montants.versDouble(reste),
                prevision,
                CalculsBudget.repartition(duMois),
                CalculsBudget.evolution(sixMois, mois, MOIS_EVOLUTION),
                Montants.versDouble(CalculsBudget.economies(duMois)),
                alerteService.nombreNonLues(utilisateurId));
    }
}
