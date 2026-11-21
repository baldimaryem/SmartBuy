# SmartBuy — API back-end (Spring Boot + Oracle)

API REST de l'application SmartBuy (Smart Budget + comparaison des prix).
Elle implémente exactement le contrat décrit dans [`docs/07-contrat-api.md`](../docs/07-contrat-api.md)
et l'architecture de [`docs/04-architecture-et-donnees.md`](../docs/04-architecture-et-donnees.md).

| Élément | Choix |
|---|---|
| Langage | Java 17 |
| Framework | Spring Boot 3.5.3 (web, data-jpa, validation, security, mail) |
| Base de données | Oracle Database 19c locale, service `orcl`, schéma `SMARTBUY` (pilote JDBC `ojdbc11`) |
| Authentification | JWT (`jjwt` 0.11.5) + mots de passe hachés avec BCrypt |
| E-mails | `JavaMailSender` via Gmail (SMTP `smtp.gmail.com:587`, STARTTLS) |
| IA (facultatif) | SDK Java officiel d'Anthropic (Claude), sinon moteur de règles local |
| Tests | JUnit 5 + Mockito + AssertJ (aucune base nécessaire) |

---

## 1. Prérequis

- **JDK 17** (vérifier avec `java -version` ; `JAVA_HOME` doit pointer vers le JDK 17).
- **Oracle Database 19c** installée sur le poste (service Windows `OracleServiceORCL` démarré),
  et **SQL Developer**.
- **Maven** : le *Maven Wrapper* (`mvnw.cmd`) télécharge Maven au premier lancement. Si Internet
  est filtré, utiliser directement un Maven déjà installé (voir §3).

---

## 2. Préparer la base Oracle (une seule fois)

1. Dans SQL Developer, ouvrir la connexion **SYS** sur `localhost:1521/orcl` (rôle SYSDBA).
2. Ouvrir [`database/00_create_user.sql`](../database/00_create_user.sql), tout sélectionner, **F5** (Run Script) :
   crée l'utilisateur `smartbuy` et ses droits. La première requête affiche le nom du serveur : vérifier que c'est le poste local.
3. Créer la connexion **SmartBuy** : `smartbuy` / `smartbuy`, hôte `localhost`, port `1521`, **Service name** `orcl`.
4. Les tables : rien à faire, l'API les crée à son premier démarrage. On peut aussi exécuter (F5)
   [`database/01_schema.sql`](../database/01_schema.sql) puis [`database/02_data.sql`](../database/02_data.sql) dans la connexion SmartBuy.

> Ne jamais utiliser une connexion vers une base client : SmartBuy n'utilise que `localhost`.
>
> Pour tout recommencer de zéro : supprimer l'utilisateur (`DROP USER smartbuy CASCADE;` en SYS),
> puis refaire l'étape 2.

**E-mails :** ils partent par Gmail, avec les identifiants de `config/application.yml` (voir plus bas,
« Envoyer les e-mails avec Gmail »). C'est facultatif : sans cette configuration, l'API fonctionne et les codes
de confirmation s'affichent dans sa console (on peut aussi mettre `MAIL_ENABLED=false`).

---

## 3. Lancer l'API

Dans une invite de commandes (cmd) :

```bat
cd /d C:\workspace\flutter_app\backend
set "JAVA_HOME=C:\Program Files\Java\jdk-17.0.8"
mvnw.cmd spring-boot:run
```

Si `mvnw.cmd` ne peut pas télécharger Maven (réseau filtré), remplacer la dernière ligne par le Maven déjà présent :
`"%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.10-bin\53h08a94dg6djh6umvruv7q564\apache-maven-3.9.10\bin\mvn.cmd" spring-boot:run`

Au démarrage, le journal affiche :

```
Connecté à Oracle Oracle Database 19c Enterprise Edition Release 19.0.0.0.0 - Production
URL JDBC : jdbc:oracle:thin:@//localhost:1521/orcl | utilisateur : SMARTBUY | ...
Table UTILISATEUR absente : création du schéma (db/schema.sql) et des données (db/data.sql)...
Schéma v2 : table CODE_EMAIL, colonne UTILISATEUR.EMAIL_VERIFIE, comptes fictifs supprimés.
Started SmartBuyApplication in ... seconds
```

L'API répond sur <http://localhost:8080/api> (émulateur Android : `http://10.0.2.2:8080/api`).

Autres commandes utiles :

```bash
mvnw test                     # tests unitaires (sans Oracle)
mvnw -DskipTests package      # crée target/smartbuy-api-1.0.0.jar
java -jar target/smartbuy-api-1.0.0.jar
```

### Comptes : e-mails réels uniquement

Aucun compte fictif. Pour s'inscrire :
1. le **domaine** de l'adresse doit recevoir des e-mails (enregistrement DNS MX) : `@gmail.com` est accepté, `@smartbuy.tn` refusé ;
2. un **code à 6 chiffres** est envoyé à l'adresse ; sans ce code, la connexion est refusée (403).

### Envoyer les e-mails avec Gmail (une seule fois)

1. Activer la **validation en 2 étapes** sur le compte Google : <https://myaccount.google.com/security>.
2. Créer un **mot de passe d'application** : <https://myaccount.google.com/apppasswords>, nom « SmartBuy », puis copier les 16 lettres.
3. Copier `backend/config/application.yml.exemple` en `backend/config/application.yml` et y mettre :
   l'adresse Gmail (`spring.mail.username` et `smartbuy.mail.from`), le mot de passe d'application (`spring.mail.password`)
   et l'adresse de l'administrateur (`smartbuy.admin.email`).
4. Relancer l'API (`demarrer-api.bat`).

Ce fichier est exclu de Git : le mot de passe n'est jamais dans le code. Tant qu'il n'est pas configuré,
les codes s'affichent dans le terminal de l'API (ligne `>>> CODE ...`).

---

## 4. Variables d'environnement

Toutes ont une valeur par défaut adaptée au développement (voir `src/main/resources/application.yml`).

| Variable | Défaut | Rôle |
|---|---|---|
| `DB_HOST` | `localhost` | Hôte Oracle |
| `DB_PORT` | `1521` | Port Oracle |
| `DB_SERVICE` | `orcl` | Nom de service de la base Oracle 19c locale |
| `DB_USER` | `smartbuy` | Utilisateur Oracle |
| `DB_PASSWORD` | `smartbuy` | Mot de passe Oracle |
| `DB_INIT` | `true` | Créer tables + données au démarrage si la table `UTILISATEUR` n'existe pas |
| `MAIL_HOST` | `smtp.gmail.com` | Serveur SMTP |
| `MAIL_PORT` | `587` | Port SMTP (STARTTLS) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | *(vide)* | Adresse Gmail et mot de passe d'application (de préférence dans `config/application.yml`) |
| `MAIL_ENABLED` | `true` | `false` = aucun e-mail envoyé (seulement journalisé) |
| `JWT_SECRET` | clé de développement | Propriété `smartbuy.jwt.secret` : clé de signature des jetons (≥ 32 caractères). **À changer en production.** |
| `ANTHROPIC_API_KEY` | *(vide)* | Clé API Claude. Vide = conseils et assistant produits par le moteur de règles |
| `ANTHROPIC_MODEL` | `claude-opus-5-5` | Propriété `smartbuy.ia.model` : modèle Claude utilisé |
| `ANTHROPIC_FALLBACK` | `true` | Repli automatique côté Anthropic si le modèle refuse une requête |

Exemple (PowerShell) : `$env:ANTHROPIC_API_KEY = "sk-ant-..."; .\mvnw spring-boot:run`

> La clé API n'est **jamais** écrite dans le code ni dans `application.yml` : seulement dans l'environnement.
> Seul un **résumé chiffré et anonyme** du mois est envoyé à Claude (montants, noms de catégories),
> jamais le nom ni l'e-mail de l'utilisateur.

---

## 5. Organisation du code

Architecture en couches **Controller → Service → Repository → Oracle**, classes regroupées par module :

```
src/main/java/tn/smartbuy/
├── SmartBuyApplication.java   point d'entrée (+ tâches planifiées, envoi asynchrone)
├── config/      connexion et initialisation d'Oracle, comptes démo, sécurité
├── securite/    jetons JWT (création, vérification, filtre)
├── commun/      erreurs { status, message }, convertisseurs Oracle, utilitaires
├── auth/        comptes : inscription, connexion, profil          (AUTH-01..06)
├── budget/      catégories, opérations, budgets, tableau de bord  (BUD-01..15)
├── alerte/      alertes 80 % / 100 %                               (BUD-11)
├── prix/        magasins, produits, prix, propositions            (PRX-01..08)
├── admin/       routes 🔒 ADMIN : catalogue et validation des prix (ADM-01..04, PRX-08)
├── liste/       listes de courses, comparaison, achat             (PRX-09..13, LNK-01..03)
├── ia/          conseils et assistant (règles ou Claude)          (BUD-16)
├── email/       envoi des e-mails
└── rapport/     bilan mensuel par e-mail
src/main/resources/
├── application.yml            configuration (connexion Oracle visible ici)
└── db/schema.sql, db/data.sql scripts exécutés au premier démarrage
```

### Pourquoi ces couches ?

| Couche | Exemple | Pourquoi ? |
|---|---|---|
| **Controller** | `OperationController` | Reçoit la requête HTTP et renvoie du JSON. Il ne contient **aucune règle métier** : on peut changer d'interface (web, mobile) sans toucher aux calculs. |
| **Service** | `OperationService`, `ListeService` | Contient les **règles métier** (vérifier que l'opération appartient à l'utilisateur, déclencher les alertes...). Un seul endroit à tester et à corriger. Les méthodes portent l'identifiant du besoin (ex. `BUD-11`). |
| **Calcul « pur »** | `ComparateurPrix`, `CalculsBudget`, `SeuilsAlerte`, `PrixActuels` | Les algorithmes importants sont dans de petites classes **sans Spring ni base de données** : on les teste avec de simples objets Java. |
| **Repository** | `OperationRepository` | Accès à Oracle via JPA / Hibernate. Spring Data écrit le SQL à partir du nom des méthodes (`findByUtilisateurIdAndMois`...). |
| **Entité** | `Operation`, `Prix` | Une classe Java = une table Oracle (`@Table`, `@Column`). |
| **DTO** (`record`) | `BudgetDtos.OperationDto` | Objets JSON échangés avec Flutter, avec **exactement** les noms du contrat. On ne renvoie jamais une entité (pas de mot de passe haché dans le JSON !). |
| **GestionnaireErreurs** | `@RestControllerAdvice` | Transforme toutes les exceptions en `{ "status": 404, "message": "Produit introuvable" }`. |

### Points de conception à expliquer à l'oral

- **Pas de `ddl-auto` Hibernate** : le schéma est écrit en SQL Oracle (`NUMBER(12,3)`, `CHAR(1)` + `CHECK`,
  `IDENTITY`, index). Hibernate ne crée ni ne modifie jamais les tables.
- **Booléens Oracle** : `OuiNonConverter` traduit `true/false` ↔ `'O'/'N'`.
- **Prix actuel** = dernier relevé `VALIDE` par (produit, magasin) : `PrixActuels`.
- **Alertes** : une seule alerte par seuil et par budget (`ALERTE.BUDGET_ID`), l'e-mail part en arrière-plan
  (`@Async`) et une panne SMTP ne fait jamais échouer la requête.
- **IA** : interface `ConseillerBudget` + deux implémentations (`ConseillerRegles`, `ConseillerClaude`).
  Si Claude n'est pas configuré ou échoue, `IaService` répond avec les règles (`source: "REGLES"`).
- **Tâches planifiées** : `RecurrenceTache` (le 1ᵉʳ du mois à 00h05, BUD-07) et `RapportTache`
  (le 1ᵉʳ du mois à 08h00, bilan du mois précédent).

---

## 6. Essayer l'API avec curl

```bash
# 1) Connexion : récupérer le jeton
curl -s -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"votre.adresse@gmail.com","motDePasse":"votre-mot-de-passe"}'
# -> {"token":"eyJhbGciOi...","utilisateur":{"id":2,"nom":"Sarra",...}}

# 2) Appel authentifié : tableau de bord du mois
TOKEN="eyJhbGciOi..."   # coller le jeton reçu
curl -s http://localhost:8080/api/dashboard?mois=2026-11 \
     -H "Authorization: Bearer $TOKEN"

# 3) Comparer la liste de courses de démonstration
curl -s http://localhost:8080/api/listes/1/comparaison -H "Authorization: Bearer $TOKEN"
```

Sous PowerShell :

```powershell
$r = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
     -ContentType "application/json" -Body '{"email":"votre.adresse@gmail.com","motDePasse":"votre-mot-de-passe"}'
Invoke-RestMethod -Uri http://localhost:8080/api/dashboard -Headers @{ Authorization = "Bearer $($r.token)" }
```

Sans jeton, toute route (sauf `/auth/register` et `/auth/login`) répond `401` ;
une route 🔒 appelée par un utilisateur non administrateur répond `403`.

---

## 7. Tests

```bash
mvnw test
```

Les tests n'ont pas besoin d'Oracle :

| Classe de test | Ce qui est vérifié |
|---|---|
| `ComparateurPrixTest` | totaux par magasin, magasins incomplets (PRX-13), classement, magasin recommandé, économie, dépassement du budget (LNK-01), économie d'un achat (LNK-03) |
| `CalculsBudgetTest` | prévision de fin de mois (BUD-15), répartition (BUD-13), évolution sur 6 mois (BUD-14) |
| `SeuilsAlerteTest`, `AlerteServiceTest` | alertes 80 % / 100 % sans doublon, e-mail envoyé (BUD-11, avec Mockito) |
| `ConseillerReglesTest`, `IaServiceTest` | règles de conseils (BUD-16), repli sur les règles si l'IA échoue |
| `JwtServiceTest` | aller-retour du jeton, jeton falsifié ou expiré |
| `PrixActuelsTest` | dernier prix validé, magasin le plus souvent le moins cher |
| `SecuriteWebTest` | 401 sans jeton, 403 pour un USER sur une route 🔒, 400 avec message en français |
