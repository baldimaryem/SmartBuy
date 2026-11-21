package tn.smartbuy.collecte;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.prix.CatalogueService;
import tn.smartbuy.prix.PrixDtos.ContributionProduitRequete;
import tn.smartbuy.prix.PrixDtos.PrixMagasinDto;
import tn.smartbuy.prix.PrixDtos.ProduitDto;
import tn.smartbuy.prix.PrixDtos.ResultatCodeBarresDto;
import tn.smartbuy.prix.PrixService;

import java.util.List;

/** Code-barres « intelligent », contribution des utilisateurs et collecte des prix en ligne (contrat v3). */
@RestController
@RequestMapping("/api")
public class CollecteController {

    private final CollecteService collecte;
    private final CatalogueService catalogue;
    private final PrixService prixService;

    public CollecteController(CollecteService collecte, CatalogueService catalogue, PrixService prixService) {
        this.collecte = collecte;
        this.catalogue = catalogue;
        this.prixService = prixService;
    }

    @GetMapping("/produits/code-barres/{code}")
    public ResultatCodeBarresDto parCodeBarres(@PathVariable String code) {
        return collecte.parCodeBarres(code);
    }

    @PostMapping("/produits/contribution")
    @ResponseStatus(HttpStatus.CREATED)
    public ProduitDto contribuer(@Valid @RequestBody ContributionProduitRequete requete) {
        return catalogue.contribuer(requete);
    }

    @PostMapping("/produits/{id}/actualiser-prix")
    public List<PrixMagasinDto> actualiser(@PathVariable Long id) {
        collecte.actualiser(id);
        return prixService.prixParMagasin(id);
    }

    @PostMapping("/admin/collecte")
    @PreAuthorize("hasRole('ADMIN')")
    public CollecteService.ResultatCollecte collecter(@RequestParam String source) {
        return switch (source.toUpperCase()) {
            case "CARREFOUR" -> collecte.collecterCarrefour();
            case "SPACENET" -> collecte.collecterSpacenet();
            default -> throw new RequeteInvalideException("Source inconnue : CARREFOUR ou SPACENET");
        };
    }
}
