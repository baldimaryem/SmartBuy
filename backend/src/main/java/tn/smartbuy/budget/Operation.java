package tn.smartbuy.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tn.smartbuy.commun.OuiNonConverter;
import tn.smartbuy.prix.Magasin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Table OPERATION : un revenu ou une dépense (BUD-03, BUD-04).
 * Le magasin facultatif fait le lien avec le module « prix ».
 */
@Entity
@Table(name = "OPERATION")
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "UTILISATEUR_ID", nullable = false)
    private Long utilisateurId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "CATEGORIE_ID")
    private Categorie categorie;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private TypeOperation type;

    @Column(name = "MONTANT", nullable = false)
    private BigDecimal montant;

    @Column(name = "DATE_OPERATION", nullable = false)
    private LocalDate dateOperation;

    @Column(name = "NOTE")
    private String note;

    @ManyToOne
    @JoinColumn(name = "MAGASIN_ID")
    private Magasin magasin;

    @Convert(converter = OuiNonConverter.class)
    @Column(name = "RECURRENTE", nullable = false)
    private boolean recurrente;

    /** Économie réalisée grâce à la comparaison des prix (LNK-03), 0 sinon. */
    @Column(name = "ECONOMIE", nullable = false)
    private BigDecimal economie = BigDecimal.ZERO;

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

    public TypeOperation getType() {
        return type;
    }

    public void setType(TypeOperation type) {
        this.type = type;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public boolean isRecurrente() {
        return recurrente;
    }

    public void setRecurrente(boolean recurrente) {
        this.recurrente = recurrente;
    }

    public BigDecimal getEconomie() {
        return economie;
    }

    public void setEconomie(BigDecimal economie) {
        this.economie = economie;
    }
}
