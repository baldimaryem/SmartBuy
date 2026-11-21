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
import tn.smartbuy.budget.BudgetDtos.CategorieDto;
import tn.smartbuy.budget.BudgetDtos.CategorieRequete;
import tn.smartbuy.securite.UtilisateurConnecte;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
    }

    @GetMapping
    public List<CategorieDto> lister(@AuthenticationPrincipal UtilisateurConnecte moi,
                                     @RequestParam(required = false) TypeOperation type) {
        return categorieService.lister(moi.id(), type);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategorieDto creer(@AuthenticationPrincipal UtilisateurConnecte moi,
                              @Valid @RequestBody CategorieRequete requete) {
        return categorieService.creer(moi.id(), requete);
    }

    @PutMapping("/{id}")
    public CategorieDto modifier(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id,
                                 @Valid @RequestBody CategorieRequete requete) {
        return categorieService.modifier(moi.id(), id, requete);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        categorieService.supprimer(moi.id(), id);
    }
}
