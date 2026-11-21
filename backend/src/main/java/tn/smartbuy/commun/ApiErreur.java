package tn.smartbuy.commun;

/**
 * Corps JSON de toutes les erreurs renvoyées par l'API : {@code { "status": 400, "message": "..." }}.
 * Le message est en français : l'application Flutter l'affiche tel quel.
 */
public record ApiErreur(int status, String message) {
}
