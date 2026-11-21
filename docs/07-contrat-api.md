# 7. Contrat de l'API REST

> Ce document est le **contrat** entre l'application Flutter et le back-end Spring Boot.
> Les deux côtés l'implémentent à l'identique : c'est ce qui permet de passer des **données simulées (mock)** du suivi 1
> à la **vraie API + Oracle** du suivi 2 sans réécrire les écrans.

## Conventions

| Sujet | Règle |
|---|---|
| URL de base | `http://localhost:8080/api` (émulateur Android : `http://10.0.2.2:8080/api`) |
| Format | JSON, noms de champs en `camelCase` |
| Montants | nombres décimaux (dinar tunisien, 3 décimales) |
| Dates | `AAAA-MM-JJ` ; mois : `AAAA-MM` ; date-heure : ISO 8601 |
| Authentification | en-tête `Authorization: Bearer <jeton JWT>` sur toutes les routes sauf `/auth/register` et `/auth/login` |
| Erreurs | code HTTP + corps `{ "status": 400, "message": "Texte lisible en français" }` |
| Rôles | `USER`, `ADMIN`. Les routes marquées 🔒 ADMIN renvoient `403` aux autres |
| Énumérations | `type` d'opération/catégorie : `DEPENSE` / `REVENU` ; statut de prix : `VALIDE` / `EN_ATTENTE` / `REJETE` ; statut de liste : `EN_COURS` / `ACHETEE` |

## Objets renvoyés

```jsonc
// Utilisateur
{ "id": 1, "nom": "Sarra", "email": "sarra.benali@gmail.com", "role": "USER", "devise": "TND", "revenuMensuel": 1800.0 }

// Categorie
{ "id": 1, "nom": "Alimentation", "type": "DEPENSE", "icone": "shopping_cart", "personnalisee": false }

// Operation
{ "id": 10, "type": "DEPENSE", "montant": 45.5, "dateOperation": "2026-11-05", "note": "Courses",
  "categorieId": 1, "categorieNom": "Alimentation", "magasinId": 2, "magasinNom": "Monoprix",
  "recurrente": false, "economie": 3.2 }

// Budget (categorieId null = budget global)
{ "id": 3, "mois": "2026-11", "categorieId": 1, "categorieNom": "Alimentation",
  "montantPlafond": 400.0, "depense": 320.0, "reste": 80.0, "pourcentage": 80.0 }

// Magasin
{ "id": 1, "nom": "Carrefour La Marsa", "enseigne": "Carrefour", "adresse": "Zone commerciale", "ville": "La Marsa" }

// CategorieProduit
{ "id": 1, "nom": "Produits laitiers" }

// Produit (meilleurPrix / meilleurMagasinNom : null si aucun prix validé)
{ "id": 5, "nom": "Lait demi-écrémé 1L", "marque": "Délice", "codeBarres": "6191234500009", "unite": "L",
  "categorieProduitId": 1, "categorieProduitNom": "Produits laitiers", "imageUrl": null,
  "meilleurPrix": 1.35, "meilleurMagasinNom": "Aziza" }

// PrixMagasin (dernier prix VALIDE par magasin, trié du moins cher au plus cher)
{ "prixId": 40, "magasinId": 3, "magasinNom": "Aziza Ariana", "enseigne": "Aziza", "montant": 1.35,
  "dateReleve": "2026-11-02", "meilleur": true }

// Prix (proposition / administration)
{ "id": 41, "produitId": 5, "produitNom": "Lait demi-écrémé 1L", "magasinId": 3, "magasinNom": "Aziza Ariana",
  "montant": 1.30, "dateReleve": "2026-11-10", "source": "UTILISATEUR", "statut": "EN_ATTENTE", "proposePar": "Sarra" }

// Alerte  (type : BUDGET_80, BUDGET_100, BAISSE_PRIX)
{ "id": 7, "type": "BUDGET_80", "message": "Alimentation : 80 % du budget atteint", "dateAlerte": "2026-11-12T10:15:00", "lue": false }
```

## Routes

### Authentification (AUTH-01..04)

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| POST | `/auth/register` | `{ nom, email, motDePasse }` | 201 `{ message, email }` : compte créé **non confirmé**, code à 6 chiffres envoyé par e-mail. 400 si le domaine ne reçoit pas d'e-mails (pas d'enregistrement DNS MX, ex. `smartbuy.tn`), 409 si l'e-mail est déjà confirmé |
| POST | `/auth/verifier-email` | `{ email, code }` | `{ token, utilisateur }` : confirme l'adresse et connecte ; 400 si le code est faux ou expiré |
| POST | `/auth/renvoyer-code` | `{ email }` | `{ message }` : nouveau code de confirmation |
| POST | `/auth/login` | `{ email, motDePasse }` | `{ token, utilisateur }` ; **403** si l'e-mail n'est pas encore confirmé |
| GET | `/auth/me` | – | `Utilisateur` |
| PUT | `/auth/me` | `{ nom, devise, revenuMensuel }` | `Utilisateur` |
| POST | `/auth/mot-de-passe-oublie` | `{ email }` | `{ message }`. Même réponse que l'e-mail existe ou non ; envoie un code à 6 chiffres valable 15 min (AUTH-05) |
| POST | `/auth/reinitialiser-mot-de-passe` | `{ email, code, nouveauMotDePasse }` | `{ message }`, ou 400 si le code est faux ou expiré (5 essais maximum) |

Les routes d'inscription, de confirmation et de réinitialisation sont publiques (sans jeton). Les codes sont stockés hachés dans la table `CODE_EMAIL`, valables 15 minutes, 5 essais maximum.

### Smart Budget (BUD-01..16)

| Méthode | Route | Corps / paramètres | Réponse |
|---|---|---|---|
| GET | `/categories?type=DEPENSE` | `type` facultatif | `Categorie[]` (par défaut + celles de l'utilisateur) |
| POST / PUT / DELETE | `/categories`, `/categories/{id}` | `{ nom, type, icone }` | `Categorie` / 204. Seules les catégories personnelles sont modifiables |
| GET | `/operations?mois=2026-11&type=&categorieId=` | filtres facultatifs | `Operation[]` triées par date décroissante |
| POST | `/operations` | `{ type, montant, dateOperation, note, categorieId, magasinId, recurrente }` | `Operation` (déclenche le contrôle des alertes 80 % / 100 % et l'e-mail) |
| PUT / DELETE | `/operations/{id}` | idem | `Operation` / 204 |
| GET | `/budgets?mois=2026-11` | – | `Budget[]` (global en premier) |
| POST | `/budgets` | `{ mois, categorieId, montantPlafond }` | `Budget` (crée ou met à jour) |
| DELETE | `/budgets/{id}` | – | 204 |
| GET | `/dashboard?mois=2026-11` | – | voir ci-dessous |
| GET | `/alertes` | – | `Alerte[]` (plus récentes d'abord) |
| PUT | `/alertes/{id}/lue` | – | `Alerte` |

Réponse de `/dashboard` :

```jsonc
{
  "mois": "2026-11",
  "totalRevenus": 1800.0, "totalDepenses": 950.0, "solde": 850.0,
  "budgetGlobal": 1200.0,            // null si aucun budget global
  "resteADepenser": 250.0,           // null si aucun budget global
  "prevision": { "depenseEstimeeFinMois": 1425.0, "depassementProbable": true },   // BUD-15
  "repartition": [ { "categorieId": 1, "categorieNom": "Alimentation", "montant": 320.0 } ], // BUD-13
  "evolution":   [ { "mois": "2026-06", "depenses": 1100.0, "revenus": 1800.0 } ],          // BUD-14, 6 mois
  "economiesRealisees": 12.4,        // LNK-03, somme des économies du mois
  "alertesNonLues": 2
}
```

**Règle de prévision (BUD-15)** : `dépensé ÷ jours écoulés × nombre de jours du mois` (mois courant) ; pour un mois passé, la prévision est le total réel. `depassementProbable` = prévision > budget global.

### Comparaison des prix (PRX-01..13)

| Méthode | Route | Corps / paramètres | Réponse |
|---|---|---|---|
| GET | `/magasins` | – | `Magasin[]` |
| POST / PUT / DELETE 🔒 | `/magasins`, `/magasins/{id}` | `{ nom, enseigne, adresse, ville }` | `Magasin` / 204 |
| GET | `/categories-produits` | – | `CategorieProduit[]` |
| POST / PUT / DELETE 🔒 | `/categories-produits`, `/categories-produits/{id}` | `{ nom }` | `CategorieProduit` / 204 |
| GET | `/produits?q=lait&categorieId=1&codeBarres=` | filtres facultatifs | `Produit[]` |
| GET | `/produits/{id}` | – | `Produit` |
| GET | `/produits/{id}/prix` | – | `PrixMagasin[]` (PRX-04) |
| GET | `/produits/{id}/historique` | – | `[{ magasinNom, montant, dateReleve }]` triés par date (PRX-05) |
| POST / PUT / DELETE 🔒 | `/produits`, `/produits/{id}` | `{ nom, marque, codeBarres, unite, categorieProduitId, imageUrl }` | `Produit` / 204 |
| POST | `/prix` | `{ produitId, magasinId, montant, dateReleve }` | `Prix`. Utilisateur → `EN_ATTENTE` (PRX-07) ; admin → `VALIDE` (ADM-04) |
| GET 🔒 | `/prix/en-attente` | – | `Prix[]` |
| PUT 🔒 | `/prix/{id}/valider`, `/prix/{id}/rejeter` | – | `Prix` (PRX-08) |

### Listes de courses (PRX-09..13, LNK-01..03)

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| GET | `/listes` | – | `[{ id, nom, dateCreation, statut, nbArticles }]` |
| POST | `/listes` | `{ nom }` | liste résumée |
| PUT / DELETE | `/listes/{id}` | `{ nom }` | liste résumée / 204 |
| GET | `/listes/{id}` | – | `{ id, nom, statut, dateCreation, lignes: [{ id, produitId, produitNom, unite, quantite }] }` |
| POST | `/listes/{id}/lignes` | `{ produitId, quantite }` | liste détaillée (si le produit existe déjà, la quantité s'additionne) |
| PUT / DELETE | `/listes/{id}/lignes/{ligneId}` | `{ quantite }` | liste détaillée |
| GET | `/listes/{id}/comparaison` | – | voir ci-dessous |
| POST | `/listes/{id}/acheter` | `{ magasinId }` | `Operation` créée (LNK-02) |

Réponse de `/listes/{id}/comparaison` :

```jsonc
{
  "listeId": 12,
  "magasins": [            // complets d'abord, puis total croissant
    { "magasinId": 3, "magasinNom": "Aziza Ariana", "enseigne": "Aziza", "total": 41.2, "complet": true,
      "produitsManquants": [],
      "details": [ { "produitId": 5, "produitNom": "Lait demi-écrémé 1L", "quantite": 6, "prixUnitaire": 1.35, "sousTotal": 8.1 } ] }
  ],
  "magasinRecommandeId": 3,       // le moins cher parmi les magasins complets (null si aucun)
  "economie": 6.8,                // total du plus cher complet − total du moins cher complet
  "budgetRestant": 80.0,          // reste du budget « Alimentation » du mois (null si pas de budget)
  "depasseBudget": false          // LNK-01
}
```

**Achat (LNK-02/03)** : crée une `DEPENSE` dans la catégorie *Alimentation*, montant = total de la liste dans le magasin choisi,
`magasinId` renseigné, note `Liste : <nom>`, `economie` = total du magasin complet le plus cher − total payé. La liste passe à `ACHETEE`.

### Fonctions avancées (validation 3)

| Méthode | Route | Corps | Réponse |
|---|---|---|---|
| GET | `/ia/conseils?mois=2026-11` | – | `{ source: "IA" \| "REGLES", conseils: [{ titre, message, niveau: "INFO" \| "ATTENTION" \| "ALERTE" }] }` (BUD-16) |
| POST | `/ia/assistant` | `{ question }` | `{ source: "IA" \| "REGLES", reponse }` : assistant budgétaire en langage naturel |
| POST | `/rapports/mensuel?mois=2026-11` | – | `{ envoye: true, destinataire }` : envoie le bilan du mois par e-mail |

- **IA** : si une clé `ANTHROPIC_API_KEY` est configurée sur le serveur, les conseils et l'assistant sont produits par un modèle Claude à partir du résumé chiffré du mois. Sinon, un moteur de **règles** local répond (l'application fonctionne toujours).
- **E-mails** : alertes de budget 80 % / 100 % et bilan mensuel (envoi automatique le 1ᵉʳ du mois + bouton manuel). Ils sont envoyés par **Gmail** (SMTP) ; sans compte d'envoi configuré, les codes s'affichent dans la console de l'API.
- **Tâche planifiée** : le 1ᵉʳ de chaque mois, les opérations `recurrente = true` du mois précédent sont recopiées (BUD-07).

## Comptes

Aucun compte fictif n'est créé. Chaque utilisateur s'inscrit avec une adresse e-mail réelle et la confirme avec le code reçu.
L'administrateur est l'adresse déclarée dans `smartbuy.admin.email` (fichier local `backend/config/application.yml`) :
ce compte reçoit le rôle `ADMIN` à son inscription, ou au démarrage de l'API s'il existe déjà.

---

## Évolution v3 : univers de courses, codes-barres réels, collecte des prix en ligne

### Univers

Trois univers de courses : `ALIMENTATION`, `VETEMENTS`, `HIGH_TECH`. Chaque rayon (catégorie de produits) et chaque liste de courses appartient à un univers.

| Univers | Catégorie de budget utilisée à l'achat (LNK-01 / LNK-02) |
|---|---|
| `ALIMENTATION` | « Alimentation » |
| `VETEMENTS` | « Vêtements » |
| `HIGH_TECH` | « High-tech » |

Objets enrichis (champs **ajoutés**, les anciens restent) :

```jsonc
// CategorieProduit
{ "id": 9, "nom": "Smartphones", "univers": "HIGH_TECH" }

// Produit : + univers, source, urlSource
{ "id": 40, "nom": "Lait demi-écrémé UHT", "marque": "Délice", "codeBarres": "6194043001019", "unite": "L",
  "categorieProduitId": 1, "categorieProduitNom": "Produits laitiers", "imageUrl": "https://images.openfoodfacts.org/...",
  "meilleurPrix": 1.35, "meilleurMagasinNom": "Carrefour Tunisie (en ligne)",
  "univers": "ALIMENTATION",
  "source": "CATALOGUE",            // CATALOGUE (saisi) | OPEN_FOOD_FACTS (trouvé par code-barres) | COLLECTE (site marchand) | UTILISATEUR
  "urlSource": null }               // page du produit sur le site marchand (collecte), sinon null

// Magasin : + enLigne, siteWeb
{ "id": 6, "nom": "Carrefour Tunisie (en ligne)", "enseigne": "Carrefour", "adresse": null, "ville": null,
  "enLigne": true, "siteWeb": "https://www.carrefour.tn" }

// PrixMagasin : + source ("ADMIN" | "UTILISATEUR" | "COLLECTE")
{ "prixId": 300, "magasinId": 6, "magasinNom": "Carrefour Tunisie (en ligne)", "enseigne": "Carrefour",
  "montant": 1.35, "dateReleve": "2026-11-21", "meilleur": true, "source": "COLLECTE" }

// ListeResume / ListeDetail : + univers
{ "id": 12, "nom": "Courses semaine", "dateCreation": "2026-11-21", "statut": "EN_COURS", "nbArticles": 6, "univers": "ALIMENTATION" }
```

### Routes ajoutées ou modifiées

| Méthode | Route | Corps / paramètres | Réponse |
|---|---|---|---|
| GET | `/categories-produits?univers=HIGH_TECH` | `univers` facultatif | `CategorieProduit[]` |
| GET | `/produits?univers=&q=&categorieId=&codeBarres=` | `univers` ajouté | `Produit[]` |
| GET | `/produits/code-barres/{code}` | – | `{ produit: Produit, origine: "CATALOGUE" \| "OPEN_FOOD_FACTS", message }`. 1) cherché dans Oracle ; 2) sinon dans **Open Food Facts** : le produit est **créé dans Oracle** (univers ALIMENTATION) ; 3) puis son **prix réel chez Carrefour Tunisie** est collecté. **404** `{status, message}` si le code est inconnu partout |
| POST | `/produits/contribution` | `{ nom, marque, codeBarres, unite, categorieProduitId }` | `Produit` (201). Tout utilisateur connecté peut ajouter un produit introuvable ; `source = UTILISATEUR` |
| POST | `/produits/{id}/actualiser-prix` | – | `PrixMagasin[]` à jour : relance la collecte en ligne pour ce produit (Carrefour par code-barres, ou la page `urlSource`) |
| POST 🔒 | `/admin/collecte?source=CARREFOUR` | – | `{ source, produitsAnalyses, produitsCrees, prixEnregistres, erreurs: [string], duree }` : prix Carrefour de tous les produits qui ont un code-barres |
| POST 🔒 | `/admin/collecte?source=SPACENET` | – | même réponse : téléphones, PC portables, tablettes depuis les pages catégories de spacenet.tn (produits créés ou mis à jour) |
| POST | `/listes` | `{ nom, univers }` | `univers` obligatoire (défaut `ALIMENTATION`) |
| POST | `/listes/{id}/lignes` | `{ produitId, quantite }` | **400** si le produit n'est pas du même univers que la liste |
| GET | `/listes/{id}/comparaison` | – | `budgetRestant` = budget de la catégorie de l'univers (Alimentation, Vêtements ou High-tech) |
| POST | `/listes/{id}/acheter` | `{ magasinId }` | la dépense est créée dans la catégorie de l'univers |

### Règles de la collecte (web scraping respectueux)

- Avant chaque site, lecture de son `robots.txt` : une page interdite n'est jamais demandée.
- Agent identifié : `SmartBuy-etudiant/1.0 (projet universitaire)`, au plus **1 requête par seconde** et par site.
- Sources : **Carrefour Tunisie** (API publique GraphQL du site, référence produit = code-barres EAN-13) et **Spacenet** (pages catégories HTML). Jumia et Tunisianet sont exclus (leur `robots.txt` l'interdit).
- Un prix collecté est enregistré avec `source = COLLECTE`, `statut = VALIDE` et la date du jour, dans le magasin « en ligne » de l'enseigne.
- Collecte automatique chaque nuit à 3 h (`smartbuy.collecte.cron`), désactivable (`smartbuy.collecte.active`).
- Les noms et codes-barres trouvés viennent d'**Open Food Facts** (licence ODbL, attribution affichée dans l'application).
