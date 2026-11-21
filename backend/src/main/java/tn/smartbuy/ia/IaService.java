package tn.smartbuy.ia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.smartbuy.ia.IaDtos.AssistantReponse;
import tn.smartbuy.ia.IaDtos.Conseil;
import tn.smartbuy.ia.IaDtos.ConseilsReponse;

import java.time.YearMonth;
import java.util.List;

/**
 * Choisit le conseiller : Claude si une clé API est configurée, sinon les règles.
 * Si Claude échoue (réseau, quota, réponse invalide...), on répond quand même avec les règles :
 * l'application fonctionne toujours (source = "REGLES").
 */
@Service
public class IaService {

    private static final Logger log = LoggerFactory.getLogger(IaService.class);

    private final ResumeMoisService resumeService;
    private final ConseillerRegles regles;
    private final ConseillerClaude claude;

    public IaService(ResumeMoisService resumeService, ConseillerRegles regles, ConseillerClaude claude) {
        this.resumeService = resumeService;
        this.regles = regles;
        this.claude = claude;
    }

    /** BUD-16 : conseils d'économie du mois. */
    public ConseilsReponse conseils(Long utilisateurId, YearMonth mois) {
        ResumeMois resume = resumeService.construire(utilisateurId, mois);
        if (claude.estDisponible()) {
            try {
                List<Conseil> conseils = claude.conseils(resume);
                if (!conseils.isEmpty()) {
                    return new ConseilsReponse("IA", conseils);
                }
            } catch (Exception e) {
                log.warn("IA indisponible, utilisation des règles : {}", e.getMessage());
            }
        }
        return new ConseilsReponse("REGLES", regles.conseils(resume));
    }

    /** Assistant budgétaire en langage naturel (sur le mois courant). */
    public AssistantReponse repondre(Long utilisateurId, String question) {
        ResumeMois resume = resumeService.construire(utilisateurId, YearMonth.now());
        if (claude.estDisponible()) {
            try {
                return new AssistantReponse("IA", claude.repondre(question, resume));
            } catch (Exception e) {
                log.warn("IA indisponible, utilisation des règles : {}", e.getMessage());
            }
        }
        return new AssistantReponse("REGLES", regles.repondre(question, resume));
    }
}
