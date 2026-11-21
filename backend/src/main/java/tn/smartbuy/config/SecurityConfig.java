package tn.smartbuy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tn.smartbuy.commun.ApiErreur;
import tn.smartbuy.securite.JwtFiltre;
import tn.smartbuy.securite.JwtService;

import java.io.IOException;
import java.util.List;

/**
 * Configuration de la sécurité :
 * <ul>
 *   <li>pas de session ni de cookie : chaque requête porte son jeton JWT (API « stateless ») ;</li>
 *   <li>seules /api/auth/register et /api/auth/login sont publiques ;</li>
 *   <li>les routes d'administration sont protégées par {@code @PreAuthorize("hasRole('ADMIN')")}
 *       (grâce à {@code @EnableMethodSecurity}) ;</li>
 *   <li>CORS ouvert pour le développement (Flutter Web tourne sur un autre port).</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filtreSecurite(HttpSecurity http, JwtService jwtService, ObjectMapper json)
            throws Exception {
        http
                // CSRF protège les sessions par cookie : inutile avec un jeton dans l'en-tête
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(configurationCors()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                                "/api/auth/mot-de-passe-oublie", "/api/auth/reinitialiser-mot-de-passe",
                                "/api/auth/verifier-email", "/api/auth/renvoyer-code").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                // Réponses JSON { status, message } au lieu des pages d'erreur par défaut
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, rep, ex) ->
                                ecrire(rep, json, 401, "Authentification requise : jeton absent, invalide ou expiré"))
                        .accessDeniedHandler((req, rep, ex) ->
                                ecrire(rep, json, 403, "Accès réservé aux administrateurs")))
                .addFilterBefore(new JwtFiltre(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt : hachage lent et salé, adapté aux mots de passe (NF sécurité). */
    @Bean
    public PasswordEncoder encodeurMotDePasse() {
        return new BCryptPasswordEncoder();
    }

    private CorsConfigurationSource configurationCors() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static void ecrire(HttpServletResponse reponse, ObjectMapper json, int statut, String message)
            throws IOException {
        reponse.setStatus(statut);
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding("UTF-8");
        json.writeValue(reponse.getOutputStream(), new ApiErreur(statut, message));
    }
}
