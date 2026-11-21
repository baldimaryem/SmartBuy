package tn.smartbuy.rapport;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.commun.Mois;
import tn.smartbuy.rapport.RapportService.RapportReponse;
import tn.smartbuy.securite.UtilisateurConnecte;

/** POST /api/rapports/mensuel?mois=2026-11 : envoie le bilan du mois par e-mail à l'utilisateur connecté. */
@RestController
@RequestMapping("/api/rapports")
public class RapportController {

    private final RapportService rapportService;

    public RapportController(RapportService rapportService) {
        this.rapportService = rapportService;
    }

    @PostMapping("/mensuel")
    public RapportReponse envoyer(@AuthenticationPrincipal UtilisateurConnecte moi,
                                  @RequestParam(required = false) String mois) {
        return rapportService.envoyerBilan(moi.id(), Mois.lireOuCourant(mois));
    }
}
