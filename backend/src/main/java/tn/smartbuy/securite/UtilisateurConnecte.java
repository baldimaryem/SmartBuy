package tn.smartbuy.securite;

/**
 * Identité de l'utilisateur qui fait la requête, lue dans le jeton JWT.
 * Les contrôleurs la reçoivent avec {@code @AuthenticationPrincipal UtilisateurConnecte moi}.
 */
public record UtilisateurConnecte(Long id, String email, String role) {

    public boolean estAdmin() {
        return "ADMIN".equals(role);
    }
}
