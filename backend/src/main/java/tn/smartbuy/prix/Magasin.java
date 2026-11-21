package tn.smartbuy.prix;

import jakarta.persistence.Convert;
import tn.smartbuy.commun.OuiNonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Table MAGASIN : un point de vente (PRX-01, ADM-01). */
@Entity
@Table(name = "MAGASIN")
public class Magasin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOM", nullable = false)
    private String nom;

    @Column(name = "ENSEIGNE", nullable = false)
    private String enseigne;

    @Column(name = "ADRESSE")
    private String adresse;

    @Column(name = "VILLE")
    private String ville;

    /** Site marchand (prix collectés automatiquement) plutôt que magasin physique. */
    @Convert(converter = OuiNonConverter.class)
    @Column(name = "EN_LIGNE", nullable = false)
    private boolean enLigne;

    @Column(name = "SITE_WEB")
    private String siteWeb;

    public boolean isEnLigne() {
        return enLigne;
    }

    public void setEnLigne(boolean enLigne) {
        this.enLigne = enLigne;
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public Magasin() {
    }

    public Magasin(Long id, String nom, String enseigne) {
        this.id = id;
        this.nom = nom;
        this.enseigne = enseigne;
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

    public String getEnseigne() {
        return enseigne;
    }

    public void setEnseigne(String enseigne) {
        this.enseigne = enseigne;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }
}
