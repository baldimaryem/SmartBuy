package tn.smartbuy.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.smartbuy.commun.GestionnaireErreurs;
import tn.smartbuy.config.SecurityConfig;
import tn.smartbuy.prix.CatalogueController;
import tn.smartbuy.prix.CatalogueService;
import tn.smartbuy.prix.PrixDtos.MagasinDto;
import tn.smartbuy.prix.PrixService;
import tn.smartbuy.securite.JwtService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test « tranche web » (@WebMvcTest) : seuls les contrôleurs, la sécurité et la gestion d'erreurs
 * sont chargés ; les services sont simulés. Aucune base Oracle n'est nécessaire.
 * Vérifie les codes 401 / 403 / 400 et le format d'erreur { status, message } du contrat.
 */
@WebMvcTest(controllers = {CatalogueController.class, CatalogueAdminController.class})
@Import({SecurityConfig.class, JwtService.class, GestionnaireErreurs.class})
@TestPropertySource(properties = "smartbuy.jwt.secret=cle-de-test-suffisamment-longue-pour-hs256-0123456789")
class SecuriteWebTest {

    private static final String MAGASIN_JSON =
            "{\"nom\":\"Aziza Menzah\",\"enseigne\":\"Aziza\",\"adresse\":\"Rue X\",\"ville\":\"Ariana\"}";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CatalogueService catalogue;
    @MockitoBean
    private PrixService prixService;

    @Test
    @DisplayName("Sans jeton : 401 au format { status, message }")
    void sansJeton() throws Exception {
        mvc.perform(get("/api/magasins"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("Utilisateur connecté : lecture des magasins, champs du contrat")
    void lectureMagasins() throws Exception {
        when(catalogue.listerMagasins()).thenReturn(List.of(
                new MagasinDto(1L, "Carrefour La Marsa", "Carrefour", "Zone commerciale", "La Marsa")));

        mvc.perform(get("/api/magasins").header("Authorization", "Bearer " + jeton("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nom").value("Carrefour La Marsa"))
                .andExpect(jsonPath("$[0].enseigne").value("Carrefour"))
                .andExpect(jsonPath("$[0].ville").value("La Marsa"));
    }

    @Test
    @DisplayName("Route 🔒 ADMIN appelée par un USER : 403")
    void utilisateurInterdit() throws Exception {
        mvc.perform(post("/api/magasins").header("Authorization", "Bearer " + jeton("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(MAGASIN_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Accès réservé aux administrateurs"));
    }

    @Test
    @DisplayName("Route 🔒 ADMIN appelée par un ADMIN : 201")
    void adminAutorise() throws Exception {
        when(catalogue.creerMagasin(any())).thenReturn(new MagasinDto(6L, "Aziza Menzah", "Aziza", "Rue X", "Ariana"));

        mvc.perform(post("/api/magasins").header("Authorization", "Bearer " + jeton("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(MAGASIN_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(6));
    }

    @Test
    @DisplayName("Corps invalide : 400 avec un message en français")
    void validation() throws Exception {
        mvc.perform(post("/api/magasins").header("Authorization", "Bearer " + jeton("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enseigne\":\"Aziza\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Le nom du magasin est obligatoire"));
    }

    private String jeton(String role) {
        return jwtService.genererJeton(1L, role.toLowerCase() + "@smartbuy.tn", role);
    }
}
