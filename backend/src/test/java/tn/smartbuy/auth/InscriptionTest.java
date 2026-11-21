package tn.smartbuy.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.smartbuy.auth.AuthDtos.ConnexionRequete;
import tn.smartbuy.auth.AuthDtos.InscriptionRequete;
import tn.smartbuy.commun.AccesInterditException;
import tn.smartbuy.commun.ConflitException;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.securite.JwtService;

import javax.naming.NameNotFoundException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Inscription avec e-mail réel : domaine qui reçoit des e-mails + code de confirmation obligatoire. */
@ExtendWith(MockitoExtension.class)
class InscriptionTest {

    @Mock
    private UtilisateurRepository utilisateurs;
    @Mock
    private JwtService jwt;
    @Mock
    private CodesEmailService codes;

    private final PasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private AuthService service;

    /** Faux DNS : gmail.com a un serveur MX, smartbuy.tn n'existe pas, example.com a un « MX nul ». */
    private final VerificateurDomaineEmail domaines = new VerificateurDomaineEmail(domaine -> switch (domaine) {
        case "gmail.com" -> List.of("5 gmail-smtp-in.l.google.com.");
        case "example.com" -> List.of("0 .");
        default -> throw new NameNotFoundException(domaine);
    }, true);

    @BeforeEach
    void preparer() {
        service = new AuthService(utilisateurs, encodeur, jwt, codes, domaines, "admin.smartbuy@gmail.com");
    }

    @Test
    @DisplayName("domaine qui ne reçoit pas d'e-mails (smartbuy.tn, example.com) → refusé")
    void domainesFictifsRefuses() {
        assertThatThrownBy(() -> domaines.verifier("demo@smartbuy.tn"))
                .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("smartbuy.tn");
        assertThatThrownBy(() -> domaines.verifier("test@example.com")).isInstanceOf(RequeteInvalideException.class);
        assertThatCode(() -> domaines.verifier("maryem@gmail.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("inscription : compte créé NON confirmé et code envoyé, pas encore de jeton")
    void inscriptionEnvoieCode() {
        when(utilisateurs.findByEmail("maryem@gmail.com")).thenReturn(Optional.empty());
        when(utilisateurs.save(any())).thenAnswer(i -> i.getArgument(0));

        String email = service.inscrire(new InscriptionRequete("Maryem", " Maryem@Gmail.com ", "secret123"));

        assertThat(email).isEqualTo("maryem@gmail.com");
        verify(codes).envoyer(any(Utilisateur.class), any());
        verify(jwt, never()).genererJeton(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("inscription refusée pour un domaine fictif, rien n'est enregistré")
    void inscriptionDomaineFictif() {
        assertThatThrownBy(() -> service.inscrire(new InscriptionRequete("Démo", "demo@smartbuy.tn", "demo1234")))
                .isInstanceOf(RequeteInvalideException.class);
        verify(utilisateurs, never()).save(any());
    }

    @Test
    @DisplayName("e-mail déjà confirmé → 409 ; l'adresse de l'admin configuré reçoit le rôle ADMIN")
    void conflitEtAdmin() {
        Utilisateur existant = new Utilisateur();
        existant.setEmailVerifie(true);
        when(utilisateurs.findByEmail("maryem@gmail.com")).thenReturn(Optional.of(existant));
        assertThatThrownBy(() -> service.inscrire(new InscriptionRequete("Maryem", "maryem@gmail.com", "secret123")))
                .isInstanceOf(ConflitException.class);

        when(utilisateurs.findByEmail("admin.smartbuy@gmail.com")).thenReturn(Optional.empty());
        when(utilisateurs.save(any())).thenAnswer(i -> i.getArgument(0));
        service.inscrire(new InscriptionRequete("Admin", "admin.smartbuy@gmail.com", "secret123"));
        verify(utilisateurs).save(org.mockito.ArgumentMatchers.argThat(u -> u.getRole() == Role.ADMIN));
    }

    @Test
    @DisplayName("connexion d'un compte non confirmé → 403 avec message explicite")
    void connexionNonConfirmee() {
        Utilisateur u = new Utilisateur();
        u.setEmail("maryem@gmail.com");
        u.setMotDePasseHache(encodeur.encode("secret123"));
        u.setRole(Role.USER);
        when(utilisateurs.findByEmail("maryem@gmail.com")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.connecter(new ConnexionRequete("maryem@gmail.com", "secret123")))
                .isInstanceOf(AccesInterditException.class).hasMessage(AuthService.EMAIL_NON_CONFIRME);

        u.setEmailVerifie(true);
        u.setId(3L);
        assertThatCode(() -> service.connecter(new ConnexionRequete("maryem@gmail.com", "secret123")))
                .doesNotThrowAnyException();
    }
}
