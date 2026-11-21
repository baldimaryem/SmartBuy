package tn.smartbuy.collecte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.commun.RessourceIntrouvableException;
import tn.smartbuy.prix.CategorieProduit;
import tn.smartbuy.prix.CategorieProduitRepository;
import tn.smartbuy.prix.Magasin;
import tn.smartbuy.prix.MagasinRepository;
import tn.smartbuy.prix.Prix;
import tn.smartbuy.prix.PrixDtos.ProduitDto;
import tn.smartbuy.prix.PrixDtos.ResultatCodeBarresDto;
import tn.smartbuy.prix.PrixRepository;
import tn.smartbuy.prix.Produit;
import tn.smartbuy.prix.ProduitRepository;
import tn.smartbuy.prix.SourcePrix;
import tn.smartbuy.prix.SourceProduit;
import tn.smartbuy.prix.StatutPrix;
import tn.smartbuy.prix.Univers;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Collecte des prix en ligne et recherche « intelligente » par code-barres.
 * <ul>
 *   <li>{@link #parCodeBarres} : Oracle → sinon Open Food Facts (le produit est ajouté au catalogue)
 *       → puis prix réel chez Carrefour Tunisie ;</li>
 *   <li>{@link #collecterCarrefour} : prix Carrefour de tous les produits qui ont un code-barres ;</li>
 *   <li>{@link #collecterSpacenet} : smartphones et PC portables de Spacenet (web scraping) ;</li>
 *   <li>{@link #actualiser} : relance la collecte pour un seul produit.</li>
 * </ul>
 * Chaque prix collecté devient une ligne PRIX (source COLLECTE, VALIDE, date du jour) dans le magasin
 * « en ligne » de l'enseigne : il entre donc directement dans la comparaison des listes de courses.
 */
@Service
public class CollecteService {

    private static final Logger log = LoggerFactory.getLogger(CollecteService.class);

    /** Résultat d'une collecte (réponse de POST /admin/collecte). */
    public record ResultatCollecte(String source, int produitsAnalyses, int produitsCrees, int prixEnregistres,
                                   List<String> erreurs, String duree) {
    }

    private final ProduitRepository produits;
    private final CategorieProduitRepository rayons;
    private final MagasinRepository magasins;
    private final PrixRepository prix;
    private final OpenFoodFacts openFoodFacts;
    private final CarrefourTn carrefour;
    private final SpacenetTn spacenet;
    private final boolean collecteActive;

    public CollecteService(ProduitRepository produits, CategorieProduitRepository rayons, MagasinRepository magasins,
                           PrixRepository prix, OpenFoodFacts openFoodFacts, CarrefourTn carrefour, SpacenetTn spacenet,
                           @org.springframework.beans.factory.annotation.Value("${smartbuy.collecte.active:true}")
                           boolean collecteActive) {
        this.collecteActive = collecteActive;
        this.produits = produits;
        this.rayons = rayons;
        this.magasins = magasins;
        this.prix = prix;
        this.openFoodFacts = openFoodFacts;
        this.carrefour = carrefour;
        this.spacenet = spacenet;
    }

    // =====================================================================
    // Recherche par code-barres
    // =====================================================================

    @Transactional
    public ResultatCodeBarresDto parCodeBarres(String saisie) {
        String code = saisie == null ? "" : saisie.replaceAll("\\s", "");
        if (!code.matches("\\d{8,14}")) {
            throw new RequeteInvalideException("Un code-barres contient 8 à 14 chiffres (ex. 6194043001019)");
        }
        // 1. Déjà dans Oracle ?
        Optional<Produit> local = produits.findFirstByCodeBarres(code);
        if (local.isPresent()) {
            return new ResultatCodeBarresDto(dto(local.get()), "CATALOGUE", "Produit trouvé dans le catalogue SmartBuy");
        }
        // 2. Sinon : Open Food Facts
        OpenFoodFacts.Fiche fiche;
        try {
            fiche = openFoodFacts.chercher(code).orElseThrow(() -> new RessourceIntrouvableException(
                    "Code-barres " + code + " inconnu : il n'est ni dans SmartBuy ni dans Open Food Facts. Vous pouvez ajouter ce produit."));
        } catch (IOException e) {
            log.warn("Open Food Facts injoignable pour {} : {}", code, e.getMessage());
            throw new RessourceIntrouvableException(
                    "Code-barres " + code + " absent du catalogue, et Open Food Facts est injoignable. Vous pouvez ajouter ce produit.");
        }
        Produit p = new Produit();
        p.setNom(tronquer(fiche.nom() + (fiche.quantite() == null ? "" : " " + fiche.quantite()), 150));
        p.setMarque(tronquer(fiche.marque(), 60));
        p.setCodeBarres(code);
        p.setUnite(unite(fiche.quantite()));
        p.setCategorieProduit(rayonAlimentaire(fiche.categories()));
        p.setImageUrl(tronquer(fiche.imageUrl(), 500));
        p.setSource(SourceProduit.OPEN_FOOD_FACTS);
        produits.save(p);

        // 3. Son prix réel chez Carrefour Tunisie (sans bloquer si le site ne répond pas)
        String message = "Produit trouvé sur Open Food Facts et ajouté au catalogue";
        try {
            int n = enregistrerPrixCarrefour(List.of(p));
            message += n > 0 ? " ; prix Carrefour Tunisie récupéré" : " ; pas vendu sur carrefour.tn";
        } catch (IOException e) {
            log.warn("Prix Carrefour indisponible pour {} : {}", code, e.getMessage());
        }
        return new ResultatCodeBarresDto(dto(p), "OPEN_FOOD_FACTS", message);
    }

    // =====================================================================
    // Collectes
    // =====================================================================

    @Transactional
    public ResultatCollecte collecterCarrefour() {
        long debut = System.currentTimeMillis();
        List<Produit> avecCode = produits.findByCodeBarresIsNotNull();
        List<String> erreurs = new ArrayList<>();
        int enregistres = 0;
        try {
            enregistres = enregistrerPrixCarrefour(avecCode);
        } catch (IOException e) {
            erreurs.add("carrefour.tn : " + e.getMessage());
        }
        return new ResultatCollecte("CARREFOUR", avecCode.size(), 0, enregistres, erreurs, duree(debut));
    }

    @Transactional
    public ResultatCollecte collecterSpacenet() {
        long debut = System.currentTimeMillis();
        Magasin boutique = magasinEnLigne(SpacenetTn.ENSEIGNE);
        List<String> erreurs = new ArrayList<>();
        int analyses = 0;
        int crees = 0;
        int enregistres = 0;
        for (SpacenetTn.PageCategorie page : SpacenetTn.PAGES) {
            try {
                CategorieProduit rayon = rayons.findFirstByNom(page.rayon())
                        .orElseThrow(() -> new IllegalStateException("Rayon absent : " + page.rayon()));
                for (SpacenetTn.Article a : spacenet.lirePage(page.url())) {
                    analyses++;
                    Produit p = produits.findFirstByUrlSource(a.url()).orElse(null);
                    if (p == null) {
                        p = new Produit();
                        p.setNom(tronquer(a.nom(), 150));
                        p.setMarque(tronquer(marqueDepuisNom(a.nom()), 60));
                        p.setUnite("pièce");
                        p.setCategorieProduit(rayon);
                        p.setSource(SourceProduit.COLLECTE);
                        p.setUrlSource(a.url());
                        produits.save(p);
                        crees++;
                    }
                    enregistrer(p, boutique, a.prix());
                    enregistres++;
                }
            } catch (IOException | RuntimeException e) {
                erreurs.add(page.url() + " : " + e.getMessage());
            }
        }
        return new ResultatCollecte("SPACENET", analyses, crees, enregistres, erreurs, duree(debut));
    }

    /** « Actualiser les prix en ligne » d'un produit. */
    @Transactional
    public void actualiser(Long produitId) {
        Produit p = produits.findById(produitId).orElseThrow(() -> new RessourceIntrouvableException("Produit introuvable"));
        try {
            if (p.getUrlSource() != null && p.getUrlSource().contains("spacenet.tn")) {
                Optional<BigDecimal> montant = spacenet.prixFiche(p.getUrlSource());
                if (montant.isPresent()) {
                    enregistrer(p, magasinEnLigne(SpacenetTn.ENSEIGNE), montant.get());
                }
            } else if (p.getCodeBarres() != null) {
                enregistrerPrixCarrefour(List.of(p));
            }
        } catch (IOException e) {
            throw new RequeteInvalideException("Site marchand injoignable : " + e.getMessage());
        }
    }

    /** Collecte automatique chaque nuit (désactivable : smartbuy.collecte.active=false). */
    @Scheduled(cron = "${smartbuy.collecte.cron:0 0 3 * * *}")
    @Transactional
    public void collecteNocturne() {
        if (!collecteActive) {
            return;
        }
        log.info("Collecte nocturne : {}", collecterCarrefour());
        log.info("Collecte nocturne : {}", collecterSpacenet());
    }

    // =====================================================================

    private int enregistrerPrixCarrefour(List<Produit> liste) throws IOException {
        List<Produit> avecCode = liste.stream().filter(p -> p.getCodeBarres() != null).toList();
        if (avecCode.isEmpty()) {
            return 0;
        }
        Map<String, CarrefourTn.Offre> offres = carrefour.prix(avecCode.stream().map(Produit::getCodeBarres).toList());
        Magasin boutique = magasinEnLigne(CarrefourTn.ENSEIGNE);
        int n = 0;
        for (Produit p : avecCode) {
            CarrefourTn.Offre offre = offres.get(p.getCodeBarres());
            if (offre != null) {
                enregistrer(p, boutique, offre.prix());
                n++;
            }
        }
        return n;
    }

    /** Un seul prix collecté par jour, produit et magasin : on met à jour au lieu de dupliquer. */
    private void enregistrer(Produit p, Magasin m, BigDecimal montant) {
        LocalDate aujourdhui = LocalDate.now();
        Prix releve = prix.findFirstByProduitIdAndMagasinIdAndSourceAndDateReleve(p.getId(), m.getId(),
                SourcePrix.COLLECTE, aujourdhui).orElseGet(Prix::new);
        releve.setProduit(p);
        releve.setMagasin(m);
        releve.setMontant(montant.setScale(3, RoundingMode.HALF_UP));
        releve.setDateReleve(aujourdhui);
        releve.setSource(SourcePrix.COLLECTE);
        releve.setStatut(StatutPrix.VALIDE);
        prix.save(releve);
    }

    private Magasin magasinEnLigne(String enseigne) {
        return magasins.findFirstByEnseigneAndEnLigneTrue(enseigne).orElseThrow(() -> new IllegalStateException(
                "Magasin en ligne « " + enseigne + " » absent : redémarrer l'API (migration v3)"));
    }

    /** Choisit le rayon alimentaire d'après les catégories Open Food Facts (en anglais). */
    CategorieProduit rayonAlimentaire(List<String> categories) {
        String tout = String.join(" ", categories);
        String rayon;
        // Les biscuits « au lait » ou « milk chocolate » ne sont pas des produits laitiers : on teste l'épicerie sucrée d'abord.
        if (tout.matches(".*(biscuit|cookie|wafer|gaufrette|snack|chocolate|confectioner|candies|cake|cereal|spread).*")) {
            rayon = "Épicerie";
        } else if (tout.matches(".*(dair|milks|yogurt|yoghurt|cheese|butter|cream|lait|fromage|yaourt).*")) {
            rayon = "Produits laitiers";
        } else if (tout.matches(".*(beverage|drink|water|juice|soda|nectar|boisson|eau|jus).*")) {
            rayon = "Boissons";
        } else if (tout.matches(".*(fresh fruit|fresh vegetable|fruits and vegetables|légume).*")) {
            rayon = "Fruits et légumes";
        } else if (tout.matches(".*(meat|poultry|egg|viande|volaille|oeuf|œuf).*")) {
            rayon = "Viandes, volailles et œufs";
        } else {
            rayon = "Épicerie";
        }
        return rayons.findFirstByNom(rayon)
                .or(() -> rayons.findByUniversOrderByNomAsc(Univers.ALIMENTATION).stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("Aucun rayon alimentaire"));
    }

    private ProduitDto dto(Produit p) {
        var actuels = prix.findByProduitIdAndStatutOrderByDateReleveAscIdAsc(p.getId(), StatutPrix.VALIDE);
        var parMagasin = tn.smartbuy.prix.PrixActuels.parProduitEtMagasin(actuels);
        return ProduitDto.depuis(p, tn.smartbuy.prix.PrixActuels.meilleur(parMagasin.get(p.getId())).orElse(null));
    }

    static String unite(String quantite) {
        if (quantite == null) {
            return "pièce";
        }
        String q = quantite.toLowerCase().replace(" ", "");
        if (q.matches(".*\\d(l|cl|ml)$")) {
            return "L";
        }
        if (q.matches(".*\\dkg$")) {
            return "kg";
        }
        return "pièce";
    }

    /** « Samsung Galaxy S25 Ultra 12Go » → « Samsung ». */
    static String marqueDepuisNom(String nom) {
        String premier = nom.trim().split("\\s+")[0];
        return premier.equalsIgnoreCase("iphone") || premier.equalsIgnoreCase("macbook") ? "Apple" : premier;
    }

    private static String tronquer(String s, int max) {
        return s == null ? null : (s.length() <= max ? s : s.substring(0, max));
    }

    private static String duree(long debut) {
        Duration d = Duration.ofMillis(System.currentTimeMillis() - debut);
        return d.toSeconds() + " s";
    }
}
