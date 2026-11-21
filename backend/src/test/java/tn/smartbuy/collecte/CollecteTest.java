package tn.smartbuy.collecte;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Collecte des prix : règles robots.txt et lecture des pages / réponses (sans réseau). */
class CollecteTest {

    @Test
    @DisplayName("robots.txt : la règle la plus précise gagne, les jokers * et $ sont compris")
    void robots() {
        RobotsTxt r = RobotsTxt.analyser("""
                User-agent: ClaudeBot
                Disallow: /

                User-agent: *
                Disallow: /*?search_query=
                Disallow: /admin/
                Allow: /admin/public$
                """, ClientWebPoli.USER_AGENT);
        assertThat(r.autorise("/130-smartphone-tunisie")).isTrue();
        assertThat(r.autorise("/recherche?search_query=iphone")).isFalse();
        assertThat(r.autorise("/admin/config")).isFalse();
        assertThat(r.autorise("/admin/public")).isTrue();
    }

    @Test
    @DisplayName("robots.txt : un groupe qui nomme notre robot remplace le groupe *")
    void robotsGroupeDedie() {
        RobotsTxt r = RobotsTxt.analyser("""
                User-agent: *
                Allow: /
                User-agent: SmartBuy
                Disallow: /
                """, ClientWebPoli.USER_AGENT);
        assertThat(r.autorise("/produits")).isFalse();
        assertThat(RobotsTxt.analyser("User-agent: *\nDisallow: /\n", "x").autorise("/a")).isFalse();
    }

    @Test
    @DisplayName("Carrefour : lecture de la réponse GraphQL (sku = code-barres, prix en TND)")
    void carrefour() throws Exception {
        var json = new ObjectMapper().readTree("""
                {"data":{"products":{"items":[
                  {"name":"Lait demi-écrémé U.H.T ","sku":"6191507220214","url_key":"lait-demi-ecreme-uht",
                   "stock_status":"IN_STOCK","price_range":{"minimum_price":{"final_price":{"value":1.35}}}},
                  {"name":"Sans prix","sku":"123","price_range":{"minimum_price":{"final_price":{"value":0}}}}]}}}
                """);
        var offres = CarrefourTn.lire(json);
        assertThat(offres).containsOnlyKeys("6191507220214");
        assertThat(offres.get("6191507220214").prix()).isEqualByComparingTo("1.35");
        assertThat(offres.get("6191507220214").nom()).isEqualTo("Lait demi-écrémé U.H.T");
        assertThat(offres.get("6191507220214").url()).isEqualTo("https://www.carrefour.tn/lait-demi-ecreme-uht.html");
    }

    @Test
    @DisplayName("Spacenet : extraction nom / prix / adresse des vignettes d'une page catégorie")
    void spacenet() {
        String html = """
                <div class="item-product-list"> <h3 class="product_name"><a href="https://spacenet.tn/iphone-tunisie/95348-iphone-17-512go-blanc.html">iPhone 17 512Go Blanc</a></h3>
                <div class="product-price-and-shipping"> <span class="price"> <span content="TND"></span><span content="6295">6 295,000 DT</span> </span></div></div>
                <div class="item-product-list"> <h3 class="product_name"><a href="https://spacenet.tn/smartphone/7-redmi.html">Redmi 15C</a></h3>
                <span class="price">
                    <span  content="TND"></span><span  content="169">169,000&nbsp;DT</span></span></div>
                <div class="item-product-list"> <h3 class="product_name"><a href="https://spacenet.tn/x/1-sans-prix.html">Sans prix</a></h3></div>
                """;
        var articles = SpacenetTn.extraireArticles(html);
        assertThat(articles).hasSize(2); // la vignette sans prix est ignorée
        assertThat(articles.get(1).prix()).isEqualByComparingTo("169"); // « <span  content= » : 2 espaces, comme sur le vrai site
        assertThat(articles.get(0).nom()).isEqualTo("iPhone 17 512Go Blanc");
        assertThat(articles.get(0).prix()).isEqualByComparingTo(new BigDecimal("6295"));
        assertThat(articles.get(0).url()).endsWith("95348-iphone-17-512go-blanc.html");
    }

    @Test
    @DisplayName("Open Food Facts : nom, marque, quantité et catégories lus dans la page produit")
    void openFoodFacts() {
        String html = """
                <html><head><title>Lait demi-écrémé UHT – Délice – 1 L</title></head><body>
                <span id="field_brands_value"><a itemprop="brand" href="/facets/brands/D%C3%A9lice">Délice</a></span>
                <span id="field_quantity_value">1 L</span>
                <span id="field_categories_value"><a href="/facets/categories/Dairies" class="tag">Dairies</a>,
                <a href="/facets/categories/UHT%20Milks" class="tag">UHT Milks</a></span></body></html>
                """;
        var fiche = OpenFoodFacts.extraire("6194043001019", html).orElseThrow();
        assertThat(fiche.nom()).isEqualTo("Lait demi-écrémé UHT");
        assertThat(fiche.marque()).isEqualTo("Délice");
        assertThat(fiche.quantite()).isEqualTo("1 L");
        assertThat(fiche.categories()).containsExactly("dairies", "uht milks");
        assertThat(OpenFoodFacts.extraire("1", "<title>Error – Open Food Facts</title>")).isEmpty();
    }

    @Test
    @DisplayName("unité déduite de la quantité, marque déduite du nom")
    void deductions() {
        assertThat(CollecteService.unite("1,5 L")).isEqualTo("L");
        assertThat(CollecteService.unite("25 cl")).isEqualTo("L");
        assertThat(CollecteService.unite("1kg")).isEqualTo("kg");
        assertThat(CollecteService.unite("400 g")).isEqualTo("pièce");
        assertThat(CollecteService.marqueDepuisNom("iPhone 17 512Go")).isEqualTo("Apple");
        assertThat(CollecteService.marqueDepuisNom("Samsung Galaxy S25")).isEqualTo("Samsung");
    }
}
