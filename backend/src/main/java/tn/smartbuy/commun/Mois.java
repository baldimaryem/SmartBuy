package tn.smartbuy.commun;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Lecture du paramètre « mois » (format AAAA-MM, ex. 2026-11) avec un message d'erreur en français. */
public final class Mois {

    private Mois() {
    }

    /** @return le mois lu, ou le mois courant si le texte est vide */
    public static YearMonth lireOuCourant(String texte) {
        return texte == null || texte.isBlank() ? YearMonth.now() : lire(texte);
    }

    public static YearMonth lire(String texte) {
        try {
            return YearMonth.parse(texte.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RequeteInvalideException("Mois invalide : utilisez le format AAAA-MM (ex. 2026-11)");
        }
    }
}
