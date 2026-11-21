package tn.smartbuy.alerte;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Règle BUD-11 : alertes 80 % / 100 %, une seule fois par seuil et par budget. */
class SeuilsAlerteTest {

    @Test
    @DisplayName("Sous 80 % : aucune alerte")
    void sousLeSeuil() {
        assertThat(SeuilsAlerte.aCreer(79.9, false, false)).isEmpty();
    }

    @Test
    @DisplayName("Passage de 80 % : alerte BUDGET_80")
    void seuil80() {
        assertThat(SeuilsAlerte.aCreer(80.0, false, false)).contains(TypeAlerte.BUDGET_80);
        assertThat(SeuilsAlerte.aCreer(95.0, false, false)).contains(TypeAlerte.BUDGET_80);
    }

    @Test
    @DisplayName("80 % déjà signalé : pas de doublon")
    void pasDeDoublon80() {
        assertThat(SeuilsAlerte.aCreer(90.0, true, false)).isEmpty();
    }

    @Test
    @DisplayName("Passage de 100 % après l'alerte 80 % : alerte BUDGET_100")
    void seuil100() {
        assertThat(SeuilsAlerte.aCreer(100.0, true, false)).contains(TypeAlerte.BUDGET_100);
    }

    @Test
    @DisplayName("Saut direct de 50 % à 120 % : seulement BUDGET_100")
    void sautDirect() {
        assertThat(SeuilsAlerte.aCreer(120.0, false, false)).contains(TypeAlerte.BUDGET_100);
    }

    @Test
    @DisplayName("100 % déjà signalé : plus aucune alerte (ni 100 %, ni 80 %)")
    void pasDeDoublon100() {
        assertThat(SeuilsAlerte.aCreer(130.0, true, true)).isEmpty();
        assertThat(SeuilsAlerte.aCreer(130.0, false, true)).isEmpty();
        assertThat(SeuilsAlerte.aCreer(85.0, false, true)).isEmpty();
    }

    @Test
    @DisplayName("Message au format du contrat")
    void message() {
        assertThat(SeuilsAlerte.message("Alimentation", TypeAlerte.BUDGET_80))
                .isEqualTo("Alimentation : 80 % du budget atteint");
    }
}
