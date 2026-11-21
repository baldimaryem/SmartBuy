package tn.smartbuy.rapport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.smartbuy.auth.Utilisateur;
import tn.smartbuy.auth.UtilisateurRepository;

import java.time.YearMonth;

/**
 * Envoi automatique du bilan du mois précédent, le 1er de chaque mois à 8h00,
 * à tous les utilisateurs qui ont eu au moins une opération ce mois-là.
 */
@Component
public class RapportTache {

    private static final Logger log = LoggerFactory.getLogger(RapportTache.class);

    private final UtilisateurRepository utilisateurs;
    private final RapportService rapportService;

    public RapportTache(UtilisateurRepository utilisateurs, RapportService rapportService) {
        this.utilisateurs = utilisateurs;
        this.rapportService = rapportService;
    }

    @Scheduled(cron = "0 0 8 1 * *", zone = "Africa/Tunis")
    public void envoyerBilansDuMoisPrecedent() {
        YearMonth moisPrecedent = YearMonth.now().minusMonths(1);
        int envoyes = 0;
        for (Utilisateur u : utilisateurs.findAll()) {
            try {
                if (rapportService.aDesOperations(u.getId(), moisPrecedent)
                        && rapportService.envoyerBilan(u.getId(), moisPrecedent).envoye()) {
                    envoyes++;
                }
            } catch (Exception e) {
                // un utilisateur en erreur ne doit pas empêcher l'envoi aux autres
                log.warn("Bilan de {} non envoyé à l'utilisateur {} : {}", moisPrecedent, u.getId(), e.getMessage());
            }
        }
        log.info("Bilan mensuel {} envoyé à {} utilisateur(s)", moisPrecedent, envoyes);
    }
}
