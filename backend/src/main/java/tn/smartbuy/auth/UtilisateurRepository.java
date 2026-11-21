package tn.smartbuy.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Accès à la table UTILISATEUR. Spring Data génère le SQL à partir du nom des méthodes
 * (ex. findByEmail -> SELECT ... WHERE email = ?).
 */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);
}
