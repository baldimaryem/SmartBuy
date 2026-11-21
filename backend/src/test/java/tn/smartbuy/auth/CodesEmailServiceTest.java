package tn.smartbuy.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.email.EmailService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Codes envoyés par e-mail (confirmation d'adresse et mot de passe oublié). */
@ExtendWith(MockitoExtension.class)
class CodesEmailServiceTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneId.of("UTC"));
    private static final CodeEmail.But VERIF = CodeEmail.But.VERIFICATION;

    @Mock
    private CodeEmailRepository codes;
    @Mock
    private EmailService emails;

    private final PasswordEncoder encodeur = new BCryptPasswordEncoder(4); // coût faible : tests rapides
    private CodesEmailService service;
    private Utilisateur maryem;

    @BeforeEach
    void preparer() {
        service = new CodesEmailService(codes, encodeur, emails, false, HORLOGE);
        maryem = new Utilisateur();
        maryem.setId(7L);
        maryem.setNom("Maryem");
        maryem.setEmail("maryem@gmail.com");
    }

    private CodeEmail code(String code, LocalDateTime expiration, int tentatives) {
        CodeEmail c = new CodeEmail();
        c.setUtilisateurId(7L);
        c.setBut(VERIF);
        c.setCodeHache(encodeur.encode(code));
        c.setExpiration(expiration);
        c.setTentatives(tentatives);
        return c;
    }

    @Test
    @DisplayName("envoi : code à 6 chiffres envoyé par e-mail, stocké haché, valable 15 min, anciens codes annulés")
    void envoi() {
        CodeEmail ancien = code("111111", LocalDateTime.now(HORLOGE).plusMinutes(5), 0);
        when(codes.findByUtilisateurIdAndButAndUtiliseFalse(7L, VERIF)).thenReturn(List.of(ancien));
        when(emails.envoyer(anyString(), anyString(), anyString())).thenReturn(true);

        service.envoyer(maryem, VERIF);

        ArgumentCaptor<CodeEmail> enregistre = ArgumentCaptor.forClass(CodeEmail.class);
        verify(codes).save(enregistre.capture());
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emails).envoyer(eq("maryem@gmail.com"), anyString(), html.capture());

        Matcher m = Pattern.compile(">(\\d{6})<").matcher(html.getValue());
        assertThat(m.find()).isTrue();
        assertThat(encodeur.matches(m.group(1), enregistre.getValue().getCodeHache())).isTrue();
        assertThat(enregistre.getValue().getCodeHache()).doesNotContain(m.group(1));
        assertThat(enregistre.getValue().getExpiration()).isEqualTo(LocalDateTime.now(HORLOGE).plusMinutes(15));
        assertThat(ancien.isUtilise()).isTrue();
    }

    @Test
    @DisplayName("vérification : bon code accepté une seule fois")
    void bonCode() {
        CodeEmail c = code("123456", LocalDateTime.now(HORLOGE).plusMinutes(10), 0);
        when(codes.findFirstByUtilisateurIdAndButAndUtiliseFalseOrderByIdDesc(7L, VERIF)).thenReturn(Optional.of(c));
        service.verifier(maryem, VERIF, "123456");
        assertThat(c.isUtilise()).isTrue();
    }

    @Test
    @DisplayName("vérification : mauvais code refusé et compté")
    void mauvaisCode() {
        CodeEmail c = code("123456", LocalDateTime.now(HORLOGE).plusMinutes(10), 0);
        when(codes.findFirstByUtilisateurIdAndButAndUtiliseFalseOrderByIdDesc(7L, VERIF)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.verifier(maryem, VERIF, "000000"))
                .isInstanceOf(RequeteInvalideException.class).hasMessageContaining("4 essai");
        assertThat(c.getTentatives()).isEqualTo(1);
        assertThat(c.isUtilise()).isFalse();
    }

    @Test
    @DisplayName("vérification : code expiré refusé même s'il est juste")
    void expire() {
        CodeEmail c = code("123456", LocalDateTime.now(HORLOGE).minusMinutes(1), 0);
        when(codes.findFirstByUtilisateurIdAndButAndUtiliseFalseOrderByIdDesc(7L, VERIF)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.verifier(maryem, VERIF, "123456")).hasMessage(CodesEmailService.CODE_INVALIDE);
    }

    @Test
    @DisplayName("vérification : bloqué après 5 essais faux")
    void tropDEssais() {
        CodeEmail c = code("123456", LocalDateTime.now(HORLOGE).plusMinutes(10), 5);
        when(codes.findFirstByUtilisateurIdAndButAndUtiliseFalseOrderByIdDesc(7L, VERIF)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.verifier(maryem, VERIF, "123456")).isInstanceOf(RequeteInvalideException.class);
    }
}
