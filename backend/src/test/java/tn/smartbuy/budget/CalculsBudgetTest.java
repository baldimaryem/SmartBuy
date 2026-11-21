package tn.smartbuy.budget;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.smartbuy.budget.BudgetDtos.EvolutionDto;
import tn.smartbuy.budget.BudgetDtos.PrevisionDto;
import tn.smartbuy.budget.BudgetDtos.RepartitionDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests de la prévision (BUD-15), de la répartition (BUD-13) et de l'évolution (BUD-14). */
class CalculsBudgetTest {

    private final Categorie alimentation = new Categorie(1L, "Alimentation", TypeOperation.DEPENSE, "shopping_cart", null);
    private final Categorie loisirs = new Categorie(4L, "Loisirs", TypeOperation.DEPENSE, "sports_esports", null);
    private final Categorie salaire = new Categorie(8L, "Salaire", TypeOperation.REVENU, "work", null);

    @Test
    @DisplayName("BUD-15 : mois en cours = dépensé ÷ jours écoulés × jours du mois")
    void previsionMoisEnCours() {
        // 10 novembre 2026 (30 jours) : 500 dépensés -> 500 / 10 × 30 = 1500
        PrevisionDto p = CalculsBudget.prevoir(new BigDecimal("500"), YearMonth.of(2026, 11),
                LocalDate.of(2026, 11, 10), new BigDecimal("1200"));

        assertThat(p.depenseEstimeeFinMois()).isEqualTo(1500.0);
        assertThat(p.depassementProbable()).isTrue();
    }

    @Test
    @DisplayName("BUD-15 : pas de dépassement si la prévision reste sous le budget global")
    void previsionSansDepassement() {
        PrevisionDto p = CalculsBudget.prevoir(new BigDecimal("300"), YearMonth.of(2026, 11),
                LocalDate.of(2026, 11, 15), new BigDecimal("1200"));

        assertThat(p.depenseEstimeeFinMois()).isEqualTo(600.0);
        assertThat(p.depassementProbable()).isFalse();
    }

    @Test
    @DisplayName("BUD-15 : mois passé = total réel, sans extrapolation")
    void previsionMoisPasse() {
        PrevisionDto p = CalculsBudget.prevoir(new BigDecimal("950.5"), YearMonth.of(2026, 10),
                LocalDate.of(2026, 11, 3), new BigDecimal("900"));

        assertThat(p.depenseEstimeeFinMois()).isEqualTo(950.5);
        assertThat(p.depassementProbable()).isTrue();
    }

    @Test
    @DisplayName("BUD-15 : sans budget global, depassementProbable vaut toujours false")
    void previsionSansBudget() {
        PrevisionDto p = CalculsBudget.prevoir(new BigDecimal("1000"), YearMonth.of(2026, 11),
                LocalDate.of(2026, 11, 1), null);

        assertThat(p.depenseEstimeeFinMois()).isEqualTo(30000.0);
        assertThat(p.depassementProbable()).isFalse();
    }

    @Test
    @DisplayName("BUD-13 : répartition par catégorie, de la plus grosse à la plus petite, sans les revenus")
    void repartition() {
        List<Operation> ops = List.of(
                op(alimentation, "120", "2026-11-02"), op(loisirs, "200", "2026-11-03"),
                op(alimentation, "100.5", "2026-11-04"), op(salaire, "1800", "2026-11-01"));

        List<RepartitionDto> r = CalculsBudget.repartition(ops);

        assertThat(r).extracting(RepartitionDto::categorieNom).containsExactly("Alimentation", "Loisirs");
        assertThat(r.get(0).montant()).isEqualTo(220.5);
    }

    @Test
    @DisplayName("BUD-14 : 6 mois du plus ancien au plus récent, 0 pour un mois vide")
    void evolution() {
        List<Operation> ops = List.of(
                op(alimentation, "100", "2026-11-02"), op(salaire, "1800", "2026-11-01"),
                op(loisirs, "50", "2026-09-15"), op(alimentation, "999", "2026-04-30"));

        List<EvolutionDto> e = CalculsBudget.evolution(ops, YearMonth.of(2026, 11), 6);

        assertThat(e).extracting(EvolutionDto::mois)
                .containsExactly("2026-06", "2026-07", "2026-08", "2026-09", "2026-10", "2026-11");
        assertThat(e.get(5).depenses()).isEqualTo(100.0);
        assertThat(e.get(5).revenus()).isEqualTo(1800.0);
        assertThat(e.get(3).depenses()).isEqualTo(50.0);
        assertThat(e.get(4).depenses()).isEqualTo(0.0); // octobre vide ; avril est hors période
    }

    @Test
    @DisplayName("LNK-03 : somme des économies du mois")
    void economies() {
        Operation achat = op(alimentation, "41.2", "2026-11-05");
        achat.setEconomie(new BigDecimal("6.8"));
        Operation autre = op(alimentation, "10", "2026-11-06");
        autre.setEconomie(null);

        assertThat(CalculsBudget.economies(List.of(achat, autre))).isEqualByComparingTo("6.8");
    }

    private static Operation op(Categorie c, String montant, String date) {
        Operation o = new Operation();
        o.setCategorie(c);
        o.setType(c.getType());
        o.setMontant(new BigDecimal(montant));
        o.setDateOperation(LocalDate.parse(date));
        return o;
    }
}
