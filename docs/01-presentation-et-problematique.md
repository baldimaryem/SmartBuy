# 1. Présentation du projet et problématique

## 1.1 Contexte

Le coût de la vie augmente et les ménages, en particulier les étudiants et les jeunes actifs, ont du mal à :

- **savoir où part leur argent** : les dépenses sont dispersées et rarement notées ;
- **respecter un budget** : on s'aperçoit du dépassement en fin de mois, quand il est trop tard ;
- **acheter au meilleur prix** : un même produit n'a pas le même prix d'un magasin à l'autre, et comparer prend du temps.

Les applications existantes traitent souvent **un seul** de ces problèmes : soit la gestion de budget, soit la comparaison de prix. L'utilisateur doit donc jongler entre plusieurs outils qui ne communiquent pas.

## 1.2 Problématique

> **Comment aider un utilisateur à maîtriser son budget mensuel tout en dépensant moins lors de ses achats du quotidien, à l'aide d'une seule application mobile ?**

Sous-questions auxquelles le projet doit répondre :

1. Comment enregistrer facilement ses revenus et dépenses et voir en temps réel ce qu'il reste ?
2. Comment prévenir l'utilisateur **avant** qu'il dépasse son budget ?
3. Comment trouver rapidement le magasin le moins cher pour un produit ou une liste de courses ?
4. Comment relier les deux : savoir si une liste de courses « rentre » dans le budget restant ?

## 1.3 Solution proposée : SmartBuy

**SmartBuy** est une application mobile (Flutter) qui réunit deux modules qui travaillent ensemble.

| Module | Rôle | Question à laquelle il répond |
|---|---|---|
| **Smart Budget** | Suivre revenus, dépenses et budgets par catégorie, avec des alertes | « Combien puis-je encore dépenser ce mois-ci ? » |
| **Comparaison des prix** | Comparer le prix d'un produit ou d'une liste de courses entre magasins | « Où acheter pour payer le moins cher ? » |

**Ce qui rend SmartBuy « smart » : le lien entre les deux modules.**
Quand l'utilisateur prépare sa liste de courses, l'application :

1. calcule le total dans chaque magasin ;
2. recommande le magasin le moins cher ;
3. compare ce total au budget restant de la catégorie (par ex. « Alimentation ») et prévient en cas de dépassement ;
4. après l'achat, transforme la liste en dépense en un clic, ce qui met le budget à jour automatiquement.

> **Pourquoi seulement 2 modules ?** Un périmètre réduit mais bien fait vaut mieux que 5 modules à moitié finis. Les fonctions « IA », statistiques et scan de code-barres sont intégrées **dans** ces 2 modules comme fonctionnalités secondaires, et non comme modules séparés.

## 1.4 Objectifs du projet

| # | Objectif | Comment on le mesure |
|---|---|---|
| O1 | Permettre de saisir une dépense en moins de 15 secondes | Test utilisateur chronométré |
| O2 | Donner en permanence le « reste à dépenser » du mois | Visible sur l'écran d'accueil |
| O3 | Alerter l'utilisateur à 80 % et 100 % d'un budget | Notification déclenchée lors des tests |
| O4 | Comparer le prix d'un produit dans au moins 3 magasins | Démo avec données de test |
| O5 | Recommander le magasin le moins cher pour une liste de courses | Démo avec données de test |
| O6 | Relier une liste de courses au budget restant | Démo du scénario complet |

## 1.5 Périmètre

**Dans le périmètre (ce que l'on fait) :**

- application mobile Android (iOS possible grâce à Flutter, mais non testé) ;
- authentification des utilisateurs ;
- module Smart Budget ;
- module Comparaison des prix, avec liste de courses ;
- espace administrateur pour gérer les magasins, les produits et les prix ;
- API back-end et base de données **Oracle**.

**Hors périmètre (ce que l'on ne fait pas, et pourquoi) :**

| Exclu | Raison |
|---|---|
| Connexion aux comptes bancaires | Complexe (normes bancaires, sécurité, accords) |
| Récupération automatique des prix sur les sites des magasins (*scraping*) | Risques juridiques et techniques ; les prix sont saisis par l'admin et proposés par les utilisateurs |
| Paiement dans l'application | Ce n'est pas l'objectif du projet |
| Multi-devises | Une seule devise configurable suffit |

## 1.6 Acteurs

| Acteur | Description |
|---|---|
| **Visiteur** | Personne non connectée : peut seulement créer un compte ou se connecter |
| **Utilisateur** | Personne connectée : gère son budget, compare les prix, gère ses listes de courses, propose des prix |
| **Administrateur** | Gère les magasins, les produits et les catégories de produits ; valide les prix proposés par les utilisateurs |
| **Système (planificateur)** | Acteur technique : génère les dépenses récurrentes et envoie les alertes |

> **Pourquoi un administrateur ?** La comparaison des prix ne vaut que si les prix sont fiables. Sans source automatique, il faut quelqu'un qui gère le catalogue et valide les prix proposés par les utilisateurs (principe de *crowdsourcing* avec modération).
