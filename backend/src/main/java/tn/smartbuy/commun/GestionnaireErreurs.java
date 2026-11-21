package tn.smartbuy.commun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Transforme toutes les exceptions en réponse JSON {@code { status, message }} (voir le contrat d'API).
 * <p>
 * Pourquoi un gestionnaire global ? Les services se contentent de lancer une exception
 * (« introuvable », « interdit »...) et c'est ici, à un seul endroit, qu'on choisit le code HTTP.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreurs.class);

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ApiErreur> introuvable(RessourceIntrouvableException e) {
        return reponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AccesInterditException.class)
    public ResponseEntity<ApiErreur> interdit(AccesInterditException e) {
        return reponse(HttpStatus.FORBIDDEN, e.getMessage());
    }

    /** Levée par @PreAuthorize("hasRole('ADMIN')") quand un simple utilisateur appelle une route 🔒. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErreur> accesRefuse(AccessDeniedException e) {
        return reponse(HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs");
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<ApiErreur> identifiants(IdentifiantsInvalidesException e) {
        return reponse(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(ConflitException.class)
    public ResponseEntity<ApiErreur> conflit(ConflitException e) {
        return reponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RequeteInvalideException.class)
    public ResponseEntity<ApiErreur> invalide(RequeteInvalideException e) {
        return reponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Erreurs de validation (@NotBlank, @Positive...) : on renvoie le premier message, déjà en français. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErreur> validation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erreur -> erreur.getDefaultMessage())
                .orElse("Données invalides");
        return reponse(HttpStatus.BAD_REQUEST, message);
    }

    /** JSON mal formé, date au mauvais format, valeur d'énumération inconnue... */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErreur> jsonIllisible(HttpMessageNotReadableException e) {
        return reponse(HttpStatus.BAD_REQUEST,
                "Corps de la requête invalide (format JSON, date AAAA-MM-JJ ou valeur inconnue)");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErreur> parametreInvalide(MethodArgumentTypeMismatchException e) {
        return reponse(HttpStatus.BAD_REQUEST, "Paramètre invalide : " + e.getName());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErreur> parametreManquant(MissingServletRequestParameterException e) {
        return reponse(HttpStatus.BAD_REQUEST, "Paramètre obligatoire manquant : " + e.getParameterName());
    }

    /** Contrainte Oracle violée (UNIQUE, clé étrangère...) qui n'a pas été vérifiée avant. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErreur> integrite(DataIntegrityViolationException e) {
        log.warn("Contrainte de base de données violée : {}", e.getMostSpecificCause().getMessage());
        return reponse(HttpStatus.CONFLICT, "Opération impossible : donnée en double ou encore utilisée");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErreur> routeInconnue(NoResourceFoundException e) {
        return reponse(HttpStatus.NOT_FOUND, "Route inconnue : /" + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErreur> methodeNonSupportee(HttpRequestMethodNotSupportedException e) {
        return reponse(HttpStatus.METHOD_NOT_ALLOWED, "Méthode HTTP non autorisée : " + e.getMethod());
    }

    /** Filet de sécurité : toute autre erreur est journalisée et renvoyée en 500 sans détail technique. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErreur> autre(Exception e) {
        log.error("Erreur inattendue", e);
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur");
    }

    private ResponseEntity<ApiErreur> reponse(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(new ApiErreur(statut.value(), message));
    }
}
