package tn.smartbuy.email;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envoi des e-mails (alertes de budget, bilan mensuel).
 * <p>
 * Règle importante : un problème d'e-mail (serveur SMTP arrêté...) ne doit JAMAIS faire échouer
 * la requête de l'utilisateur. On attrape donc toutes les erreurs et on les écrit dans le journal.
 * Envoi par Gmail (SMTP smtp.gmail.com:587, STARTTLS) avec un mot de passe d'application :
 * voir backend/config/application.yml.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean active;
    private final String expediteur;

    public EmailService(JavaMailSender mailSender,
                        @Value("${smartbuy.mail.enabled:true}") boolean active,
                        @Value("${smartbuy.mail.from:no-reply@smartbuy.tn}") String expediteur,
                        @Value("${spring.mail.username:}") String compteEnvoi,
                        @Value("${spring.mail.password:}") String motDePasse) {
        this.mailSender = mailSender;
        this.expediteur = expediteur;
        // Sans compte d'envoi configuré (backend/config/application.yml), on n'essaie même pas :
        // les codes de confirmation sont alors affichés dans la console du serveur.
        boolean configure = !compteEnvoi.isBlank() && !motDePasse.isBlank() && !motDePasse.equals("COLLER_ICI");
        this.active = active && configure;
        if (!configure) {
            log.warn("E-MAILS NON CONFIGURÉS : renseignez spring.mail.username et spring.mail.password "
                    + "(mot de passe d'application Google) dans backend/config/application.yml. "
                    + "En attendant, les codes s'affichent dans cette console.");
        }
    }

    /**
     * Au démarrage, on teste la connexion à Gmail (sans envoyer d'e-mail) : une erreur de mot de passe
     * se voit tout de suite dans la console, au lieu d'être découverte quand un utilisateur ne reçoit rien.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verifierConnexion() {
        if (!active || !(mailSender instanceof JavaMailSenderImpl smtp)) {
            return;
        }
        new Thread(() -> {
            try {
                smtp.testConnection();
                log.info("E-MAILS OK : connexion à {} réussie, envoi depuis {}", smtp.getHost(), expediteur);
            } catch (Exception e) {
                log.error("E-MAILS EN ÉCHEC : connexion à {} refusée ({}). Vérifiez le mot de passe d'application "
                        + "(16 lettres, sans espaces) dans backend/config/application.yml.", smtp.getHost(), e.getMessage());
            }
        }, "verification-smtp").start();
    }

    /**
     * Envoi immédiat (synchrone).
     * @return true si l'e-mail est parti, false sinon (désactivé ou erreur)
     */
    public boolean envoyer(String destinataire, String sujet, String html) {
        if (!active) {
            log.info("Envoi d'e-mails non configuré : « {} » non envoyé à {}", sujet, destinataire);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper aide = new MimeMessageHelper(message, false, "UTF-8");
            aide.setFrom(expediteur);
            aide.setTo(destinataire);
            aide.setSubject(sujet);
            aide.setText(html, true);
            mailSender.send(message);
            log.info("E-mail « {} » envoyé à {}", sujet, destinataire);
            return true;
        } catch (Exception e) {
            log.warn("Échec de l'envoi de l'e-mail « {} » à {} : {}", sujet, destinataire, e.getMessage());
            return false;
        }
    }

    /**
     * Envoi en arrière-plan (@Async) : la requête HTTP n'attend pas le serveur SMTP.
     * Utilisé pour les alertes de budget (BUD-11).
     */
    @Async
    public void envoyerEnArrierePlan(String destinataire, String sujet, String html) {
        envoyer(destinataire, sujet, html);
    }

    /** Petite mise en page HTML commune à tous les e-mails. */
    public static String gabarit(String titre, String contenuHtml) {
        return """
                <html><body style="font-family: Arial, sans-serif; color: #222;">
                <h2 style="color: #2e7d32;">SmartBuy</h2>
                <h3>%s</h3>
                %s
                <p style="color: #888; font-size: 12px;">E-mail automatique envoyé par SmartBuy, merci de ne pas répondre.</p>
                </body></html>
                """.formatted(echapper(titre), contenuHtml);
    }

    /** Évite qu'un nom contenant « < » casse le HTML. */
    public static String echapper(String texte) {
        if (texte == null) {
            return "";
        }
        return texte.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
