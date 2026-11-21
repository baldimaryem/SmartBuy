package tn.smartbuy.commun;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Petites fonctions utilitaires sur les montants.
 * <p>
 * Les calculs se font en {@link BigDecimal} (exact, comme NUMBER(12,3) dans Oracle) ;
 * on convertit en {@code Double} seulement pour le JSON, afin d'obtenir « 45.5 » et non « 45.500 ».
 */
public final class Montants {

    /** Le dinar tunisien a 3 décimales (millimes). */
    public static final int DECIMALES = 3;

    private Montants() {
    }

    public static BigDecimal arrondir(BigDecimal valeur) {
        return valeur == null ? null : valeur.setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    public static Double versDouble(BigDecimal valeur) {
        return valeur == null ? null : arrondir(valeur).doubleValue();
    }

    public static BigDecimal depuisDouble(Double valeur) {
        return valeur == null ? null : arrondir(BigDecimal.valueOf(valeur));
    }

    public static BigDecimal nonNul(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }

    /** Pourcentage arrondi à 1 décimale (ex. 80.0). Renvoie 0 si le total est nul. */
    public static double pourcentage(BigDecimal partie, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return 0.0;
        }
        return nonNul(partie).multiply(BigDecimal.valueOf(100))
                .divide(total, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
