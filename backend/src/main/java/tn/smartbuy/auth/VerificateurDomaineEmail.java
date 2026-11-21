package tn.smartbuy.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.smartbuy.commun.RequeteInvalideException;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

/**
 * Premier contrôle d'un e-mail « réel » : le domaine (après le @) doit pouvoir recevoir des e-mails,
 * c'est-à-dire avoir un enregistrement DNS de type MX (serveur de messagerie).
 * <p>
 * Exemples : gmail.com, outlook.com, vermeg.com → acceptés ; smartbuy.tn, test.com, example.com → refusés.
 * Le second contrôle, décisif, est le code envoyé à l'adresse (seul son propriétaire peut le lire).
 * <p>
 * Si le DNS est injoignable (pas de réseau), on n'empêche pas l'inscription : le code de confirmation
 * reste de toute façon obligatoire.
 */
@Component
public class VerificateurDomaineEmail {

    private static final Logger log = LoggerFactory.getLogger(VerificateurDomaineEmail.class);

    /** Résultat d'une recherche DNS, séparé pour pouvoir tester sans réseau. */
    public interface ResolveurMx {
        /** @return les serveurs MX du domaine (liste vide s'il n'y en a pas) ; NameNotFoundException si le domaine n'existe pas */
        List<String> serveursMx(String domaine) throws NamingException;
    }

    private final ResolveurMx resolveur;
    private final boolean actif;

    @org.springframework.beans.factory.annotation.Autowired
    public VerificateurDomaineEmail(@Value("${smartbuy.email.verifier-domaine:true}") boolean actif) {
        this(VerificateurDomaineEmail::rechercheDns, actif);
    }

    VerificateurDomaineEmail(ResolveurMx resolveur, boolean actif) {
        this.resolveur = resolveur;
        this.actif = actif;
    }

    public void verifier(String email) {
        if (!actif) {
            return;
        }
        String domaine = email.substring(email.lastIndexOf('@') + 1).trim().toLowerCase(Locale.ROOT);
        try {
            List<String> mx = resolveur.serveursMx(domaine);
            // « 0 . » (MX nul) signifie explicitement : ce domaine ne reçoit pas d'e-mails.
            boolean recoitDesEmails = mx.stream().anyMatch(s -> !s.isBlank() && !s.trim().endsWith(" ."));
            if (!recoitDesEmails) {
                throw new RequeteInvalideException(refus(domaine));
            }
        } catch (NameNotFoundException e) {
            throw new RequeteInvalideException(refus(domaine));
        } catch (NamingException e) {
            log.warn("Vérification DNS impossible pour {} ({}) : inscription autorisée, le code e-mail reste obligatoire",
                    domaine, e.getMessage());
        }
    }

    private static String refus(String domaine) {
        return "Adresse e-mail non valide : le domaine « %s » ne reçoit pas d'e-mails. Utilisez une adresse réelle (ex. Gmail)."
                .formatted(domaine);
    }

    /** Recherche DNS des enregistrements MX via JNDI (inclus dans Java, aucune dépendance). */
    private static List<String> rechercheDns(String domaine) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "2000");
        env.put("com.sun.jndi.dns.timeout.retries", "1");
        InitialDirContext dns = new InitialDirContext(env);
        try {
            Attribute attribut = dns.getAttributes(domaine, new String[]{"MX"}).get("MX");
            List<String> resultat = new ArrayList<>();
            if (attribut != null) {
                for (int i = 0; i < attribut.size(); i++) {
                    resultat.add(String.valueOf(attribut.get(i)));
                }
            }
            return resultat;
        } finally {
            dns.close();
        }
    }
}
