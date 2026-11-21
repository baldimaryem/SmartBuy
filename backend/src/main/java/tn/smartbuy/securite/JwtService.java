package tn.smartbuy.securite;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Création et vérification des jetons JWT (AUTH-02).
 * <p>
 * Pourquoi JWT ? L'API est « sans état » (stateless) : le serveur ne garde aucune session.
 * Le jeton signé contient l'identifiant et le rôle ; s'il a été modifié, la signature ne correspond plus.
 */
@Service
public class JwtService {

    private final Key cle;
    private final Duration validite;

    public JwtService(@Value("${smartbuy.jwt.secret}") String secret,
                      @Value("${smartbuy.jwt.expiration-heures:24}") long heures) {
        // HS256 exige une clé d'au moins 256 bits (32 octets)
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("smartbuy.jwt.secret doit contenir au moins 32 caractères");
        }
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validite = Duration.ofHours(heures);
    }

    public String genererJeton(Long utilisateurId, String email, String role) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(utilisateurId))
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(Date.from(maintenant))
                .setExpiration(Date.from(maintenant.plus(validite)))
                .signWith(cle, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Renvoie l'utilisateur contenu dans le jeton, ou vide si le jeton est faux ou expiré. */
    public Optional<UtilisateurConnecte> lireJeton(String jeton) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(cle).build()
                    .parseClaimsJws(jeton)
                    .getBody();
            return Optional.of(new UtilisateurConnecte(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
