package tn.smartbuy.ia;

import java.util.List;

/**
 * Résumé CHIFFRÉ et ANONYME du mois d'un utilisateur : c'est la seule chose envoyée à Claude.
 * Aucun nom, aucun e-mail, aucune note libre : seulement des montants et des noms de catégories.
 *
 * @param moisEnCours             vrai si le mois n'est pas terminé (la prévision a alors un sens)
 * @param previsionFinMois        dépenses estimées en fin de mois (BUD-15)
 * @param categories              dépenses par catégorie, avec la moyenne des 3 mois précédents
 * @param magasinLeMoinsCher      magasin le moins cher sur le plus de produits du catalogue (ou null)
 */
public record ResumeMois(
        String mois,
        String devise,
        boolean moisEnCours,
        double revenus,
        double depenses,
        Double revenuMensuelHabituel,
        Double budgetGlobal,
        double previsionFinMois,
        double economiesRealisees,
        List<DepenseCategorie> categories,
        String magasinLeMoinsCher,
        long nbProduitsMoinsChers,
        int nbProduitsCompares) {

    /**
     * @param moyenne3MoisPrecedents moyenne mensuelle des 3 mois précédents (0 si pas d'historique)
     * @param budget                 plafond du mois pour cette catégorie, ou null
     */
    public record DepenseCategorie(String categorie, double montant, double moyenne3MoisPrecedents, Double budget) {
    }
}
