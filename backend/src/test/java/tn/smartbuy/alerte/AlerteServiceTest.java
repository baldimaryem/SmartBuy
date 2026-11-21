package tn.smartbuy.alerte;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.smartbuy.auth.Utilisateur;
import tn.smartbuy.auth.UtilisateurRepository;
import tn.smartbuy.budget.Budget;
import tn.smartbuy.budget.BudgetRepository;
import tn.smartbuy.budget.Categorie;
import tn.smartbuy.budget.OperationRepository;
import tn.smartbuy.budget.TypeOperation;
import tn.smartbuy.email.EmailService;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** BUD-11 avec Mockito : les repositories sont simulés, aucune base de données n'est nécessaire. */
@ExtendWith(MockitoExtension.class)
class AlerteServiceTest {

    private static final Long UTILISATEUR = 1L;
    private static final Long ALIMENTATION = 1L;
    private static final YearMonth NOVEMBRE = YearMonth.of(2026, 11);

    @Mock
    private AlerteRepository alertes;
    @Mock
    private BudgetRepository budgets;
    @Mock
    private OperationRepository operations;
    @Mock
    private UtilisateurRepository utilisateurs;
    @Mock
    private EmailService emailService;

    private AlerteService service;
    private Budget budgetAlimentation;

    @BeforeEach
    void preparer() {
        service = new AlerteService(alertes, budgets, operations, utilisateurs, emailService);
        budgetAlimentation = new Budget();
        budgetAlimentation.setId(30L);
        budgetAlimentation.setUtilisateurId(UTILISATEUR);
        budgetAlimentation.setMois(NOVEMBRE);
        budgetAlimentation.setCategorie(new Categorie(ALIMENTATION, "Alimentation", TypeOperation.DEPENSE, "x", null));
        budgetAlimentation.setMontantPlafond(new BigDecimal("400"));

        when(budgets.findByUtilisateurIdAndMoisAndCategorieId(UTILISATEUR, NOVEMBRE, ALIMENTATION))
                .thenReturn(Optional.of(budgetAlimentation));
        when(budgets.findByUtilisateurIdAndMoisAndCategorieIsNull(UTILISATEUR, NOVEMBRE)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("320 / 400 = 80 % : une alerte BUDGET_80 est créée et un e-mail part")
    void creeAlerte80EtEnvoieEmail() {
        depenseAlimentation("320");
        Utilisateur u = new Utilisateur();
        u.setNom("Sarra");
        u.setEmail("demo@smartbuy.tn");
        when(utilisateurs.findById(UTILISATEUR)).thenReturn(Optional.of(u));

        service.verifierBudgets(UTILISATEUR, NOVEMBRE, ALIMENTATION);

        ArgumentCaptor<Alerte> capture = ArgumentCaptor.forClass(Alerte.class);
        verify(alertes).save(capture.capture());
        assertThat(capture.getValue().getType()).isEqualTo(TypeAlerte.BUDGET_80);
        assertThat(capture.getValue().getMessage()).isEqualTo("Alimentation : 80 % du budget atteint");
        assertThat(capture.getValue().getBudgetId()).isEqualTo(30L);
        verify(emailService).envoyerEnArrierePlan(eq("demo@smartbuy.tn"), anyString(), anyString());
    }

    @Test
    @DisplayName("Alerte 80 % déjà envoyée et toujours sous 100 % : rien de nouveau (pas de doublon)")
    void pasDeDoublon() {
        depenseAlimentation("350");
        when(alertes.existsByBudgetIdAndType(30L, TypeAlerte.BUDGET_80)).thenReturn(true);

        service.verifierBudgets(UTILISATEUR, NOVEMBRE, ALIMENTATION);

        verify(alertes, never()).save(any());
        verify(emailService, never()).envoyerEnArrierePlan(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Passage de 100 % : alerte BUDGET_100")
    void alerte100() {
        depenseAlimentation("410");
        when(alertes.existsByBudgetIdAndType(30L, TypeAlerte.BUDGET_80)).thenReturn(true);

        service.verifierBudgets(UTILISATEUR, NOVEMBRE, ALIMENTATION);

        ArgumentCaptor<Alerte> capture = ArgumentCaptor.forClass(Alerte.class);
        verify(alertes).save(capture.capture());
        assertThat(capture.getValue().getType()).isEqualTo(TypeAlerte.BUDGET_100);
    }

    @Test
    @DisplayName("Sous 80 % : aucune alerte")
    void sousLeSeuil() {
        depenseAlimentation("100");

        service.verifierBudgets(UTILISATEUR, NOVEMBRE, ALIMENTATION);

        verify(alertes, never()).save(any());
    }

    private void depenseAlimentation(String montant) {
        when(operations.totalDepensesCategorie(eq(UTILISATEUR), eq(ALIMENTATION), any(), any()))
                .thenReturn(new BigDecimal(montant));
    }
}
