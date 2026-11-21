package tn.smartbuy.prix;

/**
 * Univers de courses : chaque rayon (et chaque liste) appartient à l'un d'eux.
 * La catégorie de budget associée sert lors de l'achat d'une liste (LNK-02) et pour comparer
 * le total au budget restant (LNK-01).
 */
public enum Univers {
    ALIMENTATION("Alimentation"),
    VETEMENTS("Vêtements"),
    HIGH_TECH("High-tech");

    private final String categorieBudget;

    Univers(String categorieBudget) {
        this.categorieBudget = categorieBudget;
    }

    public String categorieBudget() {
        return categorieBudget;
    }
}
