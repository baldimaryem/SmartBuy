package tn.smartbuy.prix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** « Prix actuel » = dernier relevé VALIDE par (produit, magasin). */
class PrixActuelsTest {

    private final Produit lait = new Produit(1L, "Lait", "L");
    private final Produit cafe = new Produit(2L, "Café", "pièce");
    private final Magasin aziza = new Magasin(1L, "Aziza Ariana", "Aziza");
    private final Magasin monoprix = new Magasin(2L, "Monoprix Lafayette", "Monoprix");

    @Test
    @DisplayName("Le relevé le plus récent remplace l'ancien ; les prix non validés sont ignorés")
    void dernierPrixValide() {
        List<Prix> releves = List.of(
                prix(1L, lait, aziza, "1.400", "2026-09-01", StatutPrix.VALIDE),
                prix(2L, lait, aziza, "1.300", "2026-11-02", StatutPrix.VALIDE),
                prix(3L, lait, aziza, "0.900", "2026-11-10", StatutPrix.EN_ATTENTE),
                prix(4L, lait, monoprix, "1.450", "2026-11-01", StatutPrix.VALIDE));

        Map<Long, Map<Long, Prix>> actuels = PrixActuels.parProduitEtMagasin(releves);

        assertThat(actuels.get(1L).get(1L).getMontant()).isEqualByComparingTo("1.300");
        assertThat(PrixActuels.meilleur(actuels.get(1L))).map(Prix::getId).contains(2L);
        assertThat(PrixActuels.pourUnProduitTries(releves)).extracting(Prix::getId).containsExactly(2L, 4L);
    }

    @Test
    @DisplayName("Magasin le plus souvent le moins cher")
    void magasinLePlusSouventMoinsCher() {
        List<Prix> releves = List.of(
                prix(1L, lait, aziza, "1.300", "2026-11-02", StatutPrix.VALIDE),
                prix(2L, lait, monoprix, "1.450", "2026-11-02", StatutPrix.VALIDE),
                prix(3L, cafe, aziza, "7.500", "2026-11-02", StatutPrix.VALIDE),
                prix(4L, cafe, monoprix, "7.900", "2026-11-02", StatutPrix.VALIDE));

        var resultat = PrixActuels.magasinLePlusSouventMoinsCher(PrixActuels.parProduitEtMagasin(releves));

        assertThat(resultat).hasValueSatisfying(m -> {
            assertThat(m.magasinNom()).isEqualTo("Aziza Ariana");
            assertThat(m.nbProduits()).isEqualTo(2);
            assertThat(m.nbProduitsCompares()).isEqualTo(2);
        });
    }

    private static Prix prix(Long id, Produit p, Magasin m, String montant, String date, StatutPrix statut) {
        return new Prix(id, p, m, new BigDecimal(montant), LocalDate.parse(date), statut);
    }
}
