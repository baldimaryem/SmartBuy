# 4. Architecture et modèle de données

> Ce document décrit **l'organisation** du système. Il ne contient pas de code : c'est le plan que le code suivra.

## 4.1 Architecture générale : 3 couches (3-tiers)

```
┌──────────────────────┐     HTTPS / JSON      ┌──────────────────────┐      JDBC       ┌──────────────────────┐
│  APPLICATION MOBILE  │  ───────────────────▶ │     API BACK-END     │ ──────────────▶ │   BASE DE DONNÉES    │
│       Flutter        │  ◀─────────────────── │     Spring Boot      │ ◀────────────── │        Oracle        │
│                      │      (jeton JWT)      │                      │                 │                      │
│ Écrans, navigation,  │                       │ Règles métier,       │                 │ Tables, contraintes, │
│ saisie, graphiques   │                       │ sécurité, calculs    │                 │ séquences, index     │
└──────────────────────┘                       └──────────────────────┘                 └──────────────────────┘
   Couche PRÉSENTATION                            Couche MÉTIER                            Couche DONNÉES
```

### Pourquoi l'application Flutter ne se connecte-t-elle pas directement à Oracle ?

C'est une question qu'on te posera sûrement. Il y a trois raisons :

1. **Sécurité** : pour se connecter à Oracle, il faut un identifiant et un mot de passe de base de données. S'ils sont dans l'application, n'importe qui peut les extraire du fichier `.apk` et accéder à **toutes** les données de **tous** les utilisateurs.
2. **Règles métier centralisées** : les calculs (reste à dépenser, meilleur magasin, alertes) se font à un seul endroit, le serveur. Toutes les versions de l'application obtiennent donc le même résultat.
3. **Technique** : il n'existe pas de pilote Oracle fiable pour une application mobile Flutter. Les pilotes Oracle sont prévus pour des serveurs (Java/JDBC).

### Rôle de chaque couche

| Couche | Technologie | Responsabilités |
|---|---|---|
| Présentation | Flutter | Afficher les écrans, valider la saisie (champs vides, montant positif), appeler l'API, afficher les graphiques, garder les dépenses hors ligne en attente |
| Métier | Spring Boot | Authentification (JWT), contrôle d'accès, calculs de budget, comparaison des prix, alertes, tâches planifiées (dépenses récurrentes) |
| Données | Oracle | Stocker les données de façon fiable, garantir l'intégrité (clés étrangères, contraintes `CHECK`), index pour la recherche |

## 4.2 Organisation interne prévue

### Back-end Spring Boot (architecture en couches)

```
Controller  →  reçoit la requête HTTP, renvoie du JSON          (ex. DepenseController)
   ↓
Service     →  applique les règles métier, fait les calculs     (ex. BudgetService)
   ↓
Repository  →  lit et écrit dans Oracle via JPA/Hibernate       (ex. OperationRepository)
   ↓
Oracle
```

Les classes sont regroupées par module : `auth`, `budget`, `prix`, `admin`.

### Application Flutter (organisation par fonctionnalité)

```
lib/
  core/          → configuration, client HTTP, thème, gestion du jeton
  features/
    auth/        → connexion, inscription
    budget/      → accueil, transactions, budgets, statistiques
    prix/        → recherche, comparaison, listes de courses, scan
  shared/        → composants réutilisables (boutons, cartes, formats de montant)
```

> **Pourquoi organiser par fonctionnalité ?** Chaque module est indépendant : on peut travailler sur « budget » sans toucher à « prix ». Cette organisation correspond aussi directement aux deux modules du cahier des charges.

## 4.3 Modèle de données (niveau conceptuel)

### Les entités

| Entité | Rôle | Attributs principaux |
|---|---|---|
| **UTILISATEUR** | Compte de l'application | id, nom, email (unique), mot_de_passe_hache, role (`USER` / `ADMIN`), devise, date_creation |
| **CATEGORIE** | Catégorie de dépense ou de revenu | id, nom, type (`DEPENSE` / `REVENU`), icone, utilisateur_id (vide = catégorie par défaut) |
| **OPERATION** | Un revenu ou une dépense | id, utilisateur_id, categorie_id, type, montant, date_operation, note, magasin_id (facultatif), recurrente (O/N) |
| **BUDGET** | Plafond mensuel | id, utilisateur_id, categorie_id (vide = budget global), mois (AAAA-MM), montant_plafond |
| **MAGASIN** | Point de vente | id, nom, enseigne, adresse, ville |
| **CATEGORIE_PRODUIT** | Rayon (Laitiers, Boissons…) | id, nom |
| **PRODUIT** | Article du catalogue | id, nom, marque, code_barres (unique), unite (kg, L, pièce), categorie_produit_id, image_url |
| **PRIX** | Prix d'un produit dans un magasin à une date | id, produit_id, magasin_id, montant, date_releve, source (`ADMIN` / `UTILISATEUR`), statut (`VALIDE` / `EN_ATTENTE` / `REJETE`), propose_par (utilisateur_id) |
| **LISTE_COURSES** | Liste préparée par l'utilisateur | id, utilisateur_id, nom, date_creation, statut (`EN_COURS` / `ACHETEE`) |
| **LIGNE_LISTE** | Produit dans une liste | id, liste_id, produit_id, quantite |
| **ALERTE** | Notification envoyée | id, utilisateur_id, type (`BUDGET_80`, `BUDGET_100`, `BAISSE_PRIX`), message, date, lue (O/N) |

### Les relations (à transformer en diagramme de classes)

```
UTILISATEUR 1 ──── * OPERATION * ──── 1 CATEGORIE
UTILISATEUR 1 ──── * BUDGET      * ──── 0..1 CATEGORIE
UTILISATEUR 1 ──── * LISTE_COURSES 1 ──── * LIGNE_LISTE * ──── 1 PRODUIT
PRODUIT     * ──── 1 CATEGORIE_PRODUIT
PRODUIT     1 ──── * PRIX * ──── 1 MAGASIN
OPERATION * ──── 0..1 MAGASIN          ← lien entre les 2 modules
UTILISATEUR 1 ──── * ALERTE
```

> **Point de conception important : pourquoi une table PRIX séparée ?** Un prix n'est pas un attribut du produit : il dépend du **magasin** et de la **date**. Mettre le prix dans PRODUIT empêcherait de comparer entre magasins et de garder l'historique. La table PRIX est une **association** entre PRODUIT et MAGASIN qui porte ses propres données (montant, date, statut).
>
> Le « prix actuel » d'un produit dans un magasin est le dernier prix au statut `VALIDE`.

## 4.4 Points spécifiques à Oracle

| Sujet | Choix | Pourquoi |
|---|---|---|
| Montants | `NUMBER(12,3)` | Type décimal exact, sans erreur d'arrondi (NF-09) |
| Identifiants | `NUMBER` + `GENERATED BY DEFAULT AS IDENTITY` (Oracle 12c et plus) | Numérotation automatique, sans gérer de séquence à la main |
| Texte | `VARCHAR2(n)` | Type texte standard d'Oracle |
| Noms de tables | `OPERATION` plutôt que `TRANSACTION` | `TRANSACTION` est un mot-clé SQL ; l'utiliser comme nom de table crée des confusions et des erreurs |
| Booléens | `CHAR(1)` avec `CHECK (col IN ('O','N'))` | Oracle n'a pas de type booléen dans les tables avant la version 23ai |
| Valeurs fixes (type, statut, rôle) | `VARCHAR2` + contrainte `CHECK` | Empêche d'enregistrer une valeur invalide |
| Unicité | Contrainte `UNIQUE` sur `email` et `code_barres` | Pas de doublon |
| Recherche rapide | Index sur `PRODUIT(nom)`, `PRIX(produit_id, magasin_id, date_releve)`, `OPERATION(utilisateur_id, date_operation)` | Respecte les temps de réponse NF-05 et NF-06 |
| Environnement de développement | **Oracle Database 19c** locale (service `orcl`, schéma `SMARTBUY`) avec **SQL Developer** | Déjà installée sur le poste ; aucune installation supplémentaire |

## 4.5 Exemple de flux complet : comparer une liste de courses

1. L'utilisateur ouvre sa liste « Courses semaine » dans Flutter.
2. Flutter envoie `GET /api/listes/12/comparaison` avec le jeton JWT.
3. Spring Boot vérifie le jeton, puis que la liste 12 appartient bien à cet utilisateur.
4. Le service lit dans Oracle les lignes de la liste et, pour chaque produit, le dernier prix validé dans chaque magasin.
5. Il calcule le total par magasin, repère les produits manquants et classe les magasins.
6. Il lit le budget restant de la catégorie « Alimentation » pour ce mois.
7. Il renvoie le résultat en JSON : magasin recommandé, économie réalisée, et un avertissement si le total dépasse le budget restant.
8. Flutter affiche le classement des magasins et l'éventuelle alerte.

Ce scénario deviendra un **diagramme de séquence** (voir [05-uml-a-produire.md](05-uml-a-produire.md)).
