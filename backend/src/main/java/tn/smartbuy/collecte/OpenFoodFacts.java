package tn.smartbuy.collecte;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Open Food Facts : base de données ouverte et collaborative des produits alimentaires (licence ODbL).
 * Elle contient des milliers de produits tunisiens (codes-barres commençant par 619).
 * <p>
 * On lit la page publique d'un produit : https://world.openfoodfacts.org/product/{code-barres}.
 * Le robots.txt d'Open Food Facts autorise ces pages (il interdit /api, /cgi et /facets aux robots :
 * on ne les utilise donc pas). Comme toute la collecte, l'accès passe par {@link ClientWebPoli}
 * (robots.txt vérifié, 1 requête/s, robot identifié).
 */
@Component
public class OpenFoodFacts {

    /** Ce qu'Open Food Facts sait d'un code-barres. */
    public record Fiche(String codeBarres, String nom, String marque, String quantite, String imageUrl,
                        List<String> categories) {
    }

    private static final Pattern TITRE = Pattern.compile("<title>([^<]+)</title>");
    private static final Pattern MARQUE = Pattern.compile("itemprop=\"brand\"[^>]*>([^<]+)<");
    private static final Pattern QUANTITE = Pattern.compile("id=\"field_quantity_value\"[^>]*>([^<]+)<");
    private static final Pattern CATEGORIES = Pattern.compile("id=\"field_categories_value\"[^>]*>(.*?)</span>", Pattern.DOTALL);
    private static final Pattern LIEN_CATEGORIE = Pattern.compile("/facets/categories/([^\"]+)\"");
    private static final Pattern IMAGE = Pattern.compile(
            "(https://images\\.openfoodfacts\\.org/images/products/[^\"\\s]+/front[^\"\\s]*\\.(?:200|400)\\.jpg)");

    private final ClientWebPoli web;

    public OpenFoodFacts(ClientWebPoli web) {
        this.web = web;
    }

    public Optional<Fiche> chercher(String codeBarres) throws IOException {
        String html;
        try {
            html = web.get("https://world.openfoodfacts.org/product/" + codeBarres);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("HTTP 404")) {
                return Optional.empty(); // produit inconnu d'Open Food Facts
            }
            throw e;
        }
        return extraire(codeBarres, html);
    }

    /**
     * Lit la fiche dans le HTML. Le titre de la page a la forme « Nom – Marque – Quantité ».
     * Séparé de l'appel réseau pour être testé sans Internet.
     */
    static Optional<Fiche> extraire(String codeBarres, String html) {
        Matcher titre = TITRE.matcher(html);
        if (!titre.find()) {
            return Optional.empty();
        }
        String[] morceaux = decoder(titre.group(1)).split("\\s+[–-]\\s+");
        String nom = morceaux[0].trim();
        String titreBas = titre.group(1).toLowerCase();
        // Page d'erreur (« Error », « Error – Open Food Facts ») ou page générique : pas de produit.
        if (nom.isEmpty() || nom.equalsIgnoreCase("error") || titreBas.contains("open food facts")
                || titreBas.contains("not found") || titreBas.contains("introuvable") || nom.equals(codeBarres)) {
            return Optional.empty();
        }
        String marque = trouver(MARQUE, html);
        if (marque == null && morceaux.length > 1) {
            marque = morceaux[1].trim();
        }
        String quantite = trouver(QUANTITE, html);
        List<String> categories = new ArrayList<>();
        Matcher bloc = CATEGORIES.matcher(html);
        if (bloc.find()) {
            Matcher lien = LIEN_CATEGORIE.matcher(bloc.group(1));
            while (lien.find()) {
                categories.add(URLDecoder.decode(lien.group(1), StandardCharsets.UTF_8).toLowerCase());
            }
        }
        return Optional.of(new Fiche(codeBarres, nom, marque, quantite, trouver(IMAGE, html), categories));
    }

    private static String trouver(Pattern motif, String html) {
        Matcher m = motif.matcher(html);
        return m.find() ? decoder(m.group(1)).trim() : null;
    }

    private static String decoder(String s) {
        return s.replace("&amp;", "&").replace("&#39;", "'").replace("&#039;", "'").replace("&quot;", "\"");
    }
}
