package tn.smartbuy.ia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Objets JSON des fonctions avancées (BUD-16 + assistant). */
public final class IaDtos {

    private IaDtos() {
    }

    /** Gravité d'un conseil (valeurs du contrat d'API). */
    public enum Niveau {
        INFO,
        ATTENTION,
        ALERTE
    }

    public record Conseil(String titre, String message, Niveau niveau) {
    }

    /** source = "IA" (Claude) ou "REGLES" (moteur local). */
    public record ConseilsReponse(String source, List<Conseil> conseils) {
    }

    public record QuestionRequete(
            @NotBlank(message = "La question est obligatoire") @Size(max = 500, message = "Question trop longue (500 caractères maximum)")
            String question) {
    }

    public record AssistantReponse(String source, String reponse) {
    }
}
