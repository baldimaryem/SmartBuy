package tn.smartbuy.ia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.smartbuy.ia.IaDtos.Conseil;
import tn.smartbuy.ia.IaDtos.ConseilsReponse;
import tn.smartbuy.ia.IaDtos.Niveau;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Choix du conseiller : IA si configurée, sinon (ou en cas d'erreur) les règles. */
@ExtendWith(MockitoExtension.class)
class IaServiceTest {

    @Mock
    private ResumeMoisService resumeService;
    @Mock
    private ConseillerClaude claude;

    private IaService service;
    private final ResumeMois resume = new ResumeMois("2026-11", "TND", true, 1800, 500, 1800.0, null, 900, 0,
            List.of(), null, 0, 0);

    @BeforeEach
    void preparer() {
        service = new IaService(resumeService, new ConseillerRegles(), claude);
        when(resumeService.construire(anyLong(), any())).thenReturn(resume);
    }

    @Test
    @DisplayName("Sans clé API : source REGLES, Claude n'est jamais appelé")
    void sansCle() {
        when(claude.estDisponible()).thenReturn(false);

        ConseilsReponse r = service.conseils(1L, YearMonth.of(2026, 11));

        assertThat(r.source()).isEqualTo("REGLES");
        assertThat(r.conseils()).isNotEmpty();
        verify(claude, never()).conseils(any());
    }

    @Test
    @DisplayName("Avec clé API : source IA")
    void avecCle() {
        when(claude.estDisponible()).thenReturn(true);
        when(claude.conseils(resume)).thenReturn(List.of(new Conseil("Titre", "Message", Niveau.INFO)));

        assertThat(service.conseils(1L, YearMonth.of(2026, 11)).source()).isEqualTo("IA");
    }

    @Test
    @DisplayName("Erreur de l'IA : repli sur les règles, la requête ne plante pas")
    void erreurIa() {
        when(claude.estDisponible()).thenReturn(true);
        when(claude.conseils(resume)).thenThrow(new IllegalStateException("réseau indisponible"));
        when(claude.repondre(any(), any())).thenThrow(new IllegalStateException("quota dépassé"));

        assertThat(service.conseils(1L, YearMonth.of(2026, 11)).source()).isEqualTo("REGLES");
        assertThat(service.repondre(1L, "Combien me reste-t-il ?").source()).isEqualTo("REGLES");
    }
}
