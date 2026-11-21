package tn.smartbuy.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.prix.PrixDtos.PrixDto;
import tn.smartbuy.prix.PrixService;

import java.util.List;

/** 🔒 Modération des prix proposés par les utilisateurs (PRX-08). */
@RestController
@RequestMapping("/api/prix")
@PreAuthorize("hasRole('ADMIN')")
public class PrixAdminController {

    private final PrixService prixService;

    public PrixAdminController(PrixService prixService) {
        this.prixService = prixService;
    }

    @GetMapping("/en-attente")
    public List<PrixDto> enAttente() {
        return prixService.enAttente();
    }

    @PutMapping("/{id}/valider")
    public PrixDto valider(@PathVariable Long id) {
        return prixService.valider(id);
    }

    @PutMapping("/{id}/rejeter")
    public PrixDto rejeter(@PathVariable Long id) {
        return prixService.rejeter(id);
    }
}
