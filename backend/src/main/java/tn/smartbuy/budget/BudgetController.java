package tn.smartbuy.budget;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.budget.BudgetDtos.BudgetDto;
import tn.smartbuy.budget.BudgetDtos.BudgetRequete;
import tn.smartbuy.budget.BudgetDtos.DashboardDto;
import tn.smartbuy.commun.Mois;
import tn.smartbuy.securite.UtilisateurConnecte;

import java.util.List;

/** Routes /api/budgets et /api/dashboard (le mois courant est utilisé si « mois » est absent). */
@RestController
@RequestMapping("/api")
public class BudgetController {

    private final BudgetService budgetService;
    private final DashboardService dashboardService;

    public BudgetController(BudgetService budgetService, DashboardService dashboardService) {
        this.budgetService = budgetService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/budgets")
    public List<BudgetDto> lister(@AuthenticationPrincipal UtilisateurConnecte moi,
                                  @RequestParam(required = false) String mois) {
        return budgetService.lister(moi.id(), Mois.lireOuCourant(mois));
    }

    /** Crée ou met à jour (réponse 200 dans les deux cas). */
    @PostMapping("/budgets")
    public BudgetDto enregistrer(@AuthenticationPrincipal UtilisateurConnecte moi,
                                 @Valid @RequestBody BudgetRequete requete) {
        return budgetService.enregistrer(moi.id(), requete);
    }

    @DeleteMapping("/budgets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        budgetService.supprimer(moi.id(), id);
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard(@AuthenticationPrincipal UtilisateurConnecte moi,
                                  @RequestParam(required = false) String mois) {
        return dashboardService.tableauDeBord(moi.id(), Mois.lireOuCourant(mois));
    }
}
