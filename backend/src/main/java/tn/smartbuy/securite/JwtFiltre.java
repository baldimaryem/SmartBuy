package tn.smartbuy.securite;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre exécuté une fois par requête : lit l'en-tête {@code Authorization: Bearer <jeton>}.
 * Si le jeton est valide, l'utilisateur est déclaré « authentifié » pour la suite de la requête.
 * Sinon on ne fait rien : Spring Security répondra 401 sur les routes protégées.
 * <p>
 * Pas de @Component : le filtre est créé dans SecurityConfig, pour qu'il ne s'exécute
 * que dans la chaîne de sécurité (et pas une seconde fois comme filtre « normal »).
 */
public class JwtFiltre extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;

    public JwtFiltre(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        String entete = requete.getHeader("Authorization");
        if (entete != null && entete.startsWith(PREFIXE)) {
            jwtService.lireJeton(entete.substring(PREFIXE.length())).ifPresent(utilisateur -> {
                // Spring attend des rôles préfixés par ROLE_ (hasRole('ADMIN') vérifie ROLE_ADMIN)
                var droits = List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.role()));
                var authentification = new UsernamePasswordAuthenticationToken(utilisateur, null, droits);
                SecurityContextHolder.getContext().setAuthentication(authentification);
            });
        }
        chaine.doFilter(requete, reponse);
    }
}
