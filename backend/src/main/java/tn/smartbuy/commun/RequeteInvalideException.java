package tn.smartbuy.commun;

/** Règle métier non respectée (ex. catégorie de revenu pour une dépense) : HTTP 400. */
public class RequeteInvalideException extends RuntimeException {
    public RequeteInvalideException(String message) {
        super(message);
    }
}
