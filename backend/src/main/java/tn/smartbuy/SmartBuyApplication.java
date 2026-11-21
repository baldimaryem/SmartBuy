package tn.smartbuy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de l'API SmartBuy.
 * <p>
 * {@code @EnableScheduling} active les tâches planifiées (dépenses récurrentes, bilan mensuel)
 * et {@code @EnableAsync} permet d'envoyer les e-mails en arrière-plan sans ralentir les requêtes.
 * <p>
 * On exclut l'utilisateur « user / mot de passe généré » que Spring crée par défaut :
 * l'authentification passe uniquement par nos jetons JWT.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
@EnableAsync
public class SmartBuyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartBuyApplication.class, args);
    }
}
