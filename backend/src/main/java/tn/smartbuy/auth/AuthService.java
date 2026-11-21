package tn.smartbuy.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.auth.AuthDtos.AuthReponse;
import tn.smartbuy.auth.AuthDtos.ConnexionRequete;
import tn.smartbuy.auth.AuthDtos.InscriptionRequete;
import tn.smartbuy.auth.AuthDtos.ProfilRequete;
import tn.smartbuy.auth.AuthDtos.UtilisateurDto;
import tn.smartbuy.commun.AccesInterditException;
import tn.smartbuy.commun.ConflitException;
import tn.smartbuy.commun.IdentifiantsInvalidesException;
import tn.smartbuy.commun.Montants;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.commun.RessourceIntrouvableException;
import tn.smartbuy.securite.JwtService;

import java.util.Locale;

/**
 * Règles métier des comptes : inscription avec confirmation de l'e-mail, connexion, profil.
 * <p>
 * Inscription en deux temps (pour n'accepter que des adresses réelles) :
 * <ol>
 *   <li>{@link #inscrire} : le domaine doit recevoir des e-mails, le compte est créé NON confirmé
 *       et un code à 6 chiffres est envoyé à l'adresse ;</li>
 *   <li>{@link #verifierEmail} : le bon code confirme l'adresse et connecte l'utilisateur.</li>
 * </ol>
 * Un compte non confirmé ne peut pas se connecter.
 */
@Service
public class AuthService {

    static final String EMAIL_NON_CONFIRME = "Adresse e-mail non confirmée : saisissez le code reçu par e-mail.";

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encodeur;
    private final JwtService jwtService;
    private final CodesEmailService codes;
    private final VerificateurDomaineEmail domaines;
    private final String emailAdmin;

    public AuthService(UtilisateurRepository utilisateurs, PasswordEncoder encodeur, JwtService jwtService,
                       CodesEmailService codes, VerificateurDomaineEmail domaines,
                       @Value("${smartbuy.admin.email:}") String emailAdmin) {
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
        this.jwtService = jwtService;
        this.codes = codes;
        this.domaines = domaines;
        this.emailAdmin = normaliser(emailAdmin);
    }

    /**
     * AUTH-01 étape 1 : créer le compte (non confirmé) et envoyer le code.
     * Si l'e-mail est déjà inscrit mais jamais confirmé, on met à jour le nom et le mot de passe
     * et on renvoie un code (la personne a pu se tromper ou perdre l'e-mail).
     */
    @Transactional
    public String inscrire(InscriptionRequete requete) {
        String email = normaliser(requete.email());
        domaines.verifier(email);

        Utilisateur u = utilisateurs.findByEmail(email).orElse(null);
        if (u != null && u.isEmailVerifie()) {
            throw new ConflitException("Un compte existe déjà avec cet e-mail");
        }
        if (u == null) {
            u = new Utilisateur();
            u.setEmail(email);
            u.setRole(email.equals(emailAdmin) ? Role.ADMIN : Role.USER);
        }
        u.setNom(requete.nom().trim());
        u.setMotDePasseHache(encodeur.encode(requete.motDePasse()));
        u.setEmailVerifie(false);
        utilisateurs.save(u);

        codes.envoyer(u, CodeEmail.But.VERIFICATION);
        return email;
    }

    /** AUTH-01 étape 2 : le bon code confirme l'adresse et connecte directement. */
    @Transactional(noRollbackFor = RequeteInvalideException.class)
    public AuthReponse verifierEmail(String email, String code) {
        Utilisateur u = utilisateurs.findByEmail(normaliser(email))
                .orElseThrow(() -> new RequeteInvalideException(CodesEmailService.CODE_INVALIDE));
        if (!u.isEmailVerifie()) {
            codes.verifier(u, CodeEmail.But.VERIFICATION, code);
            u.setEmailVerifie(true);
        }
        return reponseAvecJeton(u);
    }

    /** Renvoyer un code de confirmation (rien de visible si le compte n'existe pas ou est déjà confirmé). */
    @Transactional
    public void renvoyerCodeVerification(String email) {
        utilisateurs.findByEmail(normaliser(email))
                .filter(u -> !u.isEmailVerifie())
                .ifPresent(u -> codes.envoyer(u, CodeEmail.But.VERIFICATION));
    }

    /** AUTH-02 / AUTH-06 : connexion (le rôle ADMIN est dans le jeton). */
    @Transactional(readOnly = true)
    public AuthReponse connecter(ConnexionRequete requete) {
        Utilisateur u = utilisateurs.findByEmail(normaliser(requete.email()))
                .filter(trouve -> encodeur.matches(requete.motDePasse(), trouve.getMotDePasseHache()))
                // même message dans les deux cas : on ne révèle pas si l'e-mail existe
                .orElseThrow(() -> new IdentifiantsInvalidesException("E-mail ou mot de passe incorrect"));
        if (!u.isEmailVerifie()) {
            // 403 : l'application ouvre alors l'écran de saisie du code
            throw new AccesInterditException(EMAIL_NON_CONFIRME);
        }
        return reponseAvecJeton(u);
    }

    @Transactional(readOnly = true)
    public UtilisateurDto profil(Long utilisateurId) {
        return UtilisateurDto.depuis(charger(utilisateurId));
    }

    /** AUTH-04 : modifier nom, devise et revenu mensuel habituel. */
    @Transactional
    public UtilisateurDto modifierProfil(Long utilisateurId, ProfilRequete requete) {
        Utilisateur u = charger(utilisateurId);
        u.setNom(requete.nom().trim());
        if (requete.devise() != null) {
            u.setDevise(requete.devise().toUpperCase(Locale.ROOT));
        }
        u.setRevenuMensuel(Montants.depuisDouble(requete.revenuMensuel()));
        return UtilisateurDto.depuis(u);
    }

    private Utilisateur charger(Long id) {
        return utilisateurs.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
    }

    private AuthReponse reponseAvecJeton(Utilisateur u) {
        String jeton = jwtService.genererJeton(u.getId(), u.getEmail(), u.getRole().name());
        return new AuthReponse(jeton, UtilisateurDto.depuis(u));
    }

    private static String normaliser(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
