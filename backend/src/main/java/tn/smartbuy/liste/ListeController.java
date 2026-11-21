package tn.smartbuy.liste;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.budget.BudgetDtos.OperationDto;
import tn.smartbuy.liste.ListeDtos.AchatRequete;
import tn.smartbuy.liste.ListeDtos.ComparaisonDto;
import tn.smartbuy.liste.ListeDtos.LigneRequete;
import tn.smartbuy.liste.ListeDtos.ListeDetailDto;
import tn.smartbuy.liste.ListeDtos.ListeRequete;
import tn.smartbuy.liste.ListeDtos.ListeResumeDto;
import tn.smartbuy.liste.ListeDtos.QuantiteRequete;
import tn.smartbuy.securite.UtilisateurConnecte;

import java.util.List;

@RestController
@RequestMapping("/api/listes")
public class ListeController {

    private final ListeService listeService;

    public ListeController(ListeService listeService) {
        this.listeService = listeService;
    }

    @GetMapping
    public List<ListeResumeDto> lister(@AuthenticationPrincipal UtilisateurConnecte moi) {
        return listeService.lister(moi.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListeResumeDto creer(@AuthenticationPrincipal UtilisateurConnecte moi,
                                @Valid @RequestBody ListeRequete requete) {
        return listeService.creer(moi.id(), requete.nom(), requete.univers());
    }

    @PutMapping("/{id}")
    public ListeResumeDto renommer(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id,
                                   @Valid @RequestBody ListeRequete requete) {
        return listeService.renommer(moi.id(), id, requete.nom());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        listeService.supprimer(moi.id(), id);
    }

    @GetMapping("/{id}")
    public ListeDetailDto detail(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        return listeService.detail(moi.id(), id);
    }

    @PostMapping("/{id}/lignes")
    public ListeDetailDto ajouterLigne(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id,
                                       @Valid @RequestBody LigneRequete requete) {
        return listeService.ajouterLigne(moi.id(), id, requete);
    }

    @PutMapping("/{id}/lignes/{ligneId}")
    public ListeDetailDto modifierLigne(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id,
                                        @PathVariable Long ligneId, @Valid @RequestBody QuantiteRequete requete) {
        return listeService.modifierLigne(moi.id(), id, ligneId, requete.quantite());
    }

    /** Renvoie la liste détaillée (et non 204), comme indiqué dans le contrat. */
    @DeleteMapping("/{id}/lignes/{ligneId}")
    public ListeDetailDto supprimerLigne(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id,
                                         @PathVariable Long ligneId) {
        return listeService.supprimerLigne(moi.id(), id, ligneId);
    }

    @GetMapping("/{id}/comparaison")
    public ComparaisonDto comparer(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        return listeService.comparer(moi.id(), id);
    }

    @PostMapping("/{id}/acheter")
    @ResponseStatus(HttpStatus.CREATED)
    public OperationDto acheter(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id,
                                @Valid @RequestBody AchatRequete requete) {
        return listeService.acheter(moi.id(), id, requete.magasinId());
    }
}
