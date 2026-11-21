package tn.smartbuy.alerte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.auth.UtilisateurRepository;
import tn.smartbuy.budget.Budget;
import tn.smartbuy.budget.BudgetRepository;
import tn.smartbuy.budget.OperationRepository;
import tn.smartbuy.budget.TypeOperation;
import tn.smartbuy.commun.AccesInterditException;
import tn.smartbuy.commun.Montants;
import tn.smartbuy.commun.RessourceIntrouvableException;
import tn.smartbuy.email.EmailService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Alertes de budget (BUD-11) : création, consultation, lecture. */
@Service
public class AlerteService {

    private static final Logger log = LoggerFactory.getLogger(AlerteService.class);

    private final AlerteRepository alertes;
    private final BudgetRepository budgets;
    private final OperationRepository operations;
    private final UtilisateurRepository utilisateurs;
    private final EmailService emailService;

    public AlerteService(AlerteRepository alertes, BudgetRepository budgets, OperationRepository operations,
                         UtilisateurRepository utilisateurs, EmailService emailService) {
        this.alertes = alertes;
        this.budgets = budgets;
        this.operations = operations;
        this.utilisateurs = utilisateurs;
        this.emailService = emailService;
    }

    /** Objet « Alerte » du contrat. */
    public record AlerteDto(Long id, TypeAlerte type, String message, LocalDateTime dateAlerte, boolean lue) {

        static AlerteDto depuis(Alerte a) {
            return new AlerteDto(a.getId(), a.getType(), a.getMessage(), a.getDateAlerte(), a.isLue());
        }
    }

    /**
     * BUD-11 : appelée après l'ajout ou la modification d'une DEPENSE.
     * Compare le dépensé du mois au budget de la catégorie puis au budget global,
     * et crée une alerte (+ e-mail) au passage de 80 % ou 100 %.
     */
    @Transactional
    public void verifierBudgets(Long utilisateurId, YearMonth mois, Long categorieId) {
        LocalDate debut = mois.atDay(1);
        LocalDate fin = mois.atEndOfMonth();

        budgets.findByUtilisateurIdAndMoisAndCategorieId(utilisateurId, mois, categorieId).ifPresent(budget ->
                verifierUnBudget(budget, operations.totalDepensesCategorie(utilisateurId, categorieId, debut, fin)));

        budgets.findByUtilisateurIdAndMoisAndCategorieIsNull(utilisateurId, mois).ifPresent(budget ->
                verifierUnBudget(budget, operations.total(utilisateurId, TypeOperation.DEPENSE, debut, fin)));
    }

    private void verifierUnBudget(Budget budget, BigDecimal depense) {
        double pourcentage = Montants.pourcentage(depense, budget.getMontantPlafond());
        boolean deja80 = alertes.existsByBudgetIdAndType(budget.getId(), TypeAlerte.BUDGET_80);
        boolean deja100 = alertes.existsByBudgetIdAndType(budget.getId(), TypeAlerte.BUDGET_100);

        SeuilsAlerte.aCreer(pourcentage, deja80, deja100).ifPresent(type -> {
            Alerte alerte = new Alerte();
            alerte.setUtilisateurId(budget.getUtilisateurId());
            alerte.setType(type);
            alerte.setMessage(SeuilsAlerte.message(budget.libelle(), type));
            alerte.setDateAlerte(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            alerte.setBudgetId(budget.getId());
            alertes.save(alerte);
            log.info("Alerte {} créée pour l'utilisateur {} ({})", type, budget.getUtilisateurId(), alerte.getMessage());
            envoyerEmail(budget, alerte, depense);
        });
    }

    private void envoyerEmail(Budget budget, Alerte alerte, BigDecimal depense) {
        utilisateurs.findById(budget.getUtilisateurId()).ifPresent(u -> {
            String contenu = """
                    <p>Bonjour %s,</p>
                    <p><strong>%s</strong> pour le mois %s.</p>
                    <p>Dépensé : %s %s sur un plafond de %s %s.</p>
                    <p>Ouvrez SmartBuy pour comparer les prix et ajuster vos dépenses.</p>
                    """.formatted(EmailService.echapper(u.getNom()), EmailService.echapper(alerte.getMessage()),
                    budget.getMois(), Montants.arrondir(depense), u.getDevise(),
                    Montants.arrondir(budget.getMontantPlafond()), u.getDevise());
            // @Async : l'e-mail part en arrière-plan, la requête de l'utilisateur n'attend pas
            emailService.envoyerEnArrierePlan(u.getEmail(), "SmartBuy - " + alerte.getMessage(),
                    EmailService.gabarit("Alerte budget", contenu));
        });
    }

    /** Alertes de l'utilisateur, les plus récentes d'abord. */
    @Transactional(readOnly = true)
    public List<AlerteDto> lister(Long utilisateurId) {
        return alertes.findByUtilisateurIdOrderByDateAlerteDescIdDesc(utilisateurId).stream()
                .map(AlerteDto::depuis).toList();
    }

    @Transactional
    public AlerteDto marquerLue(Long utilisateurId, Long alerteId) {
        Alerte a = alertes.findById(alerteId)
                .orElseThrow(() -> new RessourceIntrouvableException("Alerte introuvable"));
        if (!a.getUtilisateurId().equals(utilisateurId)) {
            throw new AccesInterditException("Cette alerte ne vous appartient pas");
        }
        a.setLue(true);
        return AlerteDto.depuis(a);
    }

    @Transactional(readOnly = true)
    public long nombreNonLues(Long utilisateurId) {
        return alertes.countByUtilisateurIdAndLue(utilisateurId, false);
    }
}
