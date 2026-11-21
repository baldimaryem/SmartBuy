package tn.smartbuy.liste;

import tn.smartbuy.commun.Montants;
import tn.smartbuy.prix.Magasin;
import tn.smartbuy.prix.Prix;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Algorithme de comparaison d'une liste de courses entre magasins (PRX-11, PRX-12, PRX-13, LNK-01).
 * <p>
 * Classe « pure » : pas de Spring, pas de base de données. On lui donne la liste, les magasins
 * et les prix actuels ; elle renvoie le classement. C'est ce qui la rend facile à tester (ComparateurPrixTest).
 *
 * <ol>
 *   <li>Pour chaque magasin : total = Σ (prix actuel × quantité) des produits disponibles ;</li>
 *   <li>un produit sans prix dans ce magasin est « manquant » et le magasin est « incomplet » ;</li>
 *   <li>un magasin qui n'a aucun produit de la liste est ignoré (il n'apparaît pas dans le classement) ;</li>
 *   <li>classement : magasins complets d'abord, puis total croissant ;</li>
 *   <li>magasin recommandé = le moins cher des magasins complets ;</li>
 *   <li>économie = total du complet le plus cher − total du complet le moins cher ;</li>
 *   <li>dépassement = total du magasin recommandé &gt; reste du budget « Alimentation ».</li>
 * </ol>
 */
public final class ComparateurPrix {

    private ComparateurPrix() {
    }

    /** Un produit de la liste et sa quantité. */
    public record Article(Long produitId, String produitNom, int quantite) {
    }

    public record Detail(Long produitId, String produitNom, int quantite, Double prixUnitaire, Double sousTotal) {
    }

    public record ResultatMagasin(Long magasinId, String magasinNom, String enseigne, Double total, boolean complet,
                                  List<String> produitsManquants, List<Detail> details) {
    }

    public record Resultat(List<ResultatMagasin> magasins, Long magasinRecommandeId, Double economie,
                           Double budgetRestant, boolean depasseBudget) {
    }

    private static final Comparator<ResultatMagasin> CLASSEMENT = Comparator
            .comparing((ResultatMagasin r) -> !r.complet())   // complets (false) d'abord
            .thenComparing(ResultatMagasin::total)
            .thenComparing(ResultatMagasin::magasinNom);

    /**
     * @param articles      lignes de la liste
     * @param magasins      magasins à comparer
     * @param prixActuels   produitId -> (magasinId -> dernier prix VALIDE)
     * @param budgetRestant reste du budget « Alimentation » du mois, ou null s'il n'y a pas de budget
     */
    public static Resultat comparer(List<Article> articles, List<Magasin> magasins,
                                    Map<Long, Map<Long, Prix>> prixActuels, BigDecimal budgetRestant) {
        Double reste = Montants.versDouble(budgetRestant);
        if (articles.isEmpty()) {
            return new Resultat(List.of(), null, 0.0, reste, false);
        }

        List<ResultatMagasin> resultats = new ArrayList<>();
        for (Magasin magasin : magasins) {
            ResultatMagasin r = calculerMagasin(magasin, articles, prixActuels);
            // Un magasin qui ne vend AUCUN article de la liste (ex. Spacenet pour une liste de vêtements)
            // n'apporte rien à la comparaison : on ne l'affiche pas.
            if (!r.details().isEmpty()) {
                resultats.add(r);
            }
        }
        resultats.sort(CLASSEMENT);

        List<ResultatMagasin> complets = resultats.stream().filter(ResultatMagasin::complet).toList();
        ResultatMagasin recommande = complets.isEmpty() ? null : complets.get(0);
        ResultatMagasin plusCher = complets.isEmpty() ? null : complets.get(complets.size() - 1);

        BigDecimal economie = recommande == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(plusCher.total()).subtract(BigDecimal.valueOf(recommande.total()));

        boolean depasse = recommande != null && budgetRestant != null
                && BigDecimal.valueOf(recommande.total()).compareTo(budgetRestant) > 0;

        return new Resultat(resultats, recommande == null ? null : recommande.magasinId(),
                Montants.versDouble(economie), reste, depasse);
    }

    /**
     * LNK-03 : économie d'un achat = total du magasin complet le plus cher − total payé.
     * Vaut 0 si le magasin choisi est incomplet (comparaison non significative) ou s'il n'y a pas de magasin complet.
     */
    public static BigDecimal economieAchat(Resultat resultat, ResultatMagasin choisi) {
        if (!choisi.complet()) {
            return BigDecimal.ZERO;
        }
        BigDecimal plusCher = resultat.magasins().stream()
                .filter(ResultatMagasin::complet)
                .map(r -> BigDecimal.valueOf(r.total()))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal economie = plusCher.subtract(BigDecimal.valueOf(choisi.total()));
        return Montants.arrondir(economie.max(BigDecimal.ZERO));
    }

    private static ResultatMagasin calculerMagasin(Magasin magasin, List<Article> articles,
                                                   Map<Long, Map<Long, Prix>> prixActuels) {
        BigDecimal total = BigDecimal.ZERO;
        List<String> manquants = new ArrayList<>();
        List<Detail> details = new ArrayList<>();
        for (Article article : articles) {
            Prix prix = prixActuels.getOrDefault(article.produitId(), Map.of()).get(magasin.getId());
            if (prix == null) {
                manquants.add(article.produitNom());
                continue;
            }
            BigDecimal sousTotal = prix.getMontant().multiply(BigDecimal.valueOf(article.quantite()));
            total = total.add(sousTotal);
            details.add(new Detail(article.produitId(), article.produitNom(), article.quantite(),
                    Montants.versDouble(prix.getMontant()), Montants.versDouble(sousTotal)));
        }
        return new ResultatMagasin(magasin.getId(), magasin.getNom(), magasin.getEnseigne(),
                Montants.versDouble(total), manquants.isEmpty(), manquants, details);
    }
}
