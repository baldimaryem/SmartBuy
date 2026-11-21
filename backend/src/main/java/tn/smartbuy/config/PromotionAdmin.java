package tn.smartbuy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.auth.Role;
import tn.smartbuy.auth.UtilisateurRepository;

import java.util.Locale;

/**
 * Qui est administrateur ? Plus de compte fictif écrit dans le code :
 * l'administrateur est une personne réelle qui s'inscrit normalement (avec confirmation de son e-mail),
 * et dont l'adresse est déclarée dans la configuration : smartbuy.admin.email (variable ADMIN_EMAIL).
 * Au démarrage, ce compte reçoit le rôle ADMIN s'il existe déjà ; sinon il l'obtiendra à son inscription.
 */
@Component
public class PromotionAdmin {

    private static final Logger log = LoggerFactory.getLogger(PromotionAdmin.class);

    private final UtilisateurRepository utilisateurs;
    private final String emailAdmin;

    public PromotionAdmin(UtilisateurRepository utilisateurs, @Value("${smartbuy.admin.email:}") String emailAdmin) {
        this.utilisateurs = utilisateurs;
        this.emailAdmin = emailAdmin.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void appliquer() {
        if (emailAdmin.isEmpty()) {
            log.warn("Aucun administrateur configuré (smartbuy.admin.email / ADMIN_EMAIL) : l'espace admin est inaccessible.");
            return;
        }
        utilisateurs.findByEmail(emailAdmin).ifPresentOrElse(u -> {
            if (u.getRole() != Role.ADMIN) {
                u.setRole(Role.ADMIN);
                log.info("{} reçoit le rôle ADMIN", emailAdmin);
            }
        }, () -> log.info("Administrateur attendu : {} (rôle ADMIN attribué à son inscription)", emailAdmin));
    }
}
