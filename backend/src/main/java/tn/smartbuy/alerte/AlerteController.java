package tn.smartbuy.alerte;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.alerte.AlerteService.AlerteDto;
import tn.smartbuy.securite.UtilisateurConnecte;

import java.util.List;

@RestController
@RequestMapping("/api/alertes")
public class AlerteController {

    private final AlerteService alerteService;

    public AlerteController(AlerteService alerteService) {
        this.alerteService = alerteService;
    }

    @GetMapping
    public List<AlerteDto> lister(@AuthenticationPrincipal UtilisateurConnecte moi) {
        return alerteService.lister(moi.id());
    }

    @PutMapping("/{id}/lue")
    public AlerteDto marquerLue(@AuthenticationPrincipal UtilisateurConnecte moi, @PathVariable Long id) {
        return alerteService.marquerLue(moi.id(), id);
    }
}
