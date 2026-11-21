package tn.smartbuy.collecte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client HTTP « poli » utilisé pour toute la collecte (web scraping) :
 * <ol>
 *   <li>il se présente honnêtement (User-Agent « SmartBuy-etudiant ») ;</li>
 *   <li>il lit le robots.txt de chaque site et refuse les pages interdites ;</li>
 *   <li>il attend au moins 1 seconde entre deux requêtes vers le même site (pas de surcharge).</li>
 * </ol>
 */
@Component
public class ClientWebPoli {

    public static final String USER_AGENT = "SmartBuy-etudiant/1.0 (projet universitaire)";
    private static final Duration PAUSE = Duration.ofSeconds(1);
    private static final Logger log = LoggerFactory.getLogger(ClientWebPoli.class);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final Map<String, RobotsTxt> robotsParSite = new ConcurrentHashMap<>();
    private final Map<String, Long> derniereRequete = new ConcurrentHashMap<>();

    /** Page refusée par le robots.txt du site. */
    public static class PageInterditeException extends IOException {
        public PageInterditeException(String url) {
            super("Accès refusé par robots.txt : " + url);
        }
    }

    public String get(String url) throws IOException {
        return envoyer(HttpRequest.newBuilder(verifier(url)).GET());
    }

    public String postJson(String url, String json) throws IOException {
        return envoyer(HttpRequest.newBuilder(verifier(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)));
    }

    private URI verifier(String url) throws IOException {
        URI uri = URI.create(url);
        String chemin = (uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        if (!robots(uri).autorise(chemin)) {
            throw new PageInterditeException(url);
        }
        return uri;
    }

    private RobotsTxt robots(URI uri) {
        String site = uri.getScheme() + "://" + uri.getHost();
        return robotsParSite.computeIfAbsent(site, s -> {
            try {
                HttpResponse<String> r = http.send(requete(URI.create(s + "/robots.txt")).GET().build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (r.statusCode() == 401 || r.statusCode() == 403) {
                    return RobotsTxt.toutInterdit();
                }
                return r.statusCode() == 200 ? RobotsTxt.analyser(r.body(), USER_AGENT) : RobotsTxt.toutAutorise();
            } catch (IOException | InterruptedException e) {
                log.warn("robots.txt illisible pour {} ({}) : collecte refusée par prudence", s, e.getMessage());
                return RobotsTxt.toutInterdit();
            }
        });
    }

    private String envoyer(HttpRequest.Builder builder) throws IOException {
        HttpRequest req = builder.header("User-Agent", USER_AGENT)
                .header("Accept-Language", "fr")
                .timeout(Duration.ofSeconds(25))
                .build();
        patienter(req.uri().getHost());
        try {
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() >= 400) {
                throw new IOException("HTTP " + r.statusCode() + " pour " + req.uri());
            }
            return r.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Requête interrompue", e);
        }
    }

    private HttpRequest.Builder requete(URI uri) {
        return HttpRequest.newBuilder(uri).header("User-Agent", USER_AGENT).timeout(Duration.ofSeconds(15));
    }

    /** Au plus une requête par seconde et par site. */
    private void patienter(String site) {
        synchronized (derniereRequete) {
            long maintenant = System.currentTimeMillis();
            long attente = derniereRequete.getOrDefault(site, 0L) + PAUSE.toMillis() - maintenant;
            if (attente > 0) {
                try {
                    Thread.sleep(attente);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            derniereRequete.put(site, System.currentTimeMillis());
        }
    }
}
