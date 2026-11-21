package tn.smartbuy.commun;

/** Doublon ou donnée encore utilisée (ex. e-mail déjà inscrit) : HTTP 409. */
public class ConflitException extends RuntimeException {
    public ConflitException(String message) {
        super(message);
    }
}
