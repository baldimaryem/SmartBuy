package tn.smartbuy.prix;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.prix.PrixDtos.PrixDto;
import tn.smartbuy.prix.PrixDtos.PrixRequete;
import tn.smartbuy.securite.UtilisateurConnecte;

/** POST /api/prix : proposition (utilisateur, PRX-07) ou saisie directe (admin, ADM-04). */
@RestController
@RequestMapping("/api/prix")
public class PrixController {

    private final PrixService prixService;

    public PrixController(PrixService prixService) {
        this.prixService = prixService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrixDto proposer(@AuthenticationPrincipal UtilisateurConnecte moi, @Valid @RequestBody PrixRequete requete) {
        return prixService.proposer(moi, requete);
    }
}
