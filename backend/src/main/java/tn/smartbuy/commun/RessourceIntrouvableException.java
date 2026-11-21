package tn.smartbuy.commun;

/** Ressource absente de la base : renvoyée en HTTP 404. */
public class RessourceIntrouvableException extends RuntimeException {
    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
