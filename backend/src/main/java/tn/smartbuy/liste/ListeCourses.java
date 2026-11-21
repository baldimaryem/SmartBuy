package tn.smartbuy.liste;

import tn.smartbuy.prix.Univers;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Table LISTE_COURSES (PRX-09). Ses lignes sont enregistrées et supprimées avec elle (cascade). */
@Entity
@Table(name = "LISTE_COURSES")
public class ListeCourses {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "UTILISATEUR_ID", nullable = false)
    private Long utilisateurId;

    @Column(name = "NOM", nullable = false)
    private String nom;

    @Column(name = "DATE_CREATION", nullable = false)
    private LocalDate dateCreation = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUT", nullable = false)
    private StatutListe statut = StatutListe.EN_COURS;

    @Enumerated(EnumType.STRING)
    @Column(name = "UNIVERS", nullable = false)
    private Univers univers = Univers.ALIMENTATION;

    public Univers getUnivers() {
        return univers;
    }

    public void setUnivers(Univers univers) {
        this.univers = univers;
    }

    /** orphanRemoval : retirer une ligne de cette liste Java la supprime aussi dans Oracle. */
    @OneToMany(mappedBy = "liste", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<LigneListe> lignes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public StatutListe getStatut() {
        return statut;
    }

    public void setStatut(StatutListe statut) {
        this.statut = statut;
    }

    public List<LigneListe> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneListe> lignes) {
        this.lignes = lignes;
    }
}
