package tn.smartbuy.budget;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.budget.BudgetDtos.OperationDto;
import tn.smartbuy.budget.BudgetDtos.OperationRequete;
import tn.smartbuy.commun.Mois;
import tn.smartbuy.securite.UtilisateurConnecte;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    /** GET /api/operations?mois=2026-11&type=DEPENSE&categorieId=1 (tous les filtres sont facultatifs). */
    @GetMapping
    public List<OperationDto> lister(@AuthenticationPrincipal UtilisateurConnecte moi,
                                     @RequestParam(required = false) String mois,
                                     @RequestParam(required = false) TypeOperation type,
                                     @RequestParam(required = false) Long categorieId) {
        var moisFiltre = mois == null || mois.isBlank() ? null : Mois.lire(mois);
        return operationService.lister(moi.id(), moisFiltre, type, categorieId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperationDto creer(@AuthenticationPrincipal UtilisateurConnecte moi,
                              @Valid @RequestBody OperationRequete requete) {
        return operationService.creer(moi.id(), requete);
    }

    @PutMapping("/{id}")
    public OperationDto modifier(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id,
                                 @Valid @RequestBody OperationRequete requete) {
        return operationService.modifier(moi.id(), id, requete);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        operationService.supprimer(moi.id(), id);
    }
}
