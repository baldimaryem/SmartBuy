package tn.smartbuy.rapport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.auth.Utilisateur;
import tn.smartbuy.auth.UtilisateurRepository;
import tn.smartbuy.budget.BudgetDtos.DashboardDto;
import tn.smartbuy.budget.BudgetDtos.RepartitionDto;
import tn.smartbuy.budget.DashboardService;
import tn.smartbuy.commun.RessourceIntrouvableException;
import tn.smartbuy.email.EmailService;

import java.time.YearMonth;
import java.util.Locale;

/** Bilan mensuel envoyé par e-mail (bouton « Envoyer le bilan » + envoi automatique le 1er du mois). */
@Service
public class RapportService {

    private final UtilisateurRepository utilisateurs;
    private final DashboardService dashboardService;
    private final EmailService emailService;

    public RapportService(UtilisateurRepository utilisateurs, DashboardService dashboardService,
                          EmailService emailService) {
        this.utilisateurs = utilisateurs;
        this.dashboardService = dashboardService;
        this.emailService = emailService;
    }

    /** Réponse de POST /rapports/mensuel. */
    public record RapportReponse(boolean envoye, String destinataire) {
    }

    /** Construit le bilan du mois et l'envoie (synchrone : la réponse indique si l'envoi a réussi). */
    @Transactional(readOnly = true)
    public RapportReponse envoyerBilan(Long utilisateurId, YearMonth mois) {
        Utilisateur u = utilisateurs.findById(utilisateurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        DashboardDto d = dashboardService.tableauDeBord(utilisateurId, mois);
        boolean envoye = emailService.envoyer(u.getEmail(), "SmartBuy - Votre bilan de " + mois,
                EmailService.gabarit("Bilan du mois " + mois, contenu(u, d)));
        return new RapportReponse(envoye, u.getEmail());
    }

    /** Vrai si l'utilisateur a au moins une opération ce mois-là (inutile d'envoyer un bilan vide). */
    @Transactional(readOnly = true)
    public boolean aDesOperations(Long utilisateurId, YearMonth mois) {
        DashboardDto d = dashboardService.tableauDeBord(utilisateurId, mois);
        return d.totalDepenses() > 0 || d.totalRevenus() > 0;
    }

    private static String contenu(Utilisateur u, DashboardDto d) {
        String devise = u.getDevise();
        StringBuilder html = new StringBuilder();
        html.append("<p>Bonjour ").append(EmailService.echapper(u.getNom())).append(",</p>");
        html.append("<p>Voici le résumé de vos finances pour ").append(d.mois()).append(" :</p>");
        html.append("<table cellpadding=\"6\" style=\"border-collapse: collapse;\">");
        ligne(html, "Revenus", montant(d.totalRevenus(), devise));
        ligne(html, "Dépenses", montant(d.totalDepenses(), devise));
        ligne(html, "Solde", montant(d.solde(), devise));
        if (d.budgetGlobal() != null) {
            ligne(html, "Budget global", montant(d.budgetGlobal(), devise));
            ligne(html, "Reste à dépenser", montant(d.resteADepenser(), devise));
        }
        ligne(html, "Économies grâce à la comparaison", montant(d.economiesRealisees(), devise));
        html.append("</table>");

        if (!d.repartition().isEmpty()) {
            html.append("<p><strong>Vos principales dépenses :</strong></p><ul>");
            d.repartition().stream().limit(3).forEach((RepartitionDto r) -> html.append("<li>")
                    .append(EmailService.echapper(r.categorieNom())).append(" : ")
                    .append(montant(r.montant(), devise)).append("</li>"));
            html.append("</ul>");
        }
        if (d.budgetGlobal() != null && d.totalDepenses() > d.budgetGlobal()) {
            html.append("<p style=\"color:#c62828;\">Attention : le budget global a été dépassé ce mois-ci.</p>");
        }
        return html.toString();
    }

    private static void ligne(StringBuilder html, String libelle, String valeur) {
        html.append("<tr><td>").append(libelle).append("</td><td style=\"text-align:right;\"><strong>")
                .append(valeur).append("</strong></td></tr>");
    }

    private static String montant(Double valeur, String devise) {
        return String.format(Locale.FRANCE, "%,.3f %s", valeur == null ? 0.0 : valeur, devise);
    }
}
