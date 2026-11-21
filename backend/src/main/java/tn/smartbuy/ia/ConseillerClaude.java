package tn.smartbuy.ia;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.anthropic.models.beta.messages.StructuredMessage;
import com.anthropic.models.beta.messages.StructuredMessageCreateParams;
import com.anthropic.models.beta.messages.StructuredTextBlock;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.smartbuy.ia.IaDtos.Conseil;
import tn.smartbuy.ia.IaDtos.Niveau;

import java.time.Duration;
import java.util.List;

/**
 * Conseiller basé sur le modèle d'IA Claude (Anthropic), via le SDK Java officiel.
 * <p>
 * Utilisé seulement si la variable d'environnement ANTHROPIC_API_KEY est définie.
 * On n'envoie que le {@link ResumeMois} (chiffres anonymes), jamais le nom ni l'e-mail.
 * En cas d'erreur (réseau, clé invalide, réponse illisible...), une exception est levée et
 * {@link IaService} se rabat sur {@link ConseillerRegles}.
 * <p>
 * « Sorties structurées » : on donne au SDK une classe Java ({@link ReponseConseils}), il en déduit
 * un schéma JSON que le modèle est obligé de respecter, puis il convertit la réponse en objet Java.
 */
@Component
public class ConseillerClaude implements ConseillerBudget {

    /** En-tête bêta du repli côté serveur Anthropic quand le modèle refuse une requête. */
    private static final String BETA_REPLI = "server-side-fallback-2026-07-01";

    private static final String CONSIGNES_CONSEILS = """
            Tu es le conseiller budgétaire de l'application SmartBuy, utilisée en Tunisie (montants en dinars, 3 décimales).
            Tu reçois le résumé chiffré et anonyme du mois d'un utilisateur (JSON). Donne 3 à 5 conseils concrets et courts,
            en français, fondés uniquement sur ces chiffres : n'invente aucune donnée.
            niveau : ALERTE pour un budget dépassé ou des dépenses supérieures aux revenus ; ATTENTION pour un risque
            (budget utilisé à plus de 80 %, forte hausse d'une catégorie, dépassement probable en fin de mois) ; INFO sinon.
            Le titre fait au plus 6 mots, le message au plus 2 phrases.
            """;

    private static final String CONSIGNES_ASSISTANT = """
            Tu es l'assistant budgétaire de l'application SmartBuy, utilisée en Tunisie (montants en dinars, 3 décimales).
            Réponds à la question de l'utilisateur en français, en 2 à 5 phrases, en t'appuyant sur le résumé chiffré
            et anonyme de son mois (JSON). N'invente aucun chiffre absent du résumé. Si la question ne concerne pas
            son budget, ses dépenses ou ses courses, dis-le poliment et propose une question utile.
            """;

    /** Format de réponse imposé pour les conseils. */
    public static class ReponseConseils {
        @JsonPropertyDescription("Conseils, du plus important au moins important")
        public List<ConseilIa> conseils;
    }

    public static class ConseilIa {
        @JsonPropertyDescription("Titre court du conseil")
        public String titre;
        @JsonPropertyDescription("Explication en une ou deux phrases")
        public String message;
        public Niveau niveau;
    }

    /** Format de réponse imposé pour l'assistant. */
    public static class ReponseAssistant {
        @JsonPropertyDescription("Réponse à la question, en français")
        public String reponse;
    }

    private final String cleApi;
    private final String modele;
    private final boolean repliServeur;
    private final Duration delai;
    private final ObjectMapper json;
    private AnthropicClient client;

    public ConseillerClaude(@Value("${smartbuy.ia.api-key:}") String cleApi,
                            @Value("${smartbuy.ia.model:claude-opus-5-5}") String modele,
                            @Value("${smartbuy.ia.repli-serveur:true}") boolean repliServeur,
                            @Value("${smartbuy.ia.timeout-secondes:60}") long timeoutSecondes,
                            ObjectMapper json) {
        this.cleApi = cleApi == null ? "" : cleApi.trim();
        this.modele = modele;
        this.repliServeur = repliServeur;
        this.delai = Duration.ofSeconds(timeoutSecondes);
        this.json = json;
    }

    /** Vrai si une clé API est configurée (sinon l'application utilise les règles). */
    public boolean estDisponible() {
        return !cleApi.isEmpty();
    }

    @Override
    public List<Conseil> conseils(ResumeMois resume) {
        ReponseConseils reponse = appeler(CONSIGNES_CONSEILS,
                "Résumé du mois :\n" + enJson(resume), ReponseConseils.class);
        if (reponse.conseils == null || reponse.conseils.isEmpty()) {
            throw new IllegalStateException("Réponse de l'IA sans conseil");
        }
        return reponse.conseils.stream()
                .filter(c -> c.titre != null && c.message != null)
                .map(c -> new Conseil(c.titre, c.message, c.niveau == null ? Niveau.INFO : c.niveau))
                .toList();
    }

    @Override
    public String repondre(String question, ResumeMois resume) {
        ReponseAssistant reponse = appeler(CONSIGNES_ASSISTANT,
                "Résumé du mois :\n" + enJson(resume) + "\n\nQuestion de l'utilisateur :\n" + question,
                ReponseAssistant.class);
        if (reponse.reponse == null || reponse.reponse.isBlank()) {
            throw new IllegalStateException("Réponse vide de l'IA");
        }
        return reponse.reponse.trim();
    }

    /** Un appel à l'API Messages de Claude avec une réponse JSON au format de {@code classe}. */
    private <T> T appeler(String consignes, String messageUtilisateur, Class<T> classe) {
        StructuredMessageCreateParams.Builder<T> requete = MessageCreateParams.builder()
                .model(modele)
                .maxTokens(16000L)
                .system(consignes)
                .addUserMessage(messageUtilisateur)
                // effort « low » : questions simples, réponse rapide et peu coûteuse
                .outputConfig(classe, BetaOutputConfig.Effort.LOW);
        if (repliServeur) {
            // Si le modèle refuse de répondre, Anthropic réessaie automatiquement avec un modèle de repli
            requete.addBeta(BETA_REPLI).fallbacksDefault();
        }

        StructuredMessage<T> reponse = client().beta().messages().create(requete.build());

        // Toujours vérifier la raison d'arrêt avant de lire le contenu
        BetaStopReason raison = reponse.stopReason().orElse(null);
        if (BetaStopReason.REFUSAL.equals(raison) || BetaStopReason.MAX_TOKENS.equals(raison)) {
            throw new IllegalStateException("Réponse de l'IA inutilisable (stop_reason = " + raison + ")");
        }
        return reponse.content().stream()
                .flatMap(bloc -> bloc.text().stream())
                .map(StructuredTextBlock::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Réponse de l'IA sans texte"));
    }

    /** Le client HTTP n'est créé qu'au premier appel, et seulement si une clé existe. */
    private synchronized AnthropicClient client() {
        if (!estDisponible()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY non configurée");
        }
        if (client == null) {
            client = AnthropicOkHttpClient.builder()
                    .apiKey(cleApi)
                    .timeout(delai)
                    .build();
        }
        return client;
    }

    private String enJson(ResumeMois resume) {
        try {
            return json.writerWithDefaultPrettyPrinter().writeValueAsString(resume);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Résumé impossible à convertir en JSON", e);
        }
    }
}
