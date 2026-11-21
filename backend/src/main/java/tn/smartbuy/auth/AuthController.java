package tn.smartbuy.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.smartbuy.auth.AuthDtos.AuthReponse;
import tn.smartbuy.auth.AuthDtos.ConnexionRequete;
import tn.smartbuy.auth.AuthDtos.DemandeCodeRequete;
import tn.smartbuy.auth.AuthDtos.InscriptionReponse;
import tn.smartbuy.auth.AuthDtos.VerificationEmailRequete;
import tn.smartbuy.auth.AuthDtos.MessageReponse;
import tn.smartbuy.auth.AuthDtos.ReinitialisationRequete;
import tn.smartbuy.auth.AuthDtos.InscriptionRequete;
import tn.smartbuy.auth.AuthDtos.ProfilRequete;
import tn.smartbuy.auth.AuthDtos.UtilisateurDto;
import tn.smartbuy.securite.UtilisateurConnecte;

/** Routes /api/auth (AUTH-01..04). Le contrôleur ne fait que recevoir / renvoyer : la logique est dans AuthService. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MotDePasseService motDePasseService;

    public AuthController(AuthService authService, MotDePasseService motDePasseService) {
        this.authService = authService;
        this.motDePasseService = motDePasseService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public InscriptionReponse inscrire(@Valid @RequestBody InscriptionRequete requete) {
        String email = authService.inscrire(requete);
        return new InscriptionReponse("Un code de confirmation a été envoyé à " + email + ".", email);
    }

    /** Inscription étape 2 : le code reçu par e-mail confirme l'adresse et connecte l'utilisateur. */
    @PostMapping("/verifier-email")
    public AuthReponse verifierEmail(@Valid @RequestBody VerificationEmailRequete requete) {
        return authService.verifierEmail(requete.email(), requete.code());
    }

    @PostMapping("/renvoyer-code")
    public MessageReponse renvoyerCode(@Valid @RequestBody DemandeCodeRequete requete) {
        authService.renvoyerCodeVerification(requete.email());
        return new MessageReponse("Si ce compte attend une confirmation, un nouveau code vient d'être envoyé.");
    }

    @PostMapping("/login")
    public AuthReponse connecter(@Valid @RequestBody ConnexionRequete requete) {
        return authService.connecter(requete);
    }

    /** AUTH-05 étape 1 : toujours la même réponse, que le compte existe ou non. */
    @PostMapping("/mot-de-passe-oublie")
    public MessageReponse motDePasseOublie(@Valid @RequestBody DemandeCodeRequete requete) {
        motDePasseService.demanderCode(requete.email());
        return new MessageReponse("Si un compte existe avec cet e-mail, un code de réinitialisation vient d'être envoyé.");
    }

    /** AUTH-05 étape 2. */
    @PostMapping("/reinitialiser-mot-de-passe")
    public MessageReponse reinitialiser(@Valid @RequestBody ReinitialisationRequete requete) {
        motDePasseService.reinitialiser(requete.email(), requete.code(), requete.nouveauMotDePasse());
        return new MessageReponse("Mot de passe modifié. Vous pouvez vous connecter.");
    }

    @GetMapping("/me")
    public UtilisateurDto moi(@AuthenticationPrincipal UtilisateurConnecte moi) {
        return authService.profil(moi.id());
    }

    @PutMapping("/me")
    public UtilisateurDto modifier(@AuthenticationPrincipal UtilisateurConnecte moi,
                                   @Valid @RequestBody ProfilRequete requete) {
        return authService.modifierProfil(moi.id(), requete);
    }
}
