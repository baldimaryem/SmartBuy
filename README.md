# SmartBuy

**Gérer son budget et acheter au meilleur prix, dans une seule application.**

SmartBuy est une application mobile Flutter, adossée à une API Spring Boot et à une base Oracle. Elle répond à une question simple que beaucoup se posent en fin de mois : *« Où part mon argent, et est-ce que je pourrais payer moins cher ? »*

---

## Pourquoi SmartBuy ?

D'un côté, on note ses dépenses dans une application de budget. De l'autre, on compare les prix d'un magasin à l'autre, souvent de tête ou sur plusieurs sites. Ces deux gestes ne se parlent jamais.

SmartBuy les réunit :

> Je prépare ma liste de courses → l'application me montre le magasin le moins cher → elle vérifie que ça rentre dans mon budget → en un clic, la dépense est enregistrée, avec l'économie réalisée.

---

## Ce que fait l'application

### Smart Budget
- Saisir ses **revenus** (salaire, freelance, carte resto…) et ses **dépenses**, y compris les dépenses **récurrentes** comme le loyer ou les abonnements.
- Fixer des **budgets** : un plafond global et un plafond par catégorie. L'application propose un budget qui garde 20 % d'épargne.
- Voir en un coup d'œil le **solde du mois**, le **reste à dépenser** et la **prévision de fin de mois**.
- Recevoir une **alerte** à 80 % puis à 100 % d'un budget, dans l'application et par e-mail.
- Suivre ses **statistiques** (répartition par catégorie, évolution sur 6 mois) et demander conseil à l'**assistant**.

### Comparaison des prix
- Trois **univers** de courses : **Alimentation**, **Vêtements** et **High-tech**.
- Rechercher un produit par son nom ou par son **vrai code-barres**. Un code inconnu est cherché sur Open Food Facts, puis ajouté au catalogue.
- Comparer le **prix d'un produit dans chaque magasin**, avec son historique.
- Préparer des **listes de courses** et trouver le **magasin le moins cher** pour toute la liste.
- **Prix réels** collectés en ligne sur Carrefour Tunisie et Spacenet.
- Proposer un prix constaté en magasin, que l'administrateur valide.

### Comptes et sécurité
- Inscription avec une **adresse e-mail réelle**, confirmée par un **code à 6 chiffres** envoyé par e-mail.
- Mot de passe oublié, profil, et **espace administrateur** (catalogue, validation des prix, collecte).

---

## Architecture en bref

```
Application Flutter  ──HTTP/JSON + JWT──▶  API REST Spring Boot  ──JDBC──▶  Oracle 19c
   (lib/)                                    (backend/)                       (schéma SMARTBUY)
                                                 │
                                                 ├── Gmail : codes, alertes, bilan mensuel
                                                 └── Open Food Facts · carrefour.tn · spacenet.tn
```

L'application ne se connecte jamais directement à la base : les identifiants restent sur le serveur, et toutes les règles métier sont calculées au même endroit.

La description complète (couches, dossiers, tables, flux) est dans **[docs/architecture.md](docs/architecture.md)**.

| Couche | Technologies |
|---|---|
| Mobile | Flutter · Dart · Provider (gestion d'état) · go_router (navigation) · http |
| API | Spring Boot 3.5 · Java 17 · Spring Security + JWT · Spring Data JPA · Spring Mail |
| Données | Oracle Database 19c · scripts SQL versionnés |
| Services | Gmail (SMTP) · Open Food Facts · Carrefour Tunisie · Spacenet · Claude (IA, en option) |

---

## Démarrer

### Prérequis
- Flutter (SDK 3.13 ou plus récent)
- JDK 17
- Oracle Database 19c en local (`localhost:1521`, service `orcl`) et SQL Developer

### 1. Préparer la base (une seule fois)
Dans SQL Developer, connecté en **SYS** sur `localhost:1521/orcl`, exécutez [`database/00_create_user.sql`](database/00_create_user.sql) avec **F5**. Le script crée l'utilisateur `smartbuy`. Les tables sont créées automatiquement par l'API à son premier démarrage.

### 2. Configurer l'envoi des e-mails (facultatif)
Créez `backend/config/application.yml` à partir de [`backend/config/application.yml.exemple`](backend/config/application.yml.exemple). Indiquez-y l'adresse Gmail d'envoi, un **mot de passe d'application** Google et l'adresse de l'administrateur. Ce fichier reste sur votre poste : il est exclu de Git.

Sans cette configuration, l'application fonctionne normalement, et les codes de confirmation s'affichent dans la console de l'API.

### 3. Lancer l'API
```bat
C:\workspace\flutter_app\backend\demarrer-api.bat
```
Attendez la ligne `Started SmartBuyApplication`, puis laissez la fenêtre ouverte.

### 4. Lancer l'application
```bat
cd /d C:\workspace\flutter_app
flutter run -d chrome
```
Sur ordinateur, l'application s'affiche dans un cadre de téléphone : elle est pensée pour le mobile. Sur un émulateur Android, l'API est jointe automatiquement à l'adresse `10.0.2.2:8080`.

### Mode démonstration, sans serveur
```bat
flutter run -d chrome --dart-define=USE_MOCK=true
```
Les données sont alors simulées en mémoire. En mode démonstration, le code de confirmation est toujours `123456`.

---

## Premiers pas

1. **Créez un compte** avec votre adresse e-mail, puis saisissez le code reçu.
2. Dans **Profil**, indiquez votre revenu habituel, puis ajoutez votre salaire dans **Opérations**.
3. Fixez vos plafonds dans **Accueil → Budgets**.
4. Dans **Prix**, tapez un code-barres réel, par exemple `6194043001019` (lait Délice), ou choisissez un univers.
5. Créez une **liste**, comparez les magasins, puis cliquez sur **J'ai acheté ici**.

Le parcours détaillé, écran par écran, est dans **[docs/guide-utilisation.md](docs/guide-utilisation.md)**.

---

## Tests

```bat
flutter test
```
```bat
cd backend && mvn test
```
Si Maven n'est pas installé, `mvnw.cmd test` le télécharge automatiquement, à condition d'avoir accès à Internet.

| Côté | Couverture |
|---|---|
| Flutter (31 tests) | calculs du budget, comparaison des listes, scénarios complets, écrans |
| Back-end (66 tests) | comparaison, alertes, JWT, codes e-mail, inscription, collecte des prix, sécurité |

`flutter analyze` ne signale aucun problème.

---

## Organisation du dépôt

```
flutter_app/
├── lib/            application Flutter, organisée par fonctionnalité
│   ├── core/         configuration, client HTTP, routes, thème
│   ├── features/     auth · budget · prix · ia · admin  (chacun : data / logic / presentation)
│   └── shared/       composants communs, graphiques, données de démonstration
├── test/           tests Flutter
├── backend/        API Spring Boot, avec son README
├── database/       scripts SQL Oracle (utilisateur, schéma, données, évolutions)
├── docs/           conception, contrat d'API, architecture, guide d'utilisation
└── reflexion.md    bilan du projet et carte du code
```

---

## Documentation

| Document | Contenu |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Architecture complète : couches, dossiers, base de données, services externes |
| [docs/07-contrat-api.md](docs/07-contrat-api.md) | Toutes les routes de l'API, avec leurs données |
| [docs/guide-utilisation.md](docs/guide-utilisation.md) | L'application pas à pas |
| [reflexion.md](reflexion.md) | Choix techniques, difficultés, limites, et où trouver chaque notion dans le code |
| [backend/README.md](backend/README.md) | Installation et configuration de l'API |
| [docs/README.md](docs/README.md) | Dossier de conception : besoins, UML, planning |

---

## Données, éthique et limites

- **Collecte respectueuse.** Avant de lire un site, l'API consulte son `robots.txt` et n'accède qu'aux pages autorisées. Elle s'identifie (`SmartBuy-etudiant`) et se limite à une requête par seconde. Jumia et Tunisianet, qui l'interdisent, sont exclus.
- **Prix réels et prix de démonstration.** Les prix des boutiques en ligne (Carrefour Tunisie, Spacenet) sont réels. Les magasins physiques ne publient pas leurs prix : ceux-là viennent du jeu de démonstration ou des propositions des utilisateurs.
- **Vie privée.** Les mots de passe et les codes e-mail sont hachés. Seul un résumé chiffré et anonyme du mois est envoyé à l'IA, et seulement si elle est activée.
- **Pistes d'évolution.** Scan du code-barres par la caméra, mode hors ligne, nouvelles sources de prix.

## Remerciements

Les informations produit proviennent d'[Open Food Facts](https://world.openfoodfacts.org), une base collaborative publiée sous licence ODbL. Merci à ses contributeurs.

---

*Projet universitaire, développement mobile avec Flutter.*
