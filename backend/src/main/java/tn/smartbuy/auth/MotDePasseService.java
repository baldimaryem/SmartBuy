package tn.smartbuy.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.commun.RequeteInvalideException;

import java.util.Locale;

/**
 * AUTH-05 : mot de passe oublié, en deux étapes.
 * <ol>
 *   <li>l'utilisateur donne son e-mail → un code à 6 chiffres lui est envoyé ({@link CodesEmailService}) ;</li>
 *   <li>il saisit le code et son nouveau mot de passe → si le code est bon, le mot de passe est remplacé.</li>
 * </ol>
 * La réponse de l'étape 1 est identique que l'e-mail existe ou non : on ne révèle pas qui a un compte.
 */
@Service
public class MotDePasseService {

    private static final Logger log = LoggerFactory.getLogger(MotDePasseService.class);

    private final UtilisateurRepository utilisateurs;
    private final CodesEmailService codes;
    private final PasswordEncoder encodeur;

    public MotDePasseService(UtilisateurRepository utilisateurs, CodesEmailService codes, PasswordEncoder encodeur) {
        this.utilisateurs = utilisateurs;
        this.codes = codes;
        this.encodeur = encodeur;
    }

    @Transactional
    public void demanderCode(String email) {
        utilisateurs.findByEmail(normaliser(email)).ifPresent(u -> codes.envoyer(u, CodeEmail.But.REINITIALISATION));
    }

    @Transactional(noRollbackFor = RequeteInvalideException.class) // le compteur d'essais doit être enregistré
    public void reinitialiser(String email, String code, String nouveauMotDePasse) {
        Utilisateur u = utilisateurs.findByEmail(normaliser(email))
                .orElseThrow(() -> new RequeteInvalideException(CodesEmailService.CODE_INVALIDE));
        codes.verifier(u, CodeEmail.But.REINITIALISATION, code);
        u.setMotDePasseHache(encodeur.encode(nouveauMotDePasse));
        // Recevoir le code prouve aussi que l'adresse est réelle.
        u.setEmailVerifie(true);
        log.info("Mot de passe réinitialisé pour {}", u.getEmail());
    }

    private static String normaliser(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
