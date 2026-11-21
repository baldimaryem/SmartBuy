package tn.smartbuy.liste;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tn.smartbuy.prix.Produit;

/** Table LIGNE_LISTE : un produit et sa quantité dans une liste (PRX-10). */
@Entity
@Table(name = "LIGNE_LISTE")
public class LigneListe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "LISTE_ID")
    private ListeCourses liste;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PRODUIT_ID")
    private Produit produit;

    @Column(name = "QUANTITE", nullable = false)
    private int quantite;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ListeCourses getListe() {
        return liste;
    }

    public void setListe(ListeCourses liste) {
        this.liste = liste;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }
}
