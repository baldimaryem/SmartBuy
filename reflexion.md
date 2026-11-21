# SmartBuy : réflexion et guide du projet

> Ce document répond à trois questions :
> 1. **Qu'avons-nous construit, et pourquoi de cette façon ?**
> 2. **Où se trouve chaque notion dans le code** (API REST CRUD, gestion d'état, base de données, navigation, formulaires…) ?
> 3. **Qu'avons-nous appris, et quelles sont les limites ?**
>
> Les chemins sont relatifs au dossier `C:\workspace\flutter_app\`. Le numéro après `:` est la ligne où commence la déclaration.

---

## 1. Le projet en une phrase

SmartBuy réunit deux besoins habituellement séparés, **suivre son budget** et **acheter au meilleur prix** :
« je prépare ma liste de courses → l'appli trouve le magasin le moins cher → elle vérifie que ça rentre dans mon budget → j'enregistre la dépense et l'économie réalisée ».

| Module | Fonctions principales |
|---|---|
| **Smart Budget** | revenus, dépenses (récurrentes), budgets par catégorie, alertes 80 % / 100 %, prévision de fin de mois, statistiques, assistant |
| **Comparaison des prix** | 3 univers (Alimentation, Vêtements, High-tech), recherche et **code-barres réel**, prix par magasin, listes de courses, comparaison, achat → dépense |
| **Transverse** | comptes avec **e-mail confirmé par code**, mot de passe oublié, administrateur, **collecte des prix en ligne** (Carrefour Tunisie, Spacenet) |

---

## 2. L'architecture

### 2.1 Vue d'ensemble : 3 tiers

```
┌───────────────────────┐   HTTP + JSON   ┌────────────────────────┐   JDBC (ojdbc11)   ┌──────────────────────┐
│  Application Flutter  │ ──────────────▶ │  API REST Spring Boot  │ ─────────────────▶ │  Oracle 19c (orcl)   │
│  (présentation)       │ ◀────────────── │  (métier + sécurité)   │ ◀───────────────── │  schéma SMARTBUY     │
│  lib/                 │   jeton JWT     │  backend/              │                    │  12 tables           │
└───────────────────────┘                 └───────────┬────────────┘                    └──────────────────────┘
                                                      │ collecte (robots.txt respecté, 1 requête/s)
                                                      ▼
                                 Open Food Facts · carrefour.tn (GraphQL) · spacenet.tn (HTML)
```

**Pourquoi Flutter ne parle pas directement à Oracle ?**
1. **Sécurité** : les identifiants de la base seraient lisibles dans l'APK, et n'importe qui accéderait aux données de tous.
2. **Règles métier au même endroit** : budget, alertes et comparaison sont calculés une seule fois, par le serveur.
3. **Technique** : il n'existe pas de pilote Oracle fiable pour Flutter. Les pilotes Oracle sont faits pour les serveurs (Java/JDBC).

### 2.2 Application Flutter : feature-first avec 3 couches par module

```
lib/
├── main.dart                     point d'entrée (runApp)
├── app.dart                      MaterialApp.router + MultiProvider (injection des contrôleurs)
├── core/                         ce qui sert à toute l'application
│   ├── config/app_config.dart       USE_MOCK, URL de l'API
│   ├── di/dependances.dart          choisit les repositories Mock ou Api
│   ├── network/api_client.dart      client HTTP unique (JSON, jeton, erreurs)
│   ├── router/app_router.dart       toutes les routes (go_router) + garde de connexion
│   └── theme/app_theme.dart         couleurs, Material 3, icônes de catégories
├── features/                     un dossier par fonctionnalité du cahier des charges
│   ├── auth/      budget/      prix/      ia/      admin/
│   │   ├── data/          modèles (fromJson/toJson) + repository (interface, Api, Mock)
│   │   ├── logic/         contrôleurs d'état (ChangeNotifier) + règles pures (calculs)
│   │   └── presentation/  écrans (widgets)
└── shared/                       widgets communs, graphiques, formats, données simulées
```

**Layer-first ou feature-first ?** En *layer-first*, on aurait `models/`, `screens/` et `services/` à la racine : une fonctionnalité serait éparpillée dans trois dossiers. Nous avons choisi le **feature-first**, où tout ce qui concerne « budget » est dans `features/budget/`. **À l'intérieur** de chaque module, on garde quand même trois couches : on profite ainsi des deux approches.

**Le chemin d'une donnée** (exemple : afficher les opérations) :

```
Écran (presentation)  ──watch/read──▶  BudgetController (logic)  ──▶  BudgetRepository (data, interface)
                                                                      ├── ApiBudgetRepository  → ApiClient → GET /api/operations
                                                                      └── MockBudgetRepository → MockStore (mémoire)
```

### 2.3 API Spring Boot : couches Controller → Service → Repository

```
backend/src/main/java/tn/smartbuy/
├── auth/        comptes, JWT, confirmation d'e-mail, mot de passe oublié
├── budget/      catégories, opérations, budgets, tableau de bord
├── alerte/      alertes 80 % / 100 %
├── prix/        magasins, rayons, produits, prix
├── liste/       listes de courses et comparaison
├── admin/       routes réservées à l'administrateur
├── collecte/    code-barres « intelligent » et collecte des prix en ligne (web scraping)
├── ia/          conseils (règles + Claude en option)
├── email/       envoi des e-mails
├── rapport/     bilan mensuel
├── securite/    filtre JWT
├── commun/      erreurs, convertisseurs
└── config/      connexion Oracle, sécurité, administrateur
```

| Couche | Rôle | Exemple |
|---|---|---|
| **Controller** | reçoit la requête HTTP, renvoie du JSON | `budget/OperationController.java` |
| **Service** | règles métier, transactions | `budget/OperationService.java` |
| **Repository** | lit et écrit dans Oracle (Spring Data JPA) | `budget/OperationRepository.java` |
| **Entity** | une classe = une table | `budget/Operation.java` → table `OPERATION` |
| **DTO (record)** | objet JSON échangé (le mot de passe n'en sort jamais) | `budget/BudgetDtos.java` |

---

## 3. Où est déclaré quoi ? (antisèche pour l'oral)

### 3.1 API REST et CRUD

**Le contrat** (toutes les routes, tous les champs) : `docs/07-contrat-api.md`.

**Exemple complet : CRUD des opérations**

| Opération | Côté serveur (Spring Boot) | Côté Flutter |
|---|---|---|
| **C**reate | `@PostMapping` dans `backend/.../budget/OperationController.java:43` | `_api.post('/operations', …)` dans `lib/features/budget/data/budget_repository.dart:58` |
| **R**ead | `@GetMapping` dans `OperationController.java:34` | `_api.get('/operations', query: …)` dans `budget_repository.dart:52` |
| **U**pdate | `@PutMapping("/{id}")` dans `OperationController.java:50` | `_api.put('/operations/$id', …)` dans `budget_repository.dart:59` |
| **D**elete | `@DeleteMapping("/{id}")` dans `OperationController.java:56` | `_api.delete('/operations/$id')` dans `budget_repository.dart:64` |

Les autres CRUD suivent le même modèle :

| Ressource | Contrôleur serveur | Repository Flutter |
|---|---|---|
| Budgets | `budget/BudgetController.java` | `lib/features/budget/data/budget_repository.dart` |
| Catégories | `budget/CategorieController.java` | idem |
| Listes et lignes | `liste/ListeController.java` | `lib/features/prix/data/prix_repository.dart` |
| Magasins, produits, rayons (admin) | `admin/CatalogueAdminController.java` | idem |
| Prix (proposer, valider) | `prix/PrixController.java`, `admin/PrixAdminController.java` | idem |
| Code-barres, collecte | `collecte/CollecteController.java` | idem |

**Les briques HTTP côté Flutter** (cours S05) :

| Notion du cours | Où |
|---|---|
| paquet `http` déclaré | `pubspec.yaml` (`http: ^1.6.0`) |
| client unique, méthodes `get/post/put/delete` | `lib/core/network/api_client.dart:18` |
| construction de l'`Uri` + paramètres | `api_client.dart:50` (`Uri.parse(...).replace(queryParameters: …)`) |
| `json.encode` (sérialisation) | `api_client.dart:56` (`jsonEncode(body)`) |
| `json.decode` (désérialisation) | `api_client.dart:69` (`jsonDecode(texte)`) |
| JSON vers objet Dart (`fromJson`) | ex. `lib/features/budget/data/budget_models.dart:71` (`Operation.fromJson`) |
| objet Dart vers JSON (`toJson`) | ex. `budget_models.dart:86` (`toRequete()`) |
| en-tête `Authorization: Bearer <jeton>` | `api_client.dart`, dans `_send` |
| URL de l'API (`10.0.2.2` sur l'émulateur Android) | `lib/core/config/app_config.dart` |

**Sécurité de l'API :**

| Élément | Où |
|---|---|
| Routes publiques / protégées | `backend/.../config/SecurityConfig.java:42` |
| Lecture du jeton à chaque requête | `securite/JwtFiltre.java:34` |
| Création du jeton | `securite/JwtService.java:40` |
| Erreurs JSON `{status, message}` | `commun/GestionnaireErreurs.java:24` |
| Routes admin | `@PreAuthorize("hasRole('ADMIN')")` dans `admin/*.java` |

### 3.2 Gestion d'état (state management) : Provider + ChangeNotifier

| Élément | Où | Rôle |
|---|---|---|
| Déclaration des « fournisseurs » | `lib/app.dart:58` (`MultiProvider`, `ChangeNotifierProvider.value`) | rend les contrôleurs accessibles à tous les écrans |
| Contrôleur de session | `lib/features/auth/logic/auth_controller.dart:11` | qui est connecté |
| Contrôleur du budget | `lib/features/budget/logic/budget_controller.dart:13` | mois, tableau de bord, opérations, budgets, alertes |
| Contrôleurs du catalogue et des listes | `lib/features/prix/logic/catalogue_controller.dart`, `listes_controller.dart` | univers, recherche, listes |
| `notifyListeners()` | dans chaque contrôleur, ex. `budget_controller.dart:34` (`charger()`) | prévient les écrans que l'état a changé |
| Lire l'état et se redessiner | `context.watch<BudgetController>()` dans `lib/features/budget/presentation/accueil_screen.dart:33` | |
| Déclencher une action | `context.read<BudgetController>()` dans `operations_screen.dart:23` | |
| Le routeur écoute la session | `refreshListenable: auth` dans `lib/core/router/app_router.dart:36` | redirection automatique à la connexion ou à la déconnexion |

**Pourquoi Provider ?** C'est la solution recommandée par la documentation Flutter pour débuter. Elle est peu verbeuse et s'explique facilement. BLoC ou Riverpod seraient pertinents pour une application plus grande.

**État local ou état partagé ?**
- L'état **d'un seul écran** (le formulaire en cours, « mot de passe masqué ») vit dans un `StatefulWidget` avec `setState`.
- L'état **partagé** par plusieurs écrans (l'utilisateur connecté, le budget du mois) vit dans un `ChangeNotifier`.

### 3.3 Base de données Oracle

| Élément | Où |
|---|---|
| **Connexion** (URL JDBC, utilisateur, mot de passe) | `backend/src/main/resources/application.yml:18` → `jdbc:oracle:thin:@//localhost:1521/orcl`, `smartbuy` / `smartbuy` |
| Pilote Oracle | `backend/pom.xml` (`ojdbc11`) |
| Affiche la version d'Oracle au démarrage | `backend/.../config/DatabaseInitializer.java:77` |
| Crée les tables si elles n'existent pas (`USER_TABLES`) | `DatabaseInitializer.java:95` |
| Évolutions du schéma (v2 e-mails, v3 univers) | `DatabaseInitializer.java:67` et `resources/db/migration_v*.sql` |
| **Création des tables** (DDL) | `backend/src/main/resources/db/schema.sql`, copié dans `database/01_schema.sql` |
| Données de départ | `resources/db/data.sql`, `database/02_data.sql` |
| Création de l'utilisateur SMARTBUY | `database/00_create_user.sql` (à exécuter en SYS dans SQL Developer) |
| Une table = une classe `@Entity` | ex. `budget/Operation.java:25` (`@Table(name = "OPERATION")`) |
| Accès aux données sans SQL écrit à la main | ex. `budget/OperationRepository.java:11` (`JpaRepository`) |
| Requête personnalisée (JPQL) | `prix/ProduitRepository.java` (`@Query` de la recherche) |

**Les 12 tables :**

| Table | Contenu |
|---|---|
| `UTILISATEUR` | comptes (rôle, e-mail confirmé, mot de passe haché) |
| `CODE_EMAIL` | codes à 6 chiffres envoyés par e-mail, stockés hachés |
| `CATEGORIE` | catégories de revenus et de dépenses |
| `OPERATION` | revenus et dépenses (le nom `TRANSACTION` est un mot réservé SQL) |
| `BUDGET` | plafonds mensuels |
| `ALERTE` | alertes 80 % / 100 % |
| `MAGASIN` | magasins physiques et en ligne |
| `CATEGORIE_PRODUIT` | rayons, avec leur univers |
| `PRODUIT` | produits (code-barres, source) |
| `PRIX` | un prix = un produit × un magasin × une date |
| `LISTE_COURSES` | listes de courses (univers) |
| `LIGNE_LISTE` | produits d'une liste et leurs quantités |

**Choix propres à Oracle :**
- `NUMBER(12,3)` pour les montants (le dinar compte des millimes) ;
- `GENERATED BY DEFAULT AS IDENTITY` pour les identifiants ;
- `CHAR(1)` avec `CHECK ('O','N')` pour les booléens (pas de type booléen avant Oracle 23ai) ;
- contraintes `CHECK`, `UNIQUE` (e-mail, code-barres) et clés étrangères `ON DELETE CASCADE`.

### 3.4 Navigation et routes (cours S04)

Le cours présente `Navigator` : `push`, `pop`, routes nommées et passage d'arguments. Nous utilisons **go_router**, la solution recommandée aujourd'hui par l'équipe Flutter. Elle repose sur le même `Navigator`, mais déclare toutes les routes au même endroit.

| Notion du cours | Équivalent dans SmartBuy | Où |
|---|---|---|
| table des routes nommées (`routes:`) | `GoRouter(routes: [...])` | `lib/core/router/app_router.dart:36` |
| `Navigator.pushNamed('/x')` | `context.push('/profil')` | `accueil_screen.dart:50` |
| `Navigator.pushReplacementNamed` | `context.go('/verification-email', extra: adresse)` (remplace l'écran, avec un paramètre) | `inscription_screen.dart:27` |
| `Navigator.pop()` | `context.pop()` | `operation_form_screen.dart:88` |
| passage de paramètres (`arguments`) | paramètre d'URL `produit/:id` et `extra` | `app_router.dart:100` |
| `BottomNavigationBar` | `NavigationBar` (version Material 3) + `StatefulShellRoute` | `lib/shared/widgets/coquille.dart:12`, `app_router.dart:61` |
| `TabBar` | onglets de l'admin et des catégories | `lib/features/admin/presentation/admin_screen.dart:22` |
| `GestureDetector` / `InkWell` | carte de budget cliquable | `lib/features/budget/presentation/budgets_screen.dart:58` |
| garde (page interdite) | `redirect:` (non connecté → `/connexion`, non admin → `/accueil`) | `app_router.dart:39` |

### 3.5 Widgets, listes et formulaires (cours S02.1 et S02.2)

| Notion du cours | Où dans SmartBuy |
|---|---|
| `Scaffold`, `AppBar` | tous les écrans, ex. `accueil_screen.dart` |
| `Column`, `Row`, `Container`, `Center`, `Card`, `Text` | `accueil_screen.dart`, `lib/shared/widgets/communs.dart` |
| `ElevatedButton` (`FilledButton` en Material 3) | `connexion_screen.dart` |
| `ListView` | `lib/features/budget/presentation/statistiques_screen.dart` |
| `ListView.builder` / `.separated` (enfants créés à la demande) | `operations_screen.dart:93` |
| `GridView` | non utilisé : nos listes sont verticales (opérations, produits). La grille aurait sa place pour un catalogue en vignettes |
| `Form` + `GlobalKey<FormState>` | `lib/features/auth/presentation/connexion_screen.dart:19` |
| `TextFormField` + `validator` | `inscription_screen.dart:53` |
| `_form.currentState!.validate()` | `connexion_screen.dart:25` |
| `TextField` + `onChanged` (recherche) | `lib/features/prix/presentation/catalogue_screen.dart:176` |
| `StatelessWidget` | ex. `budgets_screen.dart:11` (`BudgetsScreen`) |
| `StatefulWidget` + `setState` | ex. `connexion_screen.dart:12` (mot de passe masqué/affiché) |

### 3.6 Fonctions avancées (validation 3)

| Fonction | Où |
|---|---|
| Comparaison d'une liste (algorithme) | `backend/.../liste/ComparateurPrix.java:60`, version Dart : `lib/features/prix/logic/comparateur.dart:20` |
| Prévision de fin de mois | `budget/CalculsBudget.java`, version Dart : `lib/features/budget/logic/budget_calculs.dart:13` |
| Alertes 80 % / 100 % et e-mail | `alerte/AlerteService.java` |
| Code-barres : Oracle → Open Food Facts → Carrefour | `collecte/CollecteService.java:85` |
| Collecte Carrefour (API GraphQL) | `collecte/CarrefourTn.java:43` |
| Collecte Spacenet (web scraping HTML) | `collecte/SpacenetTn.java:69` |
| Respect du `robots.txt` et 1 requête/s | `collecte/ClientWebPoli.java:56`, `collecte/RobotsTxt.java` |
| Inscription avec code e-mail | `auth/AuthService.java:62`, `auth/CodesEmailService.java:57` |
| Domaine e-mail réel (DNS MX) | `auth/VerificateurDomaineEmail.java` |
| Conseils IA (règles + Claude) | `ia/ConseillerRegles.java`, `ia/ConseillerClaude.java` |
| Graphiques dessinés à la main | `lib/shared/widgets/graphiques.dart:65` (`CustomPainter`) |
| Cadre téléphone sur PC | `lib/shared/widgets/cadre_mobile.dart:8` |

---

## 4. Choix techniques et leurs raisons

| Choix | Pourquoi | Alternative écartée |
|---|---|---|
| Oracle + Spring Boot | Base imposée, intégrité par contraintes, calculs centralisés et testés | Firebase : NoSQL, pas d'Oracle, logique dispersée |
| API entre Flutter et Oracle | Pas d'identifiants de base dans l'APK | Connexion directe : faille de sécurité |
| Feature-first + 3 couches | Un module = un dossier, comme dans le cahier des charges | Layer-first : une fonction éparpillée |
| Provider | Simple, officiel, suffisant à cette taille | BLoC / Riverpod : plus verbeux |
| go_router | Routes déclarées au même endroit, garde de connexion, URL | `Navigator.push` dispersé dans les écrans |
| Repositories Mock / Api derrière une interface | Démo sans serveur (suivi 1), puis bascule sans réécriture (suivi 2) | Données codées en dur dans les écrans |
| E-mail confirmé par code | Seul moyen de prouver qu'une adresse existe et appartient à la personne | Simple vérification du format |
| Collecte respectueuse (`robots.txt`) | Légal et éthique : Jumia et Tunisianet exclus, recherche Spacenet non utilisée | Aspirer tous les sites sans règle |
| Règles, puis IA en option | Explicable, toujours disponible | IA seule : dépendance externe |
| Graphiques en `CustomPainter` | Peu de graphiques, et comprendre le Canvas | fl_chart (paquet non disponible hors ligne) |

---

## 5. Difficultés rencontrées et solutions

- **Des prix réels, légalement.** Avant de collecter, nous avons lu le `robots.txt` de chaque site. Jumia interdit tout robot et Tunisianet interdit notre outil : ils sont exclus. Carrefour Tunisie expose une API publique où la référence produit est le code-barres EAN-13. Spacenet autorise ses pages catégories, mais pas sa recherche.
- **Des codes-barres qui existent vraiment.** Les premiers codes étaient inventés. Ils ont été remplacés par de vrais codes tunisiens (Délice, Vitalait, Safia, Sicam…) issus d'Open Food Facts. Un code inconnu est cherché en ligne, puis ajouté au catalogue.
- **Un extracteur casse quand le site change.** Un double espace dans le HTML de Spacenet suffisait à ne rien lire. Chaque extracteur est isolé et couvert par un test (`CollecteTest`).
- **Des magasins hors sujet dans la comparaison.** Un magasin sans aucun article de la liste s'affichait à 0 DT. Il est désormais ignoré par l'algorithme.
- **Accès réseau limité** (pas d'accès direct à pub.dev). Nous nous sommes limités à des paquets déjà disponibles (provider, go_router, http, intl).
- **Oracle n'a pas de booléen** avant la version 23ai, d'où `CHAR(1)` avec `CHECK ('O','N')`. **`TRANSACTION` est un mot réservé**, d'où le nom de table `OPERATION`.
- **Recommander honnêtement.** Un magasin incomplet paraît moins cher : on ne recommande donc que les magasins complets.
- **Alertes en double.** Une alerte n'est créée qu'au franchissement d'un seuil, et elle est rattachée à son budget.
- **Émulateur Android.** `localhost` y désigne le téléphone lui-même : l'application utilise `10.0.2.2` automatiquement.

## 6. Limites actuelles

- Les prix des **magasins physiques** (Aziza, Géant…) ne sont pas publics : ils viennent du jeu de démonstration ou des propositions des utilisateurs. Seuls les magasins en ligne (Carrefour Tunisie, Spacenet) ont des prix collectés réels.
- Le code-barres se **saisit au clavier**, sans scan par caméra.
- Le **jeton JWT est gardé en mémoire** : il faut se reconnecter après avoir fermé l'application.
- En développement, les **codes e-mail s'affichent dans la console** du serveur (aucun compte d'envoi configuré).
- Pas de mode hors ligne.

## 7. Évolutions possibles

1. Scan caméra (`mobile_scanner`) et stockage sécurisé du jeton (`flutter_secure_storage`).
2. Nouvelles sources de prix (Mytek, sites de vêtements) et historique des prix collectés.
3. Mode hors ligne : file d'attente locale synchronisée au retour du réseau.
4. Prévision par *machine learning*, à comparer avec l'extrapolation actuelle.
5. Favoris et alerte de baisse de prix, objectifs d'épargne.

## 8. Tests et qualité

| Côté | Tests | Commande |
|---|---|---|
| Flutter | 31 (calculs, comparateur, scénarios, écrans) | `flutter test` |
| Back-end | 66 (comparaison, budget, alertes, JWT, codes e-mail, inscription, collecte, sécurité) | `cd backend` puis `mvn test` |

`flutter analyze` ne signale aucun problème.

## 9. Ce que nous avons appris

- Écrire un **contrat d'API** avant de coder permet de développer le front-end et le back-end en parallèle.
- Isoler les **règles métier dans des fonctions pures** les rend faciles à tester.
- Avec des **interfaces et l'injection des dépendances**, passer de la maquette à Oracle tient en une seule ligne de configuration.
- Le **web scraping** a des règles : lire le `robots.txt`, s'identifier, aller doucement, et accepter qu'un site refuse.
- L'IA est utile quand elle **complète** un système explicable, pas quand elle le remplace.
