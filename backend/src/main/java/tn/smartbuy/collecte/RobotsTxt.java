package tn.smartbuy.collecte;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lecture simplifiée d'un fichier robots.txt : le site y indique quelles pages les robots
 * ont le droit de visiter. SmartBuy le lit AVANT toute collecte et ne demande jamais une page interdite.
 * <p>
 * Règles appliquées (comme Google) :
 * <ul>
 *   <li>on prend le groupe qui nomme notre robot (« SmartBuy »), sinon le groupe « * » ;</li>
 *   <li>la règle Allow/Disallow la plus longue qui correspond au chemin gagne ;</li>
 *   <li>« * » remplace n'importe quelle suite de caractères, « $ » marque la fin de l'adresse.</li>
 * </ul>
 */
public final class RobotsTxt {

    private record Regle(boolean autorise, String motif, Pattern regex) {
    }

    private final List<Regle> regles;

    private RobotsTxt(List<Regle> regles) {
        this.regles = regles;
    }

    /** Aucun robots.txt (ou illisible) : tout est autorisé. */
    public static RobotsTxt toutAutorise() {
        return new RobotsTxt(List.of());
    }

    /** Tout interdit (ex. robots.txt répondant 401/403). */
    public static RobotsTxt toutInterdit() {
        return new RobotsTxt(List.of(new Regle(false, "/", Pattern.compile("^/.*"))));
    }

    public static RobotsTxt analyser(String contenu, String nomRobot) {
        String robot = nomRobot.toLowerCase(Locale.ROOT);
        List<Regle> pourNous = new ArrayList<>();
        List<Regle> pourTous = new ArrayList<>();
        List<String> agentsDuGroupe = new ArrayList<>();
        boolean dansLesRegles = false;

        for (String ligneBrute : contenu.split("\\R")) {
            String ligne = ligneBrute.replaceAll("#.*", "").trim();
            int deuxPoints = ligne.indexOf(':');
            if (deuxPoints < 0) {
                continue;
            }
            String cle = ligne.substring(0, deuxPoints).trim().toLowerCase(Locale.ROOT);
            String valeur = ligne.substring(deuxPoints + 1).trim();
            switch (cle) {
                case "user-agent" -> {
                    if (dansLesRegles) { // un nouveau groupe commence
                        agentsDuGroupe.clear();
                        dansLesRegles = false;
                    }
                    agentsDuGroupe.add(valeur.toLowerCase(Locale.ROOT));
                }
                case "allow", "disallow" -> {
                    dansLesRegles = true;
                    if (valeur.isEmpty()) {
                        continue; // « Disallow: » vide = rien d'interdit
                    }
                    Regle regle = new Regle(cle.equals("allow"), valeur, enRegex(valeur));
                    for (String agent : agentsDuGroupe) {
                        if (agent.equals("*")) {
                            pourTous.add(regle);
                        } else if (robot.contains(agent)) {
                            pourNous.add(regle);
                        }
                    }
                }
                default -> { /* Sitemap, Crawl-delay... ignorés */ }
            }
        }
        return new RobotsTxt(pourNous.isEmpty() ? pourTous : pourNous);
    }

    /** @param chemin partie de l'adresse après le nom de domaine, avec la requête (ex. /produits?page=2) */
    public boolean autorise(String chemin) {
        Regle meilleure = null;
        for (Regle r : regles) {
            if (r.regex().matcher(chemin).find()) {
                if (meilleure == null || r.motif().length() > meilleure.motif().length()
                        || (r.motif().length() == meilleure.motif().length() && r.autorise())) {
                    meilleure = r;
                }
            }
        }
        return meilleure == null || meilleure.autorise();
    }

    private static Pattern enRegex(String motif) {
        StringBuilder sb = new StringBuilder("^");
        for (char c : motif.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '$' -> sb.append("$");
                default -> sb.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(sb.toString());
    }
}
