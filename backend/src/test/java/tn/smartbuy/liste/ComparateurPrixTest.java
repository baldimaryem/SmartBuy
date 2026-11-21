package tn.smartbuy.liste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tn.smartbuy.liste.ComparateurPrix.Article;
import tn.smartbuy.liste.ComparateurPrix.Resultat;
import tn.smartbuy.liste.ComparateurPrix.ResultatMagasin;
import tn.smartbuy.prix.Magasin;
import tn.smartbuy.prix.Prix;
import tn.smartbuy.prix.Produit;
import tn.smartbuy.prix.StatutPrix;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests de l'algorithme PRX-11 / PRX-12 / PRX-13 / LNK-01 / LNK-03 (sans base de données). */
class ComparateurPrixTest {

    private final Magasin aziza = new Magasin(1L, "Aziza Ariana", "Aziza");
    private final Magasin carrefour = new Magasin(2L, "Carrefour La Marsa", "Carrefour");
    private final Magasin monoprix = new Magasin(3L, "Monoprix Lafayette", "Monoprix");
    private final Produit lait = new Produit(10L, "Lait 1L", "L");
    private final Produit pates = new Produit(11L, "Spaghetti 500g", "pièce");
    private final Produit beurre = new Produit(12L, "Beurre 200g", "pièce");

    /** produitId -> (magasinId -> prix) */
    private final Map<Long, Map<Long, Prix>> prix = new HashMap<>();
    private long idPrix = 100;

    private List<Article> liste;

    @BeforeEach
    void preparer() {
        // Lait : Aziza 1.300, Carrefour 1.350, Monoprix 1.450
        prix(lait, aziza, "1.300");
        prix(lait, carrefour, "1.350");
        prix(lait, monoprix, "1.450");
        // Pâtes : Aziza 1.100, Carrefour 1.150, Monoprix 1.250
        prix(pates, aziza, "1.100");
        prix(pates, carrefour, "1.150");
        prix(pates, monoprix, "1.250");
        // Beurre : PAS chez Aziza ; Carrefour 5.900, Monoprix 6.200
        prix(beurre, carrefour, "5.900");
        prix(beurre, monoprix, "6.200");

        liste = List.of(new Article(10L, "Lait 1L", 6), new Article(11L, "Spaghetti 500g", 2),
                new Article(12L, "Beurre 200g", 1));
    }

    @Test
    @DisplayName("Total par magasin = somme des prix × quantités")
    void calculeLesTotaux() {
        Resultat r = ComparateurPrix.comparer(liste, List.of(aziza, carrefour, monoprix), prix, null);

        ResultatMagasin c = trouver(r, 2L);
        // 6 × 1.350 + 2 × 1.150 + 1 × 5.900 = 8.100 + 2.300 + 5.900 = 16.300
        assertThat(c.total()).isEqualTo(16.3);
        assertThat(c.complet()).isTrue();
        assertThat(c.details()).hasSize(3);
        assertThat(c.details().get(0).sousTotal()).isEqualTo(8.1);
        assertThat(c.details().get(0).prixUnitaire()).isEqualTo(1.35);
    }

    @Test
    @DisplayName("PRX-13 : un produit sans prix rend le magasin incomplet et il est listé comme manquant")
    void signaleLesProduitsManquants() {
        Resultat r = ComparateurPrix.comparer(liste, List.of(aziza, carrefour, monoprix), prix, null);

        ResultatMagasin a = trouver(r, 1L);
        assertThat(a.complet()).isFalse();
        assertThat(a.produitsManquants()).containsExactly("Beurre 200g");
        // 6 × 1.300 + 2 × 1.100 = 10.000 (moins cher, mais incomplet)
        assertThat(a.total()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Classement : complets d'abord, puis total croissant ; recommandé = complet le moins cher")
    void classeEtRecommande() {
        Resultat r = ComparateurPrix.comparer(liste, List.of(aziza, monoprix, carrefour), prix, null);

        assertThat(r.magasins()).extracting(ResultatMagasin::magasinId).containsExactly(2L, 3L, 1L);
        assertThat(r.magasinRecommandeId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("PRX-12 : économie = complet le plus cher − complet le moins cher")
    void calculeLEconomie() {
        Resultat r = ComparateurPrix.comparer(liste, List.of(aziza, carrefour, monoprix), prix, null);

        // Monoprix : 6 × 1.450 + 2 × 1.250 + 6.200 = 8.700 + 2.500 + 6.200 = 17.400
        assertThat(trouver(r, 3L).total()).isEqualTo(17.4);
        assertThat(r.economie()).isEqualTo(1.1); // 17.400 − 16.300
    }

    @Test
    @DisplayName("Aucun magasin complet : pas de recommandation, économie 0, pas d'alerte budget")
    void aucunMagasinComplet() {
        Map<Long, Map<Long, Prix>> sansBeurre = new HashMap<>(prix);
        sansBeurre.remove(12L);
        Resultat r = ComparateurPrix.comparer(liste, List.of(aziza, carrefour), sansBeurre, new BigDecimal("1"));

        assertThat(r.magasinRecommandeId()).isNull();
        assertThat(r.economie()).isZero();
        assertThat(r.depasseBudget()).isFalse();
        assertThat(r.magasins()).allMatch(m -> !m.complet());
        // même incomplets, ils restent triés par total croissant
        assertThat(r.magasins()).extracting(ResultatMagasin::magasinId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("LNK-01 : dépassement si le total du magasin recommandé > reste du budget Alimentation")
    void detecteLeDepassementDeBudget() {
        Resultat juste = ComparateurPrix.comparer(liste, List.of(carrefour, monoprix), prix, new BigDecimal("16.300"));
        assertThat(juste.depasseBudget()).isFalse();
        assertThat(juste.budgetRestant()).isEqualTo(16.3);

        Resultat trop = ComparateurPrix.comparer(liste, List.of(carrefour, monoprix), prix, new BigDecimal("16.000"));
        assertThat(trop.depasseBudget()).isTrue();

        Resultat sansBudget = ComparateurPrix.comparer(liste, List.of(carrefour, monoprix), prix, null);
        assertThat(sansBudget.depasseBudget()).isFalse();
        assertThat(sansBudget.budgetRestant()).isNull();
    }

    @Test
    @DisplayName("Liste vide : aucun magasin, aucune recommandation")
    void listeVide() {
        Resultat r = ComparateurPrix.comparer(List.of(), List.of(aziza, carrefour), prix, null);

        assertThat(r.magasins()).isEmpty();
        assertThat(r.magasinRecommandeId()).isNull();
        assertThat(r.economie()).isZero();
    }

    @Test
    @DisplayName("LNK-03 : économie d'un achat = complet le plus cher − total payé (0 si magasin incomplet)")
    void economieDeLAchat() {
        Resultat r = ComparateurPrix.comparer(liste, List.of(aziza, carrefour, monoprix), prix, null);

        assertThat(ComparateurPrix.economieAchat(r, trouver(r, 2L))).isEqualByComparingTo("1.100");
        assertThat(ComparateurPrix.economieAchat(r, trouver(r, 3L))).isEqualByComparingTo("0");
        assertThat(ComparateurPrix.economieAchat(r, trouver(r, 1L))).isEqualByComparingTo("0");
    }

    private void prix(Produit produit, Magasin magasin, String montant) {
        Prix p = new Prix(idPrix++, produit, magasin, new BigDecimal(montant), LocalDate.now(), StatutPrix.VALIDE);
        prix.computeIfAbsent(produit.getId(), id -> new HashMap<>()).put(magasin.getId(), p);
    }

    private static ResultatMagasin trouver(Resultat r, Long magasinId) {
        return r.magasins().stream().filter(m -> m.magasinId().equals(magasinId)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("un magasin qui ne vend aucun article de la liste n'apparaît pas (ex. Spacenet pour des vêtements)")
    void magasinSansAucunArticleIgnore() {
        Magasin spacenet = new Magasin(9L, "Spacenet (en ligne)", "Spacenet");
        var r = ComparateurPrix.comparer(liste, List.of(aziza, carrefour, monoprix, spacenet), prix, null);
        assertThat(r.magasins()).extracting(ResultatMagasin::magasinNom).doesNotContain("Spacenet (en ligne)");
        assertThat(r.magasins()).hasSize(3);
    }
}
