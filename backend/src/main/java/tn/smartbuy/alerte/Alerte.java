package tn.smartbuy.alerte;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tn.smartbuy.commun.OuiNonConverter;

import java.time.LocalDateTime;

/** Table ALERTE : notification affichée dans l'application (et envoyée par e-mail). */
@Entity
@Table(name = "ALERTE")
public class Alerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "UTILISATEUR_ID", nullable = false)
    private Long utilisateurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", nullable = false)
    private TypeAlerte type;

    @Column(name = "MESSAGE", nullable = false)
    private String message;

    @Column(name = "DATE_ALERTE", nullable = false)
    private LocalDateTime dateAlerte;

    @Convert(converter = OuiNonConverter.class)
    @Column(name = "LUE", nullable = false)
    private boolean lue;

    /** Budget concerné : sert à ne pas créer deux fois la même alerte pour un même budget. */
    @Column(name = "BUDGET_ID")
    private Long budgetId;

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

    public TypeAlerte getType() {
        return type;
    }

    public void setType(TypeAlerte type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDateAlerte() {
        return dateAlerte;
    }

    public void setDateAlerte(LocalDateTime dateAlerte) {
        this.dateAlerte = dateAlerte;
    }

    public boolean isLue() {
        return lue;
    }

    public void setLue(boolean lue) {
        this.lue = lue;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }
}
