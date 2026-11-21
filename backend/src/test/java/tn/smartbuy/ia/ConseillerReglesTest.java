package tn.smartbuy.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.smartbuy.ia.IaDtos.Conseil;
import tn.smartbuy.ia.IaDtos.Niveau;
import tn.smartbuy.ia.ResumeMois.DepenseCategorie;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Moteur de règles BUD-16. */
class ConseillerReglesTest {

    private final ConseillerRegles regles = new ConseillerRegles();

    @Test
    @DisplayName("Hausse de plus de 30 % d'une catégorie par rapport à la moyenne de 3 mois")
    void hausseDeCategorie() {
        ResumeMois r = resume(1800, 700, null, 700, false,
                List.of(new DepenseCategorie("Loisirs", 140, 55, null)), null);

        List<Conseil> conseils = regles.conseils(r);

        assertThat(conseils).anySatisfy(c -> {
            assertThat(c.titre()).isEqualTo("Hausse des dépenses Loisirs");
            assertThat(c.message()).contains("155 %");
            assertThat(c.niveau()).isEqualTo(Niveau.ATTENTION);
        });
    }

    @Test
    @DisplayName("Pas de conseil de hausse sous 30 %")
    void petiteHausse() {
        ResumeMois r = resume(1800, 700, null, 700, false,
                List.of(new DepenseCategorie("Loisirs", 60, 55, null)), null);

        assertThat(regles.conseils(r)).noneMatch(c -> c.titre().startsWith("Hausse"));
    }

    @Test
    @DisplayName("Budget de catégorie à plus de 80 % : ATTENTION ; dépassé : ALERTE")
    void budgetCategorie() {
        ResumeMois r = resume(1800, 900, null, 900, false, List.of(
                new DepenseCategorie("Alimentation", 340, 300, 400.0),
                new DepenseCategorie("Transport", 120, 100, 100.0)), null);

        List<Conseil> conseils = regles.conseils(r);

        assertThat(conseils).anySatisfy(c -> {
            assertThat(c.titre()).isEqualTo("Budget Alimentation presque atteint");
            assertThat(c.niveau()).isEqualTo(Niveau.ATTENTION);
        });
        assertThat(conseils).anySatisfy(c -> {
            assertThat(c.titre()).isEqualTo("Budget Transport dépassé");
            assertThat(c.niveau()).isEqualTo(Niveau.ALERTE);
        });
        // les ALERTE sont en tête de liste
        assertThat(conseils.get(0).niveau()).isEqualTo(Niveau.ALERTE);
    }

    @Test
    @DisplayName("Dépassement probable du budget global d'après la prévision")
    void depassementProbable() {
        ResumeMois r = resume(1800, 600, 1000.0, 1500, true, List.of(), null);

        assertThat(regles.conseils(r)).anySatisfy(c -> {
            assertThat(c.titre()).isEqualTo("Dépassement probable");
            assertThat(c.niveau()).isEqualTo(Niveau.ALERTE);
        });
    }

    @Test
    @DisplayName("Taux d'épargne : négatif -> ALERTE, faible -> ATTENTION, bon -> INFO")
    void tauxEpargne() {
        assertThat(regles.conseils(resume(1000, 1200, null, 1200, false, List.of(), null)))
                .anyMatch(c -> c.titre().equals("Dépenses supérieures aux revenus") && c.niveau() == Niveau.ALERTE);
        assertThat(regles.conseils(resume(1000, 950, null, 950, false, List.of(), null)))
                .anyMatch(c -> c.titre().equals("Épargne faible") && c.niveau() == Niveau.ATTENTION);
        assertThat(regles.conseils(resume(1000, 500, null, 500, false, List.of(), null)))
                .anyMatch(c -> c.titre().equals("Bonne gestion") && c.niveau() == Niveau.INFO);
    }

    @Test
    @DisplayName("Conseil sur le magasin le moins cher")
    void meilleurMagasin() {
        ResumeMois r = resume(1800, 500, null, 500, false, List.of(), "Aziza Ariana");

        assertThat(regles.conseils(r)).anyMatch(c -> c.message().startsWith("Aziza Ariana est le moins cher"));
    }

    @Test
    @DisplayName("Assistant : reconnaît une question sur le reste à dépenser (accents ignorés)")
    void assistantReste() {
        ResumeMois r = resume(1800, 950, 1200.0, 1425, true, List.of(), null);

        String reponse = regles.repondre("Combien il me RESTE à dépenser ?", r);

        assertThat(reponse).startsWith("Il vous reste 250,000 TND");
    }

    @Test
    @DisplayName("Assistant : question inconnue -> résumé et suggestions")
    void assistantParDefaut() {
        ResumeMois r = resume(1800, 950, null, 950, false, List.of(), null);

        assertThat(regles.repondre("Quel temps fait-il ?", r)).contains("Vous pouvez me demander");
    }

    private static ResumeMois resume(double revenus, double depenses, Double budgetGlobal, double prevision,
                                     boolean enCours, List<DepenseCategorie> categories, String magasin) {
        return new ResumeMois("2026-11", "TND", enCours, revenus, depenses, 1800.0, budgetGlobal, prevision, 0,
                categories, magasin, magasin == null ? 0 : 12, magasin == null ? 0 : 27);
    }
}
