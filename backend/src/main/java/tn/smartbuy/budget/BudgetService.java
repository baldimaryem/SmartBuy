package tn.smartbuy.budget;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.budget.BudgetDtos.BudgetDto;
import tn.smartbuy.budget.BudgetDtos.BudgetRequete;
import tn.smartbuy.commun.AccesInterditException;
import tn.smartbuy.commun.Montants;
import tn.smartbuy.commun.Mois;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.commun.RessourceIntrouvableException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Budgets mensuels global et par catégorie (BUD-08, BUD-09, BUD-10). */
@Service
public class BudgetService {

    private final BudgetRepository budgets;
    private final OperationRepository operations;
    private final CategorieService categorieService;

    public BudgetService(BudgetRepository budgets, OperationRepository operations, CategorieService categorieService) {
        this.budgets = budgets;
        this.operations = operations;
        this.categorieService = categorieService;
    }

    /** BUD-10 : budgets du mois avec dépensé / reste / pourcentage ; le budget global en premier. */
    @Transactional(readOnly = true)
    public List<BudgetDto> lister(Long utilisateurId, YearMonth mois) {
        List<Operation> duMois = operations.findByUtilisateurIdAndDateOperationBetweenOrderByDateOperationDescIdDesc(
                utilisateurId, mois.atDay(1), mois.atEndOfMonth());
        return budgets.findByUtilisateurIdAndMois(utilisateurId, mois).stream()
                .sorted(Comparator.comparing((Budget b) -> !b.estGlobal())
                        .thenComparing(b -> b.estGlobal() ? "" : b.getCategorie().getNom()))
                .map(b -> BudgetDto.depuis(b, depense(b, duMois)))
                .toList();
    }

    /** BUD-08 / BUD-09 : crée le budget, ou met à jour son plafond s'il existe déjà pour ce mois. */
    @Transactional
    public BudgetDto enregistrer(Long utilisateurId, BudgetRequete requete) {
        YearMonth mois = Mois.lire(requete.mois());
        Categorie categorie = null;
        if (requete.categorieId() != null) {
            categorie = categorieService.chargerVisible(utilisateurId, requete.categorieId());
            if (categorie.getType() != TypeOperation.DEPENSE) {
                throw new RequeteInvalideException("Un budget ne peut porter que sur une catégorie de dépense");
            }
        }
        Optional<Budget> existant = categorie == null
                ? budgets.findByUtilisateurIdAndMoisAndCategorieIsNull(utilisateurId, mois)
                : budgets.findByUtilisateurIdAndMoisAndCategorieId(utilisateurId, mois, categorie.getId());

        Budget b = existant.orElseGet(Budget::new);
        b.setUtilisateurId(utilisateurId);
        b.setMois(mois);
        b.setCategorie(categorie);
        b.setMontantPlafond(Montants.depuisDouble(requete.montantPlafond()));
        budgets.save(b);

        List<Operation> duMois = operations.findByUtilisateurIdAndDateOperationBetweenOrderByDateOperationDescIdDesc(
                utilisateurId, mois.atDay(1), mois.atEndOfMonth());
        return BudgetDto.depuis(b, depense(b, duMois));
    }

    @Transactional
    public void supprimer(Long utilisateurId, Long id) {
        Budget b = budgets.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Budget introuvable"));
        if (!b.getUtilisateurId().equals(utilisateurId)) {
            throw new AccesInterditException("Ce budget ne vous appartient pas");
        }
        budgets.delete(b);
    }

    /**
     * LNK-01 : reste du budget d'une catégorie pour un mois (plafond − dépensé), vide s'il n'y a pas de budget.
     * Peut être négatif si le budget est déjà dépassé.
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> resteCategorie(Long utilisateurId, YearMonth mois, Long categorieId) {
        return budgets.findByUtilisateurIdAndMoisAndCategorieId(utilisateurId, mois, categorieId)
                .map(b -> b.getMontantPlafond().subtract(operations.totalDepensesCategorie(
                        utilisateurId, categorieId, mois.atDay(1), mois.atEndOfMonth())));
    }

    @Transactional(readOnly = true)
    public Optional<Budget> budgetGlobal(Long utilisateurId, YearMonth mois) {
        return budgets.findByUtilisateurIdAndMoisAndCategorieIsNull(utilisateurId, mois);
    }

    /** Dépensé pour un budget : toutes les dépenses (global) ou celles de sa catégorie. */
    static BigDecimal depense(Budget b, List<Operation> operationsDuMois) {
        return operationsDuMois.stream()
                .filter(o -> o.getType() == TypeOperation.DEPENSE)
                .filter(o -> b.estGlobal() || o.getCategorie().getId().equals(b.getCategorie().getId()))
                .map(Operation::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
