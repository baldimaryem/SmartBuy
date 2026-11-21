package tn.smartbuy.budget;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.budget.BudgetDtos.CategorieDto;
import tn.smartbuy.budget.BudgetDtos.CategorieRequete;
import tn.smartbuy.commun.AccesInterditException;
import tn.smartbuy.commun.ConflitException;
import tn.smartbuy.commun.RessourceIntrouvableException;

import java.util.Comparator;
import java.util.List;

/** Catégories de dépenses et de revenus (BUD-01, BUD-02). */
@Service
public class CategorieService {

    /** Ordre d'affichage : catégories par défaut (dans l'ordre de création), puis personnelles par nom. */
    private static final Comparator<Categorie> ORDRE = Comparator
            .comparing(Categorie::estPersonnalisee)
            .thenComparing(c -> c.estPersonnalisee() ? c.getNom().toLowerCase() : "")
            .thenComparing(Categorie::getId);

    private final CategorieRepository categories;
    private final OperationRepository operations;

    public CategorieService(CategorieRepository categories, OperationRepository operations) {
        this.categories = categories;
        this.operations = operations;
    }

    /** BUD-01 + BUD-02 : catégories par défaut + celles de l'utilisateur, filtrées par type si demandé. */
    @Transactional(readOnly = true)
    public List<CategorieDto> lister(Long utilisateurId, TypeOperation type) {
        return categories.findByUtilisateurIdIsNullOrUtilisateurId(utilisateurId).stream()
                .filter(c -> type == null || c.getType() == type)
                .sorted(ORDRE)
                .map(CategorieDto::depuis)
                .toList();
    }

    /** BUD-02 : créer une catégorie personnelle. */
    @Transactional
    public CategorieDto creer(Long utilisateurId, CategorieRequete requete) {
        Categorie c = new Categorie();
        c.setUtilisateurId(utilisateurId);
        remplir(c, requete);
        return CategorieDto.depuis(categories.save(c));
    }

    /** BUD-02 : renommer (seulement ses propres catégories). */
    @Transactional
    public CategorieDto modifier(Long utilisateurId, Long id, CategorieRequete requete) {
        Categorie c = chargerModifiable(utilisateurId, id);
        remplir(c, requete);
        return CategorieDto.depuis(c);
    }

    /** BUD-02 : supprimer, si aucune opération ne l'utilise (les budgets associés partent en cascade). */
    @Transactional
    public void supprimer(Long utilisateurId, Long id) {
        Categorie c = chargerModifiable(utilisateurId, id);
        if (operations.existsByCategorieId(id)) {
            throw new ConflitException("Impossible de supprimer : des opérations utilisent cette catégorie");
        }
        categories.delete(c);
    }

    /** Catégorie utilisable par l'utilisateur (par défaut ou personnelle), sinon 404 / 403. */
    public Categorie chargerVisible(Long utilisateurId, Long id) {
        Categorie c = categories.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Catégorie introuvable"));
        if (!c.estVisiblePar(utilisateurId)) {
            throw new AccesInterditException("Cette catégorie ne vous appartient pas");
        }
        return c;
    }

    private Categorie chargerModifiable(Long utilisateurId, Long id) {
        Categorie c = chargerVisible(utilisateurId, id);
        if (!c.estPersonnalisee()) {
            throw new AccesInterditException("Les catégories par défaut ne sont pas modifiables");
        }
        return c;
    }

    private void remplir(Categorie c, CategorieRequete r) {
        c.setNom(r.nom().trim());
        c.setType(r.type());
        c.setIcone(r.icone() == null || r.icone().isBlank() ? "category" : r.icone().trim());
    }
}
