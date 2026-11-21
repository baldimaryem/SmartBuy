package tn.smartbuy.prix;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calcul du « prix actuel » : pour chaque couple (produit, magasin), le relevé VALIDE le plus récent.
 * <p>
 * Classe sans Spring ni base de données : elle se teste avec de simples objets Java.
 */
public final class PrixActuels {

    /** Ordre « du plus ancien au plus récent » : date de relevé, puis id (dernier saisi). */
    private static final Comparator<Prix> PLUS_RECENT =
            Comparator.comparing(Prix::getDateReleve).thenComparing(Prix::getId);

    private PrixActuels() {
    }

    /**
     * @return pour chaque produit, une table magasinId -> dernier prix VALIDE
     */
    public static Map<Long, Map<Long, Prix>> parProduitEtMagasin(Collection<Prix> releves) {
        Map<Long, Map<Long, Prix>> resultat = new HashMap<>();
        for (Prix p : releves) {
            if (p.getStatut() != StatutPrix.VALIDE) {
                continue;
            }
            Map<Long, Prix> parMagasin = resultat.computeIfAbsent(p.getProduit().getId(), id -> new HashMap<>());
            parMagasin.merge(p.getMagasin().getId(), p,
                    (ancien, nouveau) -> PLUS_RECENT.compare(nouveau, ancien) > 0 ? nouveau : ancien);
        }
        return resultat;
    }

    /** Derniers prix d'un seul produit, triés du moins cher au plus cher (PRX-04). */
    public static List<Prix> pourUnProduitTries(Collection<Prix> relevesDuProduit) {
        return parProduitEtMagasin(relevesDuProduit).values().stream()
                .flatMap(parMagasin -> parMagasin.values().stream())
                .sorted(Comparator.comparing(Prix::getMontant).thenComparing(p -> p.getMagasin().getNom()))
                .toList();
    }

    /** Le prix le plus bas parmi les prix actuels d'un produit, s'il y en a un. */
    public static Optional<Prix> meilleur(Map<Long, Prix> prixParMagasin) {
        if (prixParMagasin == null) {
            return Optional.empty();
        }
        return prixParMagasin.values().stream()
                .min(Comparator.comparing(Prix::getMontant).thenComparing(p -> p.getMagasin().getNom()));
    }

    /** Magasin le moins cher sur le plus grand nombre de produits (utilisé par les conseils). */
    public record MagasinMoinsCher(String magasinNom, long nbProduits, int nbProduitsCompares) {
    }

    /**
     * Pour chaque produit vendu dans au moins 2 magasins, on cherche le magasin le moins cher,
     * puis on garde le magasin qui « gagne » le plus souvent.
     */
    public static Optional<MagasinMoinsCher> magasinLePlusSouventMoinsCher(Map<Long, Map<Long, Prix>> prixActuels) {
        Map<String, Long> victoires = new HashMap<>();
        int compares = 0;
        for (Map<Long, Prix> parMagasin : prixActuels.values()) {
            if (parMagasin.size() < 2) {
                continue;
            }
            compares++;
            meilleur(parMagasin).ifPresent(p -> victoires.merge(p.getMagasin().getNom(), 1L, Long::sum));
        }
        final int total = compares;
        return victoires.entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(e -> new MagasinMoinsCher(e.getKey(), e.getValue(), total));
    }
}
