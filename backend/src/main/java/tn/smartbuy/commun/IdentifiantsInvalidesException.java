package tn.smartbuy.commun;

/** E-mail ou mot de passe incorrect à la connexion : HTTP 401. */
public class IdentifiantsInvalidesException extends RuntimeException {
    public IdentifiantsInvalidesException(String message) {
        super(message);
    }
}
