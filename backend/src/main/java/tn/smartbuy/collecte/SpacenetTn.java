package tn.smartbuy.collecte;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prix high-tech de Spacenet (spacenet.tn), boutique tunisienne (PrestaShop).
 * <p>
 * Web scraping : on lit les pages « catégorie » (smartphones, PC portables...) comme le ferait un navigateur,
 * et on extrait de chaque vignette produit son nom, son adresse et son prix.
 * Le robots.txt de Spacenet interdit la recherche (?search_query=) : on ne l'utilise donc jamais,
 * on passe uniquement par les pages catégories, qui sont autorisées.
 */
@Component
public class SpacenetTn {

    public static final String ENSEIGNE = "Spacenet";

    /** Pages catégories collectées et rayon SmartBuy correspondant. */
    public record PageCategorie(String url, String rayon) {
    }

    public static final List<PageCategorie> PAGES = List.of(
            new PageCategorie("https://spacenet.tn/130-smartphone-tunisie", "Smartphones"),
            new PageCategorie("https://spacenet.tn/211-iphone-tunisie", "Smartphones"),
            new PageCategorie("https://spacenet.tn/18-ordinateur-portable", "Ordinateurs portables"),
            new PageCategorie("https://spacenet.tn/204-pc-portable-gamer-tunisie", "Ordinateurs portables"));

    /** Un produit lu sur une page catégorie. */
    public record Article(String nom, BigDecimal prix, String url) {
    }

    private static final Pattern NOM = Pattern.compile("class=\"product_name\"[^>]*>\\s*(?:<a[^>]*>)?\\s*([^<]+)");
    private static final Pattern PRIX = Pattern.compile(
            "class=\"price\"[^>]*>\\s*(?:<span[^>]*></span>)?\\s*<span\\s+content=\"([0-9]+(?:\\.[0-9]+)?)\"");
    private static final Pattern URL = Pattern.compile("href=\"(https://spacenet\\.tn/[^\"]+\\.html)\"");
    private static final Pattern PRIX_FICHE = Pattern.compile("\"price\"\\s*:\\s*\"([0-9]+(?:\\.[0-9]+)?)\"");

    private final ClientWebPoli web;

    public SpacenetTn(ClientWebPoli web) {
        this.web = web;
    }

    public List<Article> lirePage(String url) throws IOException {
        return extraireArticles(web.get(url));
    }

    /** Prix actuel d'un produit à partir de sa fiche (pour « Actualiser les prix »). */
    public Optional<BigDecimal> prixFiche(String url) throws IOException {
        Matcher m = PRIX_FICHE.matcher(web.get(url));
        while (m.find()) {
            BigDecimal prix = new BigDecimal(m.group(1));
            if (prix.signum() > 0) {
                return Optional.of(prix);
            }
        }
        return Optional.empty();
    }

    /** Découpe la page en vignettes produit, puis lit nom / prix / adresse dans chacune. Testable sans réseau. */
    static List<Article> extraireArticles(String html) {
        List<Article> articles = new ArrayList<>();
        String[] vignettes = html.split("class=\"item-product-list\"");
        for (int i = 1; i < vignettes.length; i++) {
            String v = vignettes[i];
            Matcher nom = NOM.matcher(v);
            Matcher prix = PRIX.matcher(v);
            Matcher url = URL.matcher(v);
            if (nom.find() && prix.find() && url.find()) {
                articles.add(new Article(decoder(nom.group(1).trim()), new BigDecimal(prix.group(1)), url.group(1)));
            }
        }
        return articles;
    }

    private static String decoder(String s) {
        return s.replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&#39;", "'");
    }
}
