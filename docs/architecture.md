# Architecture du projet SmartBuy

SmartBuy aide à gérer son budget et à comparer les prix. Le projet suit une **architecture 3-tiers** : chaque couche a son rôle et ne parle qu'à sa voisine.

```
┌──────────────────────┐   HTTP / JSON + JWT   ┌──────────────────────┐        JDBC        ┌──────────────────────┐
│  Application mobile  │ ────────────────────▶ │   API REST back-end  │ ─────────────────▶ │   Base de données    │
│  Flutter   (lib/)    │ ◀──────────────────── │  Spring Boot 3.5     │ ◀───────────────── │   Oracle 19c         │
│  Présentation        │                       │  Java 17 (backend/)  │                    │   schéma SMARTBUY    │
└──────────────────────┘                       └──────────┬───────────┘                    │   (database/)        │
                                                          │                                └──────────────────────┘
                              ┌───────────────────────────┼────────────────────────────┐
                              │ SMTP (STARTTLS, port 587) │ HTTPS (robots.txt, 1 req/s)│
                              ▼                           ▼                            ▼
                     Gmail (smtp.gmail.com)     Open Food Facts · carrefour.tn · spacenet.tn
                     codes, alertes, bilan      codes-barres et prix réels collectés
```

L'application mobile ne se connecte **jamais** directement à Oracle :
- les identifiants de la base restent sur le serveur, sinon on pourrait les lire dans l'APK ;
- toutes les règles métier (budget, alertes, comparaison) sont calculées au même endroit ;
- il n'existe pas de pilote Oracle fiable pour une application mobile.

---

## 1. Front-end Flutter : `lib/`

Le code est **organisé par fonctionnalité** (*feature-first*), et chaque fonctionnalité a trois sous-couches : `data`, `logic` et `presentation`.

```
lib/
├── main.dart                      → point d'entrée (runApp)
├── app.dart                       → MaterialApp.router + MultiProvider (contrôleurs d'état)
├── core/                          → socle technique, commun à toute l'app
│   ├── config/app_config.dart       → USE_MOCK, API_URL (via --dart-define), 10.0.2.2 sur Android
│   ├── di/dependances.dart          → injection : dépôts Mock ou Api
│   ├── network/                     → ApiClient (http + JSON + jeton JWT), ApiException
│   ├── router/app_router.dart       → toutes les routes (go_router) + garde de connexion
│   └── theme/app_theme.dart         → thème Material 3, couleurs, icônes
├── features/
│   ├── auth/      → connexion, inscription, code e-mail, mot de passe oublié, profil
│   ├── budget/    → accueil, opérations, budgets, statistiques, alertes, catégories
│   ├── prix/      → univers, code-barres, catalogue, produit, magasins, listes, comparaison
│   ├── ia/        → conseils et assistant
│   └── admin/     → prix à valider, magasins, produits, rayons, collecte
└── shared/
    ├── mock/mock_store.dart       → données simulées (mode maquette, suivi 1)
    ├── utils/formatters.dart      → montants en dinars (3 décimales), dates en français
    └── widgets/                   → barre de navigation (coquille), graphiques, cadre téléphone, composants communs
```

### Les trois couches d'une fonctionnalité (exemple : `features/budget/`)

| Couche | Fichiers | Rôle |
|---|---|---|
| **data** | `budget_models.dart`, `budget_repository.dart` | Modèles (`fromJson` / `toJson`) et dépôt : une interface `BudgetRepository` avec deux versions, `ApiBudgetRepository` (HTTP) et `MockBudgetRepository` (mémoire) |
| **logic** | `budget_controller.dart`, `budget_calculs.dart` | État partagé (`ChangeNotifier`) et règles pures (prévision, seuils d'alerte) |
| **presentation** | `accueil_screen.dart`, `operations_screen.dart`… | Écrans : ils lisent l'état avec `context.watch` et agissent avec `context.read` |

### Le chemin d'une donnée

```
Écran ──watch / read──▶ Contrôleur (ChangeNotifier) ──▶ Repository (interface)
  ▲                            │ notifyListeners()        ├── ApiRepository  ──▶ ApiClient ──▶ GET /api/…
  └──── se redessine ◀─────────┘                          └── MockRepository ──▶ MockStore (mémoire)
```

### Choix techniques

| Sujet | Choix | Pourquoi |
|---|---|---|
| Gestion d'état | **Provider + ChangeNotifier** | Recommandé par Flutter pour débuter, simple à expliquer |
| Navigation | **go_router** : routes déclarées dans un seul fichier, 5 onglets (`StatefulShellRoute`) | Garde de connexion, une URL par écran |
| HTTP | Paquet **http**, un seul `ApiClient` | JSON, jeton et erreurs gérés au même endroit |
| Maquette ou API | `--dart-define=USE_MOCK=true` | Démo sans serveur, sans changer les écrans |
| Graphiques | `CustomPainter` | Pas de dépendance supplémentaire |

---

## 2. Back-end Spring Boot : `backend/`

Une API REST en couches **Controller → Service → Repository → Oracle**, regroupée par module.

```
backend/src/main/java/tn/smartbuy/
├── SmartBuyApplication.java   → démarrage (@EnableScheduling, @EnableAsync)
├── config/      → connexion et initialisation d'Oracle, sécurité, attribution du rôle administrateur
├── securite/    → JWT (création et filtre de vérification)
├── commun/      → erreurs JSON {status, message}, convertisseurs (booléens O/N, mois)
├── auth/        → inscription, code e-mail, connexion, mot de passe oublié, domaine e-mail réel
├── budget/      → catégories, opérations, budgets, tableau de bord, opérations récurrentes
├── alerte/      → alertes 80 % / 100 %
├── prix/        → magasins, rayons, produits, prix, univers
├── liste/       → listes de courses, comparaison (ComparateurPrix)
├── admin/       → routes réservées à l'administrateur
├── collecte/    → code-barres « intelligent » et collecte des prix en ligne
├── ia/          → conseils (moteur de règles + Claude en option)
├── email/       → envoi par Gmail (SMTP)
└── rapport/     → bilan mensuel par e-mail (planifié le 1er du mois)
```

| Couche | Rôle | Exemple |
|---|---|---|
| **Controller** | reçoit la requête HTTP, renvoie du JSON | `budget/OperationController.java` (`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`) |
| **Service** | règles métier, droits, transactions | `budget/OperationService.java` |
| **Repository** | accès à Oracle sans SQL écrit à la main | `budget/OperationRepository.java` (`JpaRepository`) |
| **Entity** | une classe = une table | `budget/Operation.java` → `OPERATION` |
| **DTO** | objet JSON échangé, sans le mot de passe | `budget/BudgetDtos.java` (records) |

**Sécurité :**
- mots de passe hachés avec BCrypt ;
- jeton **JWT** à chaque requête, vérifié par `JwtFiltre` ;
- routes publiques déclarées dans `SecurityConfig` ;
- `@PreAuthorize("hasRole('ADMIN')")` pour l'administration.

**Configuration :**
- `src/main/resources/application.yml` : Oracle, Gmail, JWT et collecte, sans aucun secret ;
- `config/application.yml` : fichier local, exclu de Git, qui contient le mot de passe d'application Gmail et l'adresse de l'administrateur.

**Le contrat de toutes les routes** est dans `docs/07-contrat-api.md`.

---

## 3. Base de données Oracle

| Élément | Où |
|---|---|
| Connexion | `backend/src/main/resources/application.yml` → `jdbc:oracle:thin:@//localhost:1521/orcl`, utilisateur `smartbuy` |
| Création automatique des tables | `config/DatabaseInitializer.java` : vérifie `USER_TABLES`, puis exécute `db/schema.sql` et `db/data.sql` |
| Évolutions du schéma | `db/migration_v2_email.sql` (codes e-mail), `db/migration_v3_univers.sql` (univers, magasins en ligne) |
| Scripts manuels (SQL Developer) | `database/00_create_user.sql` … `05_evolution_univers.sql` |

**Les 12 tables :**

```
UTILISATEUR ──< CODE_EMAIL                         (codes à 6 chiffres, hachés)
UTILISATEUR ──< OPERATION >── CATEGORIE            (revenus / dépenses)
UTILISATEUR ──< BUDGET >── CATEGORIE               (plafonds mensuels)
UTILISATEUR ──< ALERTE                             (80 % / 100 %)
UTILISATEUR ──< LISTE_COURSES ──< LIGNE_LISTE >── PRODUIT
PRODUIT >── CATEGORIE_PRODUIT                      (rayon + univers : Alimentation, Vêtements, High-tech)
PRODUIT ──< PRIX >── MAGASIN                       (un prix = produit × magasin × date)
OPERATION >── MAGASIN                              (lien entre les deux modules)
```

Choix propres à Oracle :
- `NUMBER(12,3)` pour les montants (le dinar compte des millimes) ;
- `GENERATED BY DEFAULT AS IDENTITY` pour les identifiants ;
- `CHAR(1)` avec `CHECK ('O','N')` pour les booléens ;
- contraintes `CHECK`, `UNIQUE` (e-mail, code-barres) et clés étrangères `ON DELETE CASCADE` ;
- la table s'appelle `OPERATION`, car `TRANSACTION` est un mot réservé du SQL.

---

## 4. Services externes

| Service | Usage | Règle respectée |
|---|---|---|
| **Gmail** (`smtp.gmail.com:587`) | Codes de confirmation, mot de passe oublié, alertes, bilan mensuel | Mot de passe d'application dans un fichier local ; test de connexion au démarrage |
| **Open Food Facts** | Nom et marque d'un code-barres inconnu, ajouté au catalogue | Pages produit publiques autorisées par le `robots.txt` |
| **Carrefour Tunisie** (carrefour.tn) | Prix réels de l'alimentation : référence produit = code-barres EAN-13 | API publique du site, autorisée |
| **Spacenet** (spacenet.tn) | Prix réels high-tech (smartphones, PC) | Pages catégories seulement, car la recherche est interdite par son `robots.txt` |

La collecte passe toujours par `collecte/ClientWebPoli.java` : lecture du `robots.txt`, robot identifié (`SmartBuy-etudiant`), une requête par seconde au plus. Jumia et Tunisianet sont exclus, car ils interdisent les robots.

---

## 5. Exemple de flux complet : comparer une liste de courses

1. L'utilisateur ouvre sa liste « Courses » dans l'onglet **Listes**.
2. Flutter envoie `GET /api/listes/12/comparaison` avec son jeton JWT (`ApiClient`).
3. Spring Boot vérifie le jeton (`JwtFiltre`), puis que la liste lui appartient (`ListeService`).
4. Le service lit dans Oracle les lignes de la liste et le dernier prix validé de chaque produit, dans chaque magasin.
5. `ComparateurPrix` calcule le total par magasin, repère les produits manquants, ignore les magasins sans aucun article et recommande le moins cher parmi les magasins complets.
6. Le total est comparé au budget restant de la catégorie de l'univers (Alimentation, Vêtements ou High-tech).
7. La réponse JSON contient le classement, le magasin recommandé, l'économie et l'éventuel dépassement de budget.
8. Flutter affiche le classement. « J'ai acheté ici » crée la dépense et enregistre l'économie réalisée.

---

## 6. Tests

| Côté | Tests | Commande |
|---|---|---|
| Flutter | 31 (`test/`) : calculs, comparateur, scénarios, écrans | `flutter test` |
| Back-end | 66 (`backend/src/test/java/`) : comparaison, budget, alertes, JWT, codes e-mail, inscription, collecte, sécurité | `mvn test` |

Pour aller plus loin :
- `reflexion.md` : où chaque notion est déclarée dans le code ;
- `docs/guide-utilisation.md` : l'application étape par étape.
