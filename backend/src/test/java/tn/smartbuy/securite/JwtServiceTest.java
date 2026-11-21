package tn.smartbuy.securite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Création puis lecture d'un jeton JWT (aller-retour), et rejet des jetons invalides. */
class JwtServiceTest {

    private static final String SECRET = "cle-de-test-suffisamment-longue-pour-hs256-0123456789";

    @Test
    @DisplayName("Aller-retour : on retrouve l'id, l'e-mail et le rôle")
    void allerRetour() {
        JwtService jwt = new JwtService(SECRET, 24);

        String jeton = jwt.genererJeton(42L, "demo@smartbuy.tn", "USER");

        assertThat(jwt.lireJeton(jeton)).contains(new UtilisateurConnecte(42L, "demo@smartbuy.tn", "USER"));
    }

    @Test
    @DisplayName("Jeton modifié : refusé")
    void jetonModifie() {
        JwtService jwt = new JwtService(SECRET, 24);
        String jeton = jwt.genererJeton(1L, "admin@smartbuy.tn", "ADMIN");
        String falsifie = jeton.substring(0, jeton.length() - 2) + (jeton.endsWith("A") ? "BB" : "AA");

        assertThat(jwt.lireJeton(falsifie)).isEmpty();
        assertThat(jwt.lireJeton("n'importe quoi")).isEmpty();
    }

    @Test
    @DisplayName("Jeton signé avec une autre clé : refusé")
    void autreCle() {
        String jeton = new JwtService(SECRET, 24).genererJeton(1L, "a@b.tn", "USER");
        JwtService autre = new JwtService("une-autre-cle-secrete-tout-aussi-longue-9876543210", 24);

        assertThat(autre.lireJeton(jeton)).isEmpty();
    }

    @Test
    @DisplayName("Jeton expiré : refusé")
    void jetonExpire() {
        JwtService jwt = new JwtService(SECRET, 0); // validité de 0 heure : expiré immédiatement
        String jeton = jwt.genererJeton(1L, "a@b.tn", "USER");

        assertThat(jwt.lireJeton(jeton)).isEmpty();
    }

    @Test
    @DisplayName("Clé trop courte : erreur au démarrage")
    void cleTropCourte() {
        assertThatThrownBy(() -> new JwtService("trop-courte", 24)).isInstanceOf(IllegalStateException.class);
    }
}
