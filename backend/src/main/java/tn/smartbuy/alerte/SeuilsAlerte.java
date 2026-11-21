package tn.smartbuy.alerte;

import java.util.Optional;

/**
 * Règle BUD-11, sans base de données : quelle alerte faut-il créer pour un budget ?
 * <ul>
 *   <li>une seule alerte par seuil et par budget (le budget est déjà propre à un mois) ;</li>
 *   <li>si on passe directement de 50 % à 120 %, on n'envoie que l'alerte 100 % (la plus utile) ;</li>
 *   <li>une fois l'alerte 100 % envoyée, l'alerte 80 % n'a plus de sens.</li>
 * </ul>
 */
public final class SeuilsAlerte {

    public static final double SEUIL_ATTENTION = 80.0;
    public static final double SEUIL_DEPASSEMENT = 100.0;

    private SeuilsAlerte() {
    }

    /**
     * @param pourcentage   dépensé / plafond × 100
     * @param deja80Envoyee une alerte BUDGET_80 existe déjà pour ce budget
     * @param deja100Envoyee une alerte BUDGET_100 existe déjà pour ce budget
     * @return le type d'alerte à créer, ou vide s'il n'y a rien à faire
     */
    public static Optional<TypeAlerte> aCreer(double pourcentage, boolean deja80Envoyee, boolean deja100Envoyee) {
        if (pourcentage >= SEUIL_DEPASSEMENT) {
            return deja100Envoyee ? Optional.empty() : Optional.of(TypeAlerte.BUDGET_100);
        }
        if (pourcentage >= SEUIL_ATTENTION && !deja80Envoyee && !deja100Envoyee) {
            return Optional.of(TypeAlerte.BUDGET_80);
        }
        return Optional.empty();
    }

    /** Texte de l'alerte, ex. « Alimentation : 80 % du budget atteint ». */
    public static String message(String libelleBudget, TypeAlerte type) {
        return switch (type) {
            case BUDGET_80 -> libelleBudget + " : 80 % du budget atteint";
            case BUDGET_100 -> libelleBudget + " : 100 % du budget atteint, budget dépassé";
            case BAISSE_PRIX -> libelleBudget + " : baisse de prix";
        };
    }
}
