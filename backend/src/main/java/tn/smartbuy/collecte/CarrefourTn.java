package tn.smartbuy.collecte;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prix réels de Carrefour Tunisie (www.carrefour.tn).
 * <p>
 * Le site est construit sur Magento, qui expose une API publique GraphQL (celle qu'utilise le site
 * lui-même pour afficher ses pages). Sur ce site, la référence d'un produit (« sku ») EST son code-barres
 * EAN-13 : on peut donc demander directement le prix d'une liste de codes-barres.
 * Le robots.txt de carrefour.tn autorise l'accès (seules les pages d'administration sont interdites).
 */
@Component
public class CarrefourTn {

    public static final String ENSEIGNE = "Carrefour";
    private static final String API = "https://www.carrefour.tn/graphql";
    private static final int PAR_PAQUET = 40;

    /** Un produit vendu sur carrefour.tn. */
    public record Offre(String codeBarres, String nom, BigDecimal prix, String url, boolean enStock) {
    }

    private final ClientWebPoli web;
    private final ObjectMapper json;

    public CarrefourTn(ClientWebPoli web, ObjectMapper json) {
        this.web = web;
        this.json = json;
    }

    /** Prix des codes-barres demandés (ceux que Carrefour ne vend pas sont absents du résultat). */
    public Map<String, Offre> prix(Collection<String> codesBarres) throws IOException {
        List<String> codes = codesBarres.stream().distinct().toList();
        Map<String, Offre> resultat = new HashMap<>();
        for (int i = 0; i < codes.size(); i += PAR_PAQUET) {
            List<String> paquet = codes.subList(i, Math.min(i + PAR_PAQUET, codes.size()));
            String liste = paquet.stream().map(c -> "\\\"" + c + "\\\"").collect(Collectors.joining(", "));
            String requete = "{\"query\":\"{ products(filter: { sku: { in: [" + liste + "] } }, pageSize: " + PAR_PAQUET
                    + ") { items { name sku url_key stock_status price_range { minimum_price { final_price { value } } } } } }\"}";
            resultat.putAll(lire(json.readTree(web.postJson(API, requete))));
        }
        return resultat;
    }

    /** Séparé de l'appel réseau pour être testé sans Internet. */
    static Map<String, Offre> lire(JsonNode racine) {
        Map<String, Offre> offres = new HashMap<>();
        for (JsonNode item : racine.path("data").path("products").path("items")) {
            JsonNode prix = item.path("price_range").path("minimum_price").path("final_price").path("value");
            if (!prix.isNumber() || prix.decimalValue().signum() <= 0) {
                continue;
            }
            String sku = item.path("sku").asText();
            String urlKey = item.path("url_key").asText("");
            offres.put(sku, new Offre(sku, item.path("name").asText().trim(), prix.decimalValue(),
                    urlKey.isEmpty() ? null : "https://www.carrefour.tn/" + urlKey + ".html",
                    "IN_STOCK".equals(item.path("stock_status").asText())));
        }
        return offres;
    }
}
