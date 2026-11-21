package tn.smartbuy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Connexion à Oracle et initialisation de la base, exécutées une fois au démarrage.
 * <p>
 * Étapes :
 * <ol>
 *   <li>ouvrir une connexion JDBC (paramètres dans application.yml : spring.datasource.*) et afficher
 *       la version d'Oracle — si cela échoue, l'application s'arrête avec un message clair ;</li>
 *   <li>regarder dans le dictionnaire Oracle (vue USER_TABLES) si la table UTILISATEUR existe ;</li>
 *   <li>si elle n'existe pas : exécuter db/schema.sql (tables) puis db/data.sql (données initiales) ;</li>
 *   <li>appliquer les évolutions du schéma, puis donner le rôle ADMIN à l'adresse configurée ({@link PromotionAdmin}).</li>
 * </ol>
 * Les mêmes scripts sont dans le dossier database/ pour être exécutés à la main dans SQL Developer.
 */
@Component
@Order(1)
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final PromotionAdmin promotionAdmin;
    private final boolean initialiser;

    public DatabaseInitializer(DataSource dataSource, PromotionAdmin promotionAdmin,
                               @Value("${smartbuy.db.initialiser:true}") boolean initialiser) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.promotionAdmin = promotionAdmin;
        this.initialiser = initialiser;
    }

    @Override
    public void run(ApplicationArguments args) {
        afficherConnexion();
        if (initialiser && !tableExiste("UTILISATEUR")) {
            creerSchemaEtDonnees();
        }
        // Évolution v2 du schéma : e-mails réels (codes de confirmation, plus de comptes fictifs).
        if (initialiser && !tableExiste("CODE_EMAIL")) {
            if (tableExiste("REINITIALISATION_MDP")) {
                jdbc.execute("DROP TABLE reinitialisation_mdp PURGE"); // remplacée par CODE_EMAIL
            }
            executer("db/migration_v2_email.sql");
            log.info("Schéma v2 : table CODE_EMAIL, colonne UTILISATEUR.EMAIL_VERIFIE, comptes fictifs supprimés.");
        }
        // Évolution v3 : univers de courses, vrais codes-barres, magasins en ligne (collecte des prix).
        if (initialiser && !colonneExiste("CATEGORIE_PRODUIT", "UNIVERS")) {
            executer("db/migration_v3_univers.sql");
            log.info("Schéma v3 : univers (alimentation, vêtements, high-tech), vrais codes-barres, magasins en ligne.");
        }
        if (initialiser) {
            executer("db/categories_revenus.sql"); // ajoute seulement les catégories de revenus manquantes
        }
        promotionAdmin.appliquer();
    }

    /** Étape 1 : DatabaseMetaData donne des informations sur la base à laquelle on est connecté. */
    private void afficherConnexion() {
        try (Connection connexion = dataSource.getConnection()) {
            DatabaseMetaData meta = connexion.getMetaData();
            log.info("Connecté à {} {}", meta.getDatabaseProductName(), meta.getDatabaseProductVersion().lines()
                    .findFirst().orElse(""));
            log.info("URL JDBC : {} | utilisateur : {} | pilote : {} {}",
                    meta.getURL(), meta.getUserName(), meta.getDriverName(), meta.getDriverVersion());
        } catch (SQLException e) {
            throw new IllegalStateException("Connexion à Oracle impossible (" + e.getMessage() + "). "
                    + "Vérifiez que le service Windows OracleServiceORCL est démarré, que l'utilisateur "
                    + "SMARTBUY existe (database/00_create_user.sql) et les variables "
                    + "DB_HOST, DB_PORT, DB_SERVICE, DB_USER, DB_PASSWORD.", e);
        }
    }

    /** Étape 2 : USER_TABLES liste les tables appartenant à l'utilisateur connecté (noms en MAJUSCULES). */
    private boolean tableExiste(String nomTable) {
        Integer nombre = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_tables WHERE table_name = ?", Integer.class, nomTable);
        return nombre != null && nombre > 0;
    }

    private boolean colonneExiste(String table, String colonne) {
        Integer nombre = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_tab_columns WHERE table_name = ? AND column_name = ?", Integer.class, table, colonne);
        return nombre != null && nombre > 0;
    }

    /** Étape 3 : exécution des scripts SQL, instruction par instruction (séparateur « ; »). */
    private void creerSchemaEtDonnees() {
        log.info("Table UTILISATEUR absente : création du schéma (db/schema.sql) et des données (db/data.sql)...");
        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator(
                new ClassPathResource("db/schema.sql"),
                new ClassPathResource("db/data.sql"));
        scripts.setSqlScriptEncoding("UTF-8"); // accents (Santé, Épicerie...)
        scripts.setSeparator(";");
        scripts.setContinueOnError(false);
        scripts.execute(dataSource);
        log.info("Schéma SmartBuy créé : 11 tables et données initiales.");
    }

    private void executer(String script) {
        ResourceDatabasePopulator populateur = new ResourceDatabasePopulator(new ClassPathResource(script));
        populateur.setSqlScriptEncoding("UTF-8");
        populateur.setSeparator(";");
        populateur.execute(dataSource);
    }
}
