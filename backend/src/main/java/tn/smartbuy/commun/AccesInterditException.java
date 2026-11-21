package tn.smartbuy.commun;

/** L'utilisateur connecté n'a pas le droit d'agir sur cette ressource : HTTP 403. */
public class AccesInterditException extends RuntimeException {
    public AccesInterditException(String message) {
        super(message);
    }
}
