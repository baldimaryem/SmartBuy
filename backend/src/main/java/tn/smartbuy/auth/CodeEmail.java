package tn.smartbuy.auth;

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

/** Table CODE_EMAIL : code à 6 chiffres envoyé par e-mail (confirmation d'adresse ou mot de passe oublié). */
@Entity
@Table(name = "CODE_EMAIL")
public class CodeEmail {

    public enum But { VERIFICATION, REINITIALISATION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "UTILISATEUR_ID", nullable = false)
    private Long utilisateurId;

    @Enumerated(EnumType.STRING)
    @Column(name = "BUT", nullable = false)
    private But but;

    /** Empreinte BCrypt du code : même si la base fuite, les codes restent illisibles. */
    @Column(name = "CODE_HACHE", nullable = false)
    private String codeHache;

    @Column(name = "EXPIRATION", nullable = false)
    private LocalDateTime expiration;

    @Column(name = "TENTATIVES", nullable = false)
    private int tentatives;

    @Convert(converter = OuiNonConverter.class)
    @Column(name = "UTILISE", nullable = false)
    private boolean utilise;

    @Column(name = "DATE_CREATION", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Long utilisateurId) { this.utilisateurId = utilisateurId; }
    public But getBut() { return but; }
    public void setBut(But but) { this.but = but; }
    public String getCodeHache() { return codeHache; }
    public void setCodeHache(String codeHache) { this.codeHache = codeHache; }
    public LocalDateTime getExpiration() { return expiration; }
    public void setExpiration(LocalDateTime expiration) { this.expiration = expiration; }
    public int getTentatives() { return tentatives; }
    public void setTentatives(int tentatives) { this.tentatives = tentatives; }
    public boolean isUtilise() { return utilise; }
    public void setUtilise(boolean utilise) { this.utilise = utilise; }
}
