package tn.smartbuy.ia;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.commun.Mois;
import tn.smartbuy.ia.IaDtos.AssistantReponse;
import tn.smartbuy.ia.IaDtos.ConseilsReponse;
import tn.smartbuy.ia.IaDtos.QuestionRequete;
import tn.smartbuy.securite.UtilisateurConnecte;

@RestController
@RequestMapping("/api/ia")
public class IaController {

    private final IaService iaService;

    public IaController(IaService iaService) {
        this.iaService = iaService;
    }

    @GetMapping("/conseils")
    public ConseilsReponse conseils(@AuthenticationPrincipal UtilisateurConnecte moi,
                                    @RequestParam(required = false) String mois) {
        return iaService.conseils(moi.id(), Mois.lireOuCourant(mois));
    }

    @PostMapping("/assistant")
    public AssistantReponse assistant(@AuthenticationPrincipal UtilisateurConnecte moi,
                                      @Valid @RequestBody QuestionRequete requete) {
        return iaService.repondre(moi.id(), requete.question());
    }
}
