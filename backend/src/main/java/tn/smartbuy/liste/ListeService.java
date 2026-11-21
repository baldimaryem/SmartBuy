package tn.smartbuy.liste;

import tn.smartbuy.prix.Univers;
import tn.smartbuy.prix.Produit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.budget.BudgetDtos.OperationDto;
import tn.smartbuy.budget.BudgetService;
import tn.smartbuy.budget.Categorie;
import tn.smartbuy.budget.CategorieRepository;
import tn.smartbuy.budget.OperationService;
import tn.smartbuy.budget.TypeOperation;
import tn.smartbuy.commun.AccesInterditException;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.commun.RessourceIntrouvableException;
import tn.smartbuy.liste.ComparateurPrix.Article;
import tn.smartbuy.liste.ComparateurPrix.Resultat;
import tn.smartbuy.liste.ComparateurPrix.ResultatMagasin;
import tn.smartbuy.liste.ListeDtos.ComparaisonDto;
import tn.smartbuy.liste.ListeDtos.LigneRequete;
import tn.smartbuy.liste.ListeDtos.ListeDetailDto;
import tn.smartbuy.liste.ListeDtos.ListeResumeDto;
import tn.smartbuy.prix.CatalogueService;
import tn.smartbuy.prix.MagasinRepository;
import tn.smartbuy.prix.PrixService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Listes de courses (PRX-09..13) et lien avec le budget (LNK-01..03). */
@Service
public class ListeService {

    /** Catégorie par défaut utilisée pour les courses (LNK-01, LNK-02). */

    private final ListeCoursesRepository listes;
    private final CatalogueService catalogue;
    private final MagasinRepository magasins;
    private final PrixService prixService;
    private final BudgetService budgetService;
    private final CategorieRepository categories;
    private final OperationService operationService;

    public ListeService(ListeCoursesRepository listes, CatalogueService catalogue, MagasinRepository magasins,
                        PrixService prixService, BudgetService budgetService, CategorieRepository categories,
                        OperationService operationService) {
        this.listes = listes;
        this.catalogue = catalogue;
        this.magasins = magasins;
        this.prixService = prixService;
        this.budgetService = budgetService;
        this.categories = categories;
        this.operationService = operationService;
    }

    // ---------------------------------------------------------------------
    // PRX-09 : créer, renommer, supprimer
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ListeResumeDto> lister(Long utilisateurId) {
        return listes.findByUtilisateurIdOrderByDateCreationDescIdDesc(utilisateurId).stream()
                .map(ListeResumeDto::depuis).toList();
    }

    /** PRX-09 */
    @Transactional
    public ListeResumeDto creer(Long utilisateurId, String nom, Univers univers) {
        ListeCourses l = new ListeCourses();
        l.setUtilisateurId(utilisateurId);
        l.setNom(nom.trim());
        l.setUnivers(univers == null ? Univers.ALIMENTATION : univers);
        return ListeResumeDto.depuis(listes.save(l));
    }

    /** PRX-09 */
    @Transactional
    public ListeResumeDto renommer(Long utilisateurId, Long id, String nom) {
        ListeCourses l = chargerMienne(utilisateurId, id);
        l.setNom(nom.trim());
        return ListeResumeDto.depuis(l);
    }

    /** PRX-09 (les lignes sont supprimées en cascade) */
    @Transactional
    public void supprimer(Long utilisateurId, Long id) {
        listes.delete(chargerMienne(utilisateurId, id));
    }

    @Transactional(readOnly = true)
    public ListeDetailDto detail(Long utilisateurId, Long id) {
        return ListeDetailDto.depuis(chargerMienne(utilisateurId, id));
    }

    // ---------------------------------------------------------------------
    // PRX-10 : ajouter / modifier / retirer des produits
    // ---------------------------------------------------------------------

    /** PRX-10 : si le produit est déjà dans la liste, les quantités s'additionnent. */
    @Transactional
    public ListeDetailDto ajouterLigne(Long utilisateurId, Long listeId, LigneRequete requete) {
        ListeCourses liste = chargerModifiable(utilisateurId, listeId);
        var existante = liste.getLignes().stream()
                .filter(l -> l.getProduit().getId().equals(requete.produitId()))
                .findFirst();
        if (existante.isPresent()) {
            existante.get().setQuantite(existante.get().getQuantite() + requete.quantite());
        } else {
            Produit produit = catalogue.chargerProduit(requete.produitId());
            // Une liste « Vêtements » ne contient que des vêtements : la comparaison et le budget restent cohérents.
            if (produit.getUnivers() != liste.getUnivers()) {
                throw new RequeteInvalideException("« %s » n'appartient pas à l'univers de cette liste (%s)"
                        .formatted(produit.getNom(), liste.getUnivers().categorieBudget()));
            }
            LigneListe ligne = new LigneListe();
            ligne.setListe(liste);
            ligne.setProduit(produit);
            ligne.setQuantite(requete.quantite());
            liste.getLignes().add(ligne);
        }
        listes.saveAndFlush(liste); // flush : pour que la nouvelle ligne ait son id dans la réponse
        return ListeDetailDto.depuis(liste);
    }

    /** PRX-10 */
    @Transactional
    public ListeDetailDto modifierLigne(Long utilisateurId, Long listeId, Long ligneId, int quantite) {
        ListeCourses liste = chargerModifiable(utilisateurId, listeId);
        chargerLigne(liste, ligneId).setQuantite(quantite);
        return ListeDetailDto.depuis(liste);
    }

    /** PRX-10 : retirer un produit (orphanRemoval supprime la ligne dans Oracle). */
    @Transactional
    public ListeDetailDto supprimerLigne(Long utilisateurId, Long listeId, Long ligneId) {
        ListeCourses liste = chargerModifiable(utilisateurId, listeId);
        liste.getLignes().remove(chargerLigne(liste, ligneId));
        return ListeDetailDto.depuis(liste);
    }

    // ---------------------------------------------------------------------
    // PRX-11..13 + LNK-01 : comparaison
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ComparaisonDto comparer(Long utilisateurId, Long listeId) {
        ListeCourses liste = chargerMienne(utilisateurId, listeId);
        Resultat r = calculer(utilisateurId, liste);
        return new ComparaisonDto(liste.getId(), r.magasins(), r.magasinRecommandeId(), r.economie(),
                r.budgetRestant(), r.depasseBudget());
    }

    // ---------------------------------------------------------------------
    // LNK-02 / LNK-03 : transformer la liste en dépense
    // ---------------------------------------------------------------------

    /**
     * LNK-02 : crée une DEPENSE (catégorie de l'univers : Alimentation, Vêtements, High-tech) du montant de la liste dans le magasin choisi,
     * note « Liste : nom ». LNK-03 : economie = total du complet le plus cher − total payé.
     * La liste passe ensuite à ACHETEE.
     */
    @Transactional
    public OperationDto acheter(Long utilisateurId, Long listeId, Long magasinId) {
        ListeCourses liste = chargerModifiable(utilisateurId, listeId);
        if (liste.getLignes().isEmpty()) {
            throw new RequeteInvalideException("La liste est vide");
        }
        var magasin = catalogue.chargerMagasin(magasinId);
        Resultat r = calculer(utilisateurId, liste);
        ResultatMagasin choisi = r.magasins().stream()
                .filter(m -> m.magasinId().equals(magasinId))
                .findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException("Magasin introuvable"));
        if (choisi.total() == 0) {
            throw new RequeteInvalideException("Aucun produit de la liste n'a de prix dans ce magasin");
        }

        OperationDto operation = operationService.creerDepenseAchat(utilisateurId, categorieBudget(liste.getUnivers()),
                BigDecimal.valueOf(choisi.total()), magasin, "Liste : " + liste.getNom(),
                ComparateurPrix.economieAchat(r, choisi));
        liste.setStatut(StatutListe.ACHETEE);
        return operation;
    }

    // ---------------------------------------------------------------------

    private Resultat calculer(Long utilisateurId, ListeCourses liste) {
        List<Article> articles = liste.getLignes().stream()
                .map(l -> new Article(l.getProduit().getId(), l.getProduit().getNom(), l.getQuantite()))
                .toList();
        var prix = prixService.prixActuels(articles.stream().map(Article::produitId).toList());
        BigDecimal reste = budgetService.resteCategorie(utilisateurId, YearMonth.now(),
                        categorieBudget(liste.getUnivers()).getId())
                .orElse(null);
        return ComparateurPrix.comparer(articles, magasins.findAllByOrderByNomAsc(), prix, reste);
    }

    /** Catégorie de budget de l'univers : Alimentation, Vêtements ou High-tech (LNK-01 / LNK-02). */
    private Categorie categorieBudget(Univers univers) {
        return categories.findFirstByNomAndTypeAndUtilisateurIdIsNull(univers.categorieBudget(), TypeOperation.DEPENSE)
                .orElseThrow(() -> new IllegalStateException(
                        "Catégorie par défaut « " + univers.categorieBudget() + " » absente : redémarrer l'API (migration v3)"));
    }

    private ListeCourses chargerMienne(Long utilisateurId, Long id) {
        ListeCourses l = listes.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Liste de courses introuvable"));
        if (!l.getUtilisateurId().equals(utilisateurId)) {
            throw new AccesInterditException("Cette liste ne vous appartient pas");
        }
        return l;
    }

    /** Une liste déjà achetée ne change plus (sauf son nom) : elle correspond à une dépense enregistrée. */
    private ListeCourses chargerModifiable(Long utilisateurId, Long id) {
        ListeCourses l = chargerMienne(utilisateurId, id);
        if (l.getStatut() == StatutListe.ACHETEE) {
            throw new RequeteInvalideException("Cette liste a déjà été achetée : elle ne peut plus être modifiée");
        }
        return l;
    }

    private LigneListe chargerLigne(ListeCourses liste, Long ligneId) {
        return liste.getLignes().stream()
                .filter(l -> l.getId().equals(ligneId))
                .findFirst()
                .orElseThrow(() -> new RessourceIntrouvableException("Ligne introuvable dans cette liste"));
    }
}
