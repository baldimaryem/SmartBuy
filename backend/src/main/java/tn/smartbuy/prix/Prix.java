package tn.smartbuy.prix;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tn.smartbuy.auth.Utilisateur;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Table PRIX : association entre PRODUIT et MAGASIN qui porte ses propres données
 * (montant, date du relevé, statut). On garde tous les relevés : c'est l'historique (PRX-05).
 */
@Entity
@Table(name = "PRIX")
public class Prix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PRODUIT_ID")
    private Produit produit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "MAGASIN_ID")
    private Magasin magasin;

    @Column(name = "MONTANT", nullable = false)
    private BigDecimal montant;

    @Column(name = "DATE_RELEVE", nullable = false)
    private LocalDate dateReleve;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE", nullable = false)
    private SourcePrix source;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUT", nullable = false)
    private StatutPrix statut;

    @ManyToOne
    @JoinColumn(name = "PROPOSE_PAR")
    private Utilisateur proposePar;

    public Prix() {
    }

    public Prix(Long id, Produit produit, Magasin magasin, BigDecimal montant, LocalDate dateReleve, StatutPrix statut) {
        this.id = id;
        this.produit = produit;
        this.magasin = magasin;
        this.montant = montant;
        this.dateReleve = dateReleve;
        this.statut = statut;
        this.source = SourcePrix.ADMIN;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public LocalDate getDateReleve() {
        return dateReleve;
    }

    public void setDateReleve(LocalDate dateReleve) {
        this.dateReleve = dateReleve;
    }

    public SourcePrix getSource() {
        return source;
    }

    public void setSource(SourcePrix source) {
        this.source = source;
    }

    public StatutPrix getStatut() {
        return statut;
    }

    public void setStatut(StatutPrix statut) {
        this.statut = statut;
    }

    public Utilisateur getProposePar() {
        return proposePar;
    }

    public void setProposePar(Utilisateur proposePar) {
        this.proposePar = proposePar;
    }
}
