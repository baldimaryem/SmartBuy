package tn.smartbuy.admin;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.prix.CatalogueService;
import tn.smartbuy.prix.PrixDtos.CategorieProduitDto;
import tn.smartbuy.prix.PrixDtos.CategorieProduitRequete;
import tn.smartbuy.prix.PrixDtos.MagasinDto;
import tn.smartbuy.prix.PrixDtos.MagasinRequete;
import tn.smartbuy.prix.PrixDtos.ProduitDto;
import tn.smartbuy.prix.PrixDtos.ProduitRequete;

/**
 * 🔒 Administration du catalogue (ADM-01, ADM-02, ADM-03).
 * <p>
 * {@code @PreAuthorize} sur la classe : TOUTES les méthodes exigent le rôle ADMIN,
 * sinon Spring lève AccessDeniedException, transformée en 403 par GestionnaireErreurs.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
public class CatalogueAdminController {

    private final CatalogueService catalogue;

    public CatalogueAdminController(CatalogueService catalogue) {
        this.catalogue = catalogue;
    }

    // ----- Magasins (ADM-01) -----

    @PostMapping("/magasins")
    @ResponseStatus(HttpStatus.CREATED)
    public MagasinDto creerMagasin(@Valid @RequestBody MagasinRequete requete) {
        return catalogue.creerMagasin(requete);
    }

    @PutMapping("/magasins/{id}")
    public MagasinDto modifierMagasin(@PathVariable Long id, @Valid @RequestBody MagasinRequete requete) {
        return catalogue.modifierMagasin(id, requete);
    }

    @DeleteMapping("/magasins/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerMagasin(@PathVariable Long id) {
        catalogue.supprimerMagasin(id);
    }

    // ----- Catégories de produits (ADM-03) -----

    @PostMapping("/categories-produits")
    @ResponseStatus(HttpStatus.CREATED)
    public CategorieProduitDto creerCategorie(@Valid @RequestBody CategorieProduitRequete requete) {
        return catalogue.creerCategorie(requete);
    }

    @PutMapping("/categories-produits/{id}")
    public CategorieProduitDto modifierCategorie(@PathVariable Long id,
                                                 @Valid @RequestBody CategorieProduitRequete requete) {
        return catalogue.modifierCategorie(id, requete);
    }

    @DeleteMapping("/categories-produits/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerCategorie(@PathVariable Long id) {
        catalogue.supprimerCategorie(id);
    }

    // ----- Produits (ADM-02) -----

    @PostMapping("/produits")
    @ResponseStatus(HttpStatus.CREATED)
    public ProduitDto creerProduit(@Valid @RequestBody ProduitRequete requete) {
        return catalogue.creerProduit(requete);
    }

    @PutMapping("/produits/{id}")
    public ProduitDto modifierProduit(@PathVariable Long id, @Valid @RequestBody ProduitRequete requete) {
        return catalogue.modifierProduit(id, requete);
    }

    @DeleteMapping("/produits/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerProduit(@PathVariable Long id) {
        catalogue.supprimerProduit(id);
    }
}
