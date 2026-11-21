package tn.smartbuy.prix;

/** D'où vient la fiche produit. */
public enum SourceProduit {
    /** saisi par l'administrateur ou le jeu de données */
    CATALOGUE,
    /** trouvé par son code-barres dans la base ouverte Open Food Facts */
    OPEN_FOOD_FACTS,
    /** créé par la collecte automatique sur un site marchand */
    COLLECTE,
    /** ajouté par un utilisateur qui ne trouvait pas le produit */
    UTILISATEUR
}
