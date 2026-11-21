package tn.smartbuy.prix;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Table PRODUIT : un article du catalogue (ADM-02).
 * Le prix n'est PAS ici : il dépend du magasin et de la date, il est donc dans la table PRIX.
 */
@Entity
@Table(name = "PRODUIT")
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOM", nullable = false)
    private String nom;

    @Column(name = "MARQUE")
    private String marque;

    @Column(name = "CODE_BARRES", unique = true)
    private String codeBarres;

    @Column(name = "UNITE", nullable = false)
    private String unite;

    @ManyToOne(optional = false)
    @JoinColumn(name = "CATEGORIE_PRODUIT_ID")
    private CategorieProduit categorieProduit;

    @Column(name = "IMAGE_URL")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE", nullable = false)
    private SourceProduit source = SourceProduit.CATALOGUE;

    /** Page du produit sur le site marchand (produits collectés), sert à actualiser son prix. */
    @Column(name = "URL_SOURCE")
    private String urlSource;

    public SourceProduit getSource() {
        return source;
    }

    public void setSource(SourceProduit source) {
        this.source = source;
    }

    public String getUrlSource() {
        return urlSource;
    }

    public void setUrlSource(String urlSource) {
        this.urlSource = urlSource;
    }

    public Univers getUnivers() {
        return categorieProduit == null ? Univers.ALIMENTATION : categorieProduit.getUnivers();
    }

    public Produit() {
    }

    public Produit(Long id, String nom, String unite) {
        this.id = id;
        this.nom = nom;
        this.unite = unite;
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

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getCodeBarres() {
        return codeBarres;
    }

    public void setCodeBarres(String codeBarres) {
        this.codeBarres = codeBarres;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public CategorieProduit getCategorieProduit() {
        return categorieProduit;
    }

    public void setCategorieProduit(CategorieProduit categorieProduit) {
        this.categorieProduit = categorieProduit;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
