package tn.smartbuy.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Table CATEGORIE. {@code utilisateurId == null} : catégorie par défaut (BUD-01), commune à tous ;
 * sinon catégorie personnelle (BUD-02).
 * <p>
 * Pour le propriétaire on garde un simple identifiant (Long) plutôt qu'une relation JPA :
 * on n'a jamais besoin de charger l'utilisateur, seulement de comparer son id.
 */
@Entity
@Table(name = "CATEGORIE")
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOM", nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private TypeOperation type;

    @Column(name = "ICONE")
    private String icone;

    @Column(name = "UTILISATEUR_ID")
    private Long utilisateurId;

    public Categorie() {
    }

    public Categorie(Long id, String nom, TypeOperation type, String icone, Long utilisateurId) {
        this.id = id;
        this.nom = nom;
        this.type = type;
        this.icone = icone;
        this.utilisateurId = utilisateurId;
    }

    public boolean estPersonnalisee() {
        return utilisateurId != null;
    }

    /** Une catégorie est visible si elle est commune ou si elle appartient à l'utilisateur. */
    public boolean estVisiblePar(Long idUtilisateur) {
        return utilisateurId == null || utilisateurId.equals(idUtilisateur);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public TypeOperation getType() {
        return type;
    }

    public void setType(TypeOperation type) {
        this.type = type;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }
}
