package tn.smartbuy.prix;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Table CATEGORIE_PRODUIT : un rayon du catalogue (ADM-03). */
@Entity
@Table(name = "CATEGORIE_PRODUIT")
public class CategorieProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOM", nullable = false, unique = true)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "UNIVERS", nullable = false)
    private Univers univers = Univers.ALIMENTATION;

    public Univers getUnivers() {
        return univers;
    }

    public void setUnivers(Univers univers) {
        this.univers = univers;
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
}
