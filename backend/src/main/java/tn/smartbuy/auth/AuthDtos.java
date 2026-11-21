package tn.smartbuy.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import tn.smartbuy.commun.Montants;

/**
 * Objets échangés en JSON par le module « auth » (DTO = Data Transfer Object).
 * <p>
 * Pourquoi des DTO et pas directement l'entité ? Pour ne jamais renvoyer le mot de passe haché
 * et pour garder exactement les noms de champs du contrat d'API.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record InscriptionRequete(
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 100, message = "Le nom est trop long") String nom,
            @NotBlank(message = "L'e-mail est obligatoire") @Email(message = "L'e-mail n'est pas valide") String email,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères") String motDePasse) {
    }

    public record ConnexionRequete(
            @NotBlank(message = "L'e-mail est obligatoire") String email,
            @NotBlank(message = "Le mot de passe est obligatoire") String motDePasse) {
    }

    public record ProfilRequete(
            @NotBlank(message = "Le nom est obligatoire") @Size(max = 100, message = "Le nom est trop long") String nom,
            @Size(min = 3, max = 3, message = "La devise doit faire 3 lettres (ex. TND)") String devise,
            @PositiveOrZero(message = "Le revenu mensuel ne peut pas être négatif") Double revenuMensuel) {
    }

    /** AUTH-05, étape 1. */
    public record DemandeCodeRequete(
            @NotBlank(message = "L'e-mail est obligatoire") @Email(message = "L'e-mail n'est pas valide") String email) {
    }

    /** AUTH-05, étape 2. */
    public record ReinitialisationRequete(
            @NotBlank(message = "L'e-mail est obligatoire") String email,
            @NotBlank(message = "Le code est obligatoire") String code,
            @NotBlank(message = "Le mot de passe est obligatoire")
            @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères") String nouveauMotDePasse) {
    }

    public record MessageReponse(String message) {
    }

    /** Inscription étape 2 : code reçu par e-mail. */
    public record VerificationEmailRequete(
            @NotBlank(message = "L'e-mail est obligatoire") String email,
            @NotBlank(message = "Le code est obligatoire") String code) {
    }

    /** Réponse de /auth/register : pas encore de jeton, il faut d'abord confirmer l'e-mail. */
    public record InscriptionReponse(String message, String email) {
    }

    /** Objet « Utilisateur » du contrat. */
    public record UtilisateurDto(Long id, String nom, String email, String role, String devise, Double revenuMensuel) {

        public static UtilisateurDto depuis(Utilisateur u) {
            return new UtilisateurDto(u.getId(), u.getNom(), u.getEmail(), u.getRole().name(), u.getDevise(),
                    Montants.versDouble(u.getRevenuMensuel()));
        }
    }

    /** Réponse de /auth/register et /auth/login. */
    public record AuthReponse(String token, UtilisateurDto utilisateur) {
    }
}
