package tn.smartbuy.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.email.EmailService;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Codes à 6 chiffres envoyés par e-mail, utilisés pour :
 * <ul>
 *   <li>confirmer qu'une adresse e-mail existe vraiment et appartient à la personne (inscription) ;</li>
 *   <li>choisir un nouveau mot de passe (mot de passe oublié, AUTH-05).</li>
 * </ul>
 * Règles : code haché (BCrypt), valable 15 minutes, utilisable une seule fois, bloqué après 5 essais faux ;
 * un nouveau code annule les précédents du même type.
 */
@Service
public class CodesEmailService {

    private static final Logger log = LoggerFactory.getLogger(CodesEmailService.class);
    static final int DUREE_MINUTES = 15;
    static final int ESSAIS_MAX = 5;
    static final String CODE_INVALIDE = "Code invalide ou expiré. Demandez un nouveau code.";

    private final CodeEmailRepository codes;
    private final PasswordEncoder encodeur;
    private final EmailService emails;
    private final boolean afficherCodeSiEchecEmail;
    private final Clock horloge;
    private final SecureRandom hasard = new SecureRandom();

    /** Constructeur utilisé par Spring (le second, avec une horloge fixe, sert aux tests). */
    @Autowired
    public CodesEmailService(CodeEmailRepository codes, PasswordEncoder encodeur, EmailService emails,
                             @Value("${smartbuy.dev.afficher-code-si-echec-email:true}") boolean afficherCodeSiEchecEmail) {
        this(codes, encodeur, emails, afficherCodeSiEchecEmail, Clock.systemDefaultZone());
    }

    CodesEmailService(CodeEmailRepository codes, PasswordEncoder encodeur, EmailService emails,
                      boolean afficherCodeSiEchecEmail, Clock horloge) {
        this.codes = codes;
        this.encodeur = encodeur;
        this.emails = emails;
        this.afficherCodeSiEchecEmail = afficherCodeSiEchecEmail;
        this.horloge = horloge;
    }

    /** Génère un nouveau code, l'enregistre haché et l'envoie par e-mail. */
    public void envoyer(Utilisateur u, CodeEmail.But but) {
        codes.findByUtilisateurIdAndButAndUtiliseFalse(u.getId(), but).forEach(ancien -> ancien.setUtilise(true));

        String code = "%06d".formatted(hasard.nextInt(1_000_000));
        CodeEmail c = new CodeEmail();
        c.setUtilisateurId(u.getId());
        c.setBut(but);
        c.setCodeHache(encodeur.encode(code));
        c.setExpiration(LocalDateTime.now(horloge).plusMinutes(DUREE_MINUTES));
        codes.save(c);

        boolean verification = but == CodeEmail.But.VERIFICATION;
        String sujet = verification ? "Confirmez votre adresse e-mail SmartBuy" : "Votre code de réinitialisation SmartBuy";
        String intro = verification
                ? "Bienvenue sur SmartBuy ! Pour activer votre compte, saisissez ce code dans l'application :"
                : "Pour choisir un nouveau mot de passe, saisissez ce code dans l'application :";
        boolean envoye = emails.envoyer(u.getEmail(), sujet, EmailService.gabarit(sujet, """
                <p>Bonjour %s,</p>
                <p>%s</p>
                <p><b style="font-size: 24px; letter-spacing: 6px;">%s</b></p>
                <p>Ce code est valable %d minutes. Si vous n'êtes pas à l'origine de cette demande, ignorez cet e-mail.</p>
                """.formatted(EmailService.echapper(u.getNom()), intro, code, DUREE_MINUTES)));

        // Si le serveur d'e-mails n'est pas configuré (développement), le code est écrit dans le journal
        // pour pouvoir continuer à tester. À désactiver en production : smartbuy.dev.afficher-code-si-echec-email=false
        if (!envoye && afficherCodeSiEchecEmail) {
            log.warn(">>> CODE {} pour {} : {}  (valable {} min)", but, u.getEmail(), code, DUREE_MINUTES);
        }
    }

    /**
     * Vérifie le code saisi. Lance {@link RequeteInvalideException} s'il est faux, expiré ou épuisé.
     * À appeler dans une transaction qui ne s'annule pas sur cette exception (le compteur d'essais doit rester).
     */
    public void verifier(Utilisateur u, CodeEmail.But but, String codeSaisi) {
        CodeEmail c = codes.findFirstByUtilisateurIdAndButAndUtiliseFalseOrderByIdDesc(u.getId(), but)
                .orElseThrow(() -> new RequeteInvalideException(CODE_INVALIDE));
        if (c.getExpiration().isBefore(LocalDateTime.now(horloge)) || c.getTentatives() >= ESSAIS_MAX) {
            c.setUtilise(true);
            throw new RequeteInvalideException(CODE_INVALIDE);
        }
        if (codeSaisi == null || !encodeur.matches(codeSaisi.trim(), c.getCodeHache())) {
            c.setTentatives(c.getTentatives() + 1);
            int restants = ESSAIS_MAX - c.getTentatives();
            throw new RequeteInvalideException(restants > 0
                    ? "Code incorrect (%d essai(s) restant(s))".formatted(restants)
                    : CODE_INVALIDE);
        }
        c.setUtilise(true);
    }
}
