package tn.smartbuy.ia;

import tn.smartbuy.ia.IaDtos.Conseil;

import java.util.List;

/**
 * « Conseiller budgétaire » (BUD-16) : une interface, deux implémentations.
 * <ul>
 *   <li>{@link ConseillerRegles} : règles simples codées en Java, toujours disponibles ;</li>
 *   <li>{@link ConseillerClaude} : modèle d'IA Claude, utilisé seulement si une clé API est configurée.</li>
 * </ul>
 * Pourquoi une interface ? Le reste de l'application ne dépend pas de la technique utilisée :
 * on peut changer de moteur (ou en ajouter un) sans toucher aux contrôleurs.
 */
public interface ConseillerBudget {

    /** Conseils d'économie à partir du résumé chiffré du mois. */
    List<Conseil> conseils(ResumeMois resume);

    /** Réponse en langage naturel à une question de l'utilisateur sur son budget. */
    String repondre(String question, ResumeMois resume);
}
