package tn.smartbuy.prix;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.commun.ConflitException;
import tn.smartbuy.commun.RessourceIntrouvableException;
import tn.smartbuy.prix.PrixDtos.CategorieProduitDto;
import tn.smartbuy.prix.PrixDtos.CategorieProduitRequete;
import tn.smartbuy.prix.PrixDtos.MagasinDto;
import tn.smartbuy.prix.PrixDtos.MagasinRequete;
import tn.smartbuy.prix.PrixDtos.ProduitDto;
import tn.smartbuy.prix.PrixDtos.ProduitRequete;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Catalogue : magasins, catégories de produits et produits (consultation + administration). */
@Service
public class CatalogueService {

    private final MagasinRepository magasins;
    private final CategorieProduitRepository categories;
    private final ProduitRepository produits;
    private final PrixRepository prix;

    public CatalogueService(MagasinRepository magasins, CategorieProduitRepository categories,
                            ProduitRepository produits, PrixRepository prix) {
        this.magasins = magasins;
        this.categories = categories;
        this.produits = produits;
        this.prix = prix;
    }

    // =====================================================================
    // Magasins
    // =====================================================================

    /** PRX-01 */
    @Transactional(readOnly = true)
    public List<MagasinDto> listerMagasins() {
        return magasins.findAllByOrderByNomAsc().stream().map(MagasinDto::depuis).toList();
    }

    /** ADM-01 */
    @Transactional
    public MagasinDto creerMagasin(MagasinRequete requete) {
        Magasin m = new Magasin();
        remplir(m, requete);
        return MagasinDto.depuis(magasins.save(m));
    }

    /** ADM-01 */
    @Transactional
    public MagasinDto modifierMagasin(Long id, MagasinRequete requete) {
        Magasin m = chargerMagasin(id);
        remplir(m, requete);
        return MagasinDto.depuis(m);
    }

    /** ADM-01 : les prix du magasin sont supprimés en cascade (ON DELETE CASCADE dans Oracle). */
    @Transactional
    public void supprimerMagasin(Long id) {
        magasins.delete(chargerMagasin(id));
    }

    public Magasin chargerMagasin(Long id) {
        return magasins.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Magasin introuvable"));
    }

    private void remplir(Magasin m, MagasinRequete r) {
        m.setNom(r.nom().trim());
        m.setEnseigne(r.enseigne().trim());
        m.setAdresse(r.adresse());
        m.setVille(r.ville());
    }

    // =====================================================================
    // Catégories de produits
    // =====================================================================

    @Transactional(readOnly = true)
    public List<CategorieProduitDto> listerCategories(Univers univers) {
        var liste = univers == null ? categories.findAllByOrderByNomAsc() : categories.findByUniversOrderByNomAsc(univers);
        return liste.stream().map(CategorieProduitDto::depuis).toList();
    }

    /** ADM-03 */
    @Transactional
    public CategorieProduitDto creerCategorie(CategorieProduitRequete requete) {
        if (categories.existsByNomIgnoreCase(requete.nom().trim())) {
            throw new ConflitException("Cette catégorie de produits existe déjà");
        }
        CategorieProduit c = new CategorieProduit();
        c.setNom(requete.nom().trim());
        c.setUnivers(requete.univers() == null ? Univers.ALIMENTATION : requete.univers());
        return CategorieProduitDto.depuis(categories.save(c));
    }

    /** ADM-03 */
    @Transactional
    public CategorieProduitDto modifierCategorie(Long id, CategorieProduitRequete requete) {
        CategorieProduit c = chargerCategorie(id);
        c.setNom(requete.nom().trim());
        if (requete.univers() != null) {
            c.setUnivers(requete.univers());
        }
        return CategorieProduitDto.depuis(c);
    }

    /** ADM-03 : refusé si des produits utilisent encore la catégorie. */
    @Transactional
    public void supprimerCategorie(Long id) {
        CategorieProduit c = chargerCategorie(id);
        if (produits.existsByCategorieProduitId(id)) {
            throw new ConflitException("Impossible de supprimer : des produits utilisent cette catégorie");
        }
        categories.delete(c);
    }

    private CategorieProduit chargerCategorie(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Catégorie de produits introuvable"));
    }

    // =====================================================================
    // Produits
    // =====================================================================

    /** PRX-02 (nom, marque, catégorie) et PRX-03 (code-barres). */
    @Transactional(readOnly = true)
    public List<ProduitDto> rechercherProduits(String q, Long categorieId, String codeBarres, Univers univers) {
        String texte = estVide(q) ? null : q.trim().toLowerCase(Locale.ROOT);
        String code = estVide(codeBarres) ? null : codeBarres.trim();
        List<Produit> trouves = produits.rechercher(texte, categorieId, code, univers);
        if (trouves.isEmpty()) {
            return List.of();
        }
        // Une seule requête pour tous les prix des produits trouvés, puis calcul en mémoire
        List<Long> ids = trouves.stream().map(Produit::getId).toList();
        Map<Long, Map<Long, Prix>> actuels =
                PrixActuels.parProduitEtMagasin(prix.findByProduitIdInAndStatut(ids, StatutPrix.VALIDE));
        return trouves.stream()
                .map(p -> ProduitDto.depuis(p, PrixActuels.meilleur(actuels.get(p.getId())).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProduitDto detailProduit(Long id) {
        Produit p = chargerProduit(id);
        return ProduitDto.depuis(p, meilleurPrix(p.getId()));
    }

    /** ADM-02 */
    @Transactional
    public ProduitDto creerProduit(ProduitRequete requete) {
        verifierCodeBarresLibre(requete.codeBarres(), null);
        Produit p = new Produit();
        remplir(p, requete);
        return ProduitDto.depuis(produits.save(p), null);
    }

    /** ADM-02 */
    @Transactional
    public ProduitDto modifierProduit(Long id, ProduitRequete requete) {
        Produit p = chargerProduit(id);
        verifierCodeBarresLibre(requete.codeBarres(), p);
        remplir(p, requete);
        return ProduitDto.depuis(p, meilleurPrix(id));
    }

    /** Un utilisateur ajoute un produit introuvable (code-barres inconnu partout). */
    @Transactional
    public ProduitDto contribuer(PrixDtos.ContributionProduitRequete r) {
        verifierCodeBarresLibre(r.codeBarres(), null);
        Produit p = new Produit();
        remplir(p, new ProduitRequete(r.nom(), r.marque(), r.codeBarres(), r.unite(), r.categorieProduitId(), null));
        p.setSource(SourceProduit.UTILISATEUR);
        return ProduitDto.depuis(produits.save(p), null);
    }

    /** ADM-02 : prix et lignes de listes supprimés en cascade. */
    @Transactional
    public void supprimerProduit(Long id) {
        produits.delete(chargerProduit(id));
    }

    public Produit chargerProduit(Long id) {
        return produits.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable"));
    }

    private Prix meilleurPrix(Long produitId) {
        var actuels = PrixActuels.parProduitEtMagasin(
                prix.findByProduitIdAndStatutOrderByDateReleveAscIdAsc(produitId, StatutPrix.VALIDE));
        return PrixActuels.meilleur(actuels.get(produitId)).orElse(null);
    }

    private void verifierCodeBarresLibre(String codeBarres, Produit actuel) {
        if (estVide(codeBarres)) {
            return;
        }
        boolean inchange = actuel != null && codeBarres.trim().equals(actuel.getCodeBarres());
        if (!inchange && produits.existsByCodeBarres(codeBarres.trim())) {
            throw new ConflitException("Un produit existe déjà avec ce code-barres");
        }
    }

    private void remplir(Produit p, ProduitRequete r) {
        p.setNom(r.nom().trim());
        p.setMarque(estVide(r.marque()) ? null : r.marque().trim());
        p.setCodeBarres(estVide(r.codeBarres()) ? null : r.codeBarres().trim());
        p.setUnite(r.unite().trim());
        p.setCategorieProduit(chargerCategorie(r.categorieProduitId()));
        p.setImageUrl(estVide(r.imageUrl()) ? null : r.imageUrl().trim());
    }

    private static boolean estVide(String s) {
        return s == null || s.isBlank();
    }
}
