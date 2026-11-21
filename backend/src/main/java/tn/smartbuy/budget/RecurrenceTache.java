package tn.smartbuy.budget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

/**
 * BUD-07 : tâche planifiée qui recopie les opérations récurrentes (loyer, abonnement...).
 * <p>
 * Expression cron Spring : seconde minute heure jour-du-mois mois jour-de-semaine.
 * "0 5 0 1 * *" = le 1er de chaque mois à 00h05.
 */
@Component
public class RecurrenceTache {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceTache.class);

    private final OperationService operationService;

    public RecurrenceTache(OperationService operationService) {
        this.operationService = operationService;
    }

    @Scheduled(cron = "0 5 0 1 * *", zone = "Africa/Tunis")
    public void copierOperationsRecurrentes() {
        YearMonth moisCourant = YearMonth.now();
        int nombre = operationService.copierRecurrentes(moisCourant);
        log.info("BUD-07 : {} opération(s) récurrente(s) recopiée(s) dans {}", nombre, moisCourant);
    }
}
