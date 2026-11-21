package tn.smartbuy.prix;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.prix.PrixDtos.CategorieProduitDto;
import tn.smartbuy.prix.PrixDtos.HistoriquePrixDto;
import tn.smartbuy.prix.PrixDtos.MagasinDto;
import tn.smartbuy.prix.PrixDtos.PrixMagasinDto;
import tn.smartbuy.prix.PrixDtos.ProduitDto;

import java.util.List;

/**
 * Consultation du catalogue (tout utilisateur connecté).
 * Les modifications (POST / PUT / DELETE) sont dans admin.CatalogueAdminController.
 */
@RestController
@RequestMapping("/api")
public class CatalogueController {

    private final CatalogueService catalogue;
    private final PrixService prixService;

    public CatalogueController(CatalogueService catalogue, PrixService prixService) {
        this.catalogue = catalogue;
        this.prixService = prixService;
    }

    @GetMapping("/magasins")
    public List<MagasinDto> magasins() {
        return catalogue.listerMagasins();
    }

    @GetMapping("/categories-produits")
    public List<CategorieProduitDto> categoriesProduits(@RequestParam(required = false) Univers univers) {
        return catalogue.listerCategories(univers);
    }

    @GetMapping("/produits")
    public List<ProduitDto> produits(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) Long categorieId,
                                     @RequestParam(required = false) String codeBarres,
                                     @RequestParam(required = false) Univers univers) {
        return catalogue.rechercherProduits(q, categorieId, codeBarres, univers);
    }

    @GetMapping("/produits/{id}")
    public ProduitDto produit(@PathVariable Long id) {
        return catalogue.detailProduit(id);
    }

    @GetMapping("/produits/{id}/prix")
    public List<PrixMagasinDto> prix(@PathVariable Long id) {
        return prixService.prixParMagasin(id);
    }

    @GetMapping("/produits/{id}/historique")
    public List<HistoriquePrixDto> historique(@PathVariable Long id) {
        return prixService.historique(id);
    }
}
