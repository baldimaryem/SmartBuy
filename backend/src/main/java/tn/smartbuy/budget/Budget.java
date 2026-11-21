package tn.smartbuy.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tn.smartbuy.commun.MoisConverter;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Table BUDGET : plafond mensuel global (categorie == null, BUD-08) ou par catégorie (BUD-09). */
@Entity
@Table(name = "BUDGET")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "UTILISATEUR_ID", nullable = false)
    private Long utilisateurId;

    @ManyToOne
    @JoinColumn(name = "CATEGORIE_ID")
    private Categorie categorie;

    @Convert(converter = MoisConverter.class)
    @Column(name = "MOIS", nullable = false)
    private YearMonth mois;

    @Column(name = "MONTANT_PLAFOND", nullable = false)
    private BigDecimal montantPlafond;

    public boolean estGlobal() {
        return categorie == null;
    }

    /** Libellé utilisé dans les alertes et e-mails. */
    public String libelle() {
        return estGlobal() ? "Budget global" : categorie.getNom();
    }

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

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public YearMonth getMois() {
        return mois;
    }

    public void setMois(YearMonth mois) {
        this.mois = mois;
    }

    public BigDecimal getMontantPlafond() {
        return montantPlafond;
    }

    public void setMontantPlafond(BigDecimal montantPlafond) {
        this.montantPlafond = montantPlafond;
    }
}
