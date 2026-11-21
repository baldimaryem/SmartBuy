# 2. Besoins fonctionnels

Un **besoin fonctionnel** décrit **ce que fait** l'application : une action possible pour un acteur.

## Comment lire ce document

- **ID** : identifiant unique, réutilisé dans les diagrammes UML, le planning et les tests. Exemple : `BUD-03` est le 3ᵉ besoin du module Budget.
- **Priorité** (méthode **MoSCoW**) :
  - **M** (*Must*) : obligatoire, le projet échoue sans ;
  - **S** (*Should*) : important, à faire si possible ;
  - **C** (*Could*) : bonus, s'il reste du temps.

> **Pourquoi des priorités ?** Elles montrent au professeur que le projet est maîtrisé : on sait ce qu'il faut livrer en premier et ce qu'on peut sacrifier en cas de retard.

---

## 2.1 Module commun : comptes utilisateurs

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| AUTH-01 | Visiteur | Créer un compte (nom, e-mail, mot de passe) | M |
| AUTH-02 | Visiteur | Se connecter avec e-mail et mot de passe | M |
| AUTH-03 | Utilisateur | Se déconnecter | M |
| AUTH-04 | Utilisateur | Modifier son profil (nom, devise, revenu mensuel habituel) | S |
| AUTH-05 | Visiteur | Réinitialiser son mot de passe oublié | C |
| AUTH-06 | Administrateur | Se connecter à l'espace d'administration | M |

---

## 2.2 Module 1 : Smart Budget

### a) Catégories

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| BUD-01 | Utilisateur | Utiliser des catégories par défaut (Alimentation, Transport, Logement, Loisirs, Santé…) | M |
| BUD-02 | Utilisateur | Créer, renommer et supprimer ses propres catégories | S |

### b) Transactions (revenus et dépenses)

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| BUD-03 | Utilisateur | Ajouter un **revenu** (montant, date, source, note) | M |
| BUD-04 | Utilisateur | Ajouter une **dépense** (montant, date, catégorie, magasin facultatif, note) | M |
| BUD-05 | Utilisateur | Modifier ou supprimer une transaction | M |
| BUD-06 | Utilisateur | Consulter l'historique des transactions, filtré par mois, catégorie ou type | M |
| BUD-07 | Utilisateur | Déclarer une transaction **récurrente** (loyer, abonnement), générée automatiquement chaque mois | S |

### c) Budgets et alertes

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| BUD-08 | Utilisateur | Définir un budget mensuel **global** | M |
| BUD-09 | Utilisateur | Définir un budget mensuel **par catégorie** | M |
| BUD-10 | Utilisateur | Voir sur l'accueil le solde du mois et le **reste à dépenser** (global et par catégorie) | M |
| BUD-11 | Système | Alerter l'utilisateur quand une catégorie atteint **80 %** puis **100 %** de son budget | M |
| BUD-12 | Utilisateur | Définir un objectif d'épargne (montant, date cible) et suivre sa progression | C |

### d) Statistiques et partie « intelligente »

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| BUD-13 | Utilisateur | Voir la répartition des dépenses du mois par catégorie (graphique circulaire) | M |
| BUD-14 | Utilisateur | Voir l'évolution des dépenses sur les 6 derniers mois (graphique en barres) | S |
| BUD-15 | Système | **Prévoir** les dépenses de fin de mois à partir du rythme actuel et prévenir d'un dépassement probable | S |
| BUD-16 | Système | Donner des **conseils d'économie** simples (« Vos dépenses Loisirs ont augmenté de 30 % par rapport à la moyenne ») | C |

> **Pourquoi BUD-15 et BUD-16 sont « intelligents » sans être de l'IA lourde :** une prévision par extrapolation (dépensé ÷ jours écoulés × jours du mois) ou par moyenne des mois précédents est simple, explicable et suffisante pour un premier projet. Elle pourra être remplacée plus tard par un vrai modèle de *machine learning* : c'est une piste d'évolution à présenter.

---

## 2.3 Module 2 : Comparaison des prix

### a) Catalogue (consultation)

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| PRX-01 | Utilisateur | Consulter la liste des magasins (nom, adresse, enseigne) | M |
| PRX-02 | Utilisateur | Rechercher un produit par nom ou par catégorie | M |
| PRX-03 | Utilisateur | Trouver un produit en **scannant son code-barres** | S |
| PRX-04 | Utilisateur | Voir le prix d'un produit dans **tous les magasins**, triés du moins cher au plus cher, avec le meilleur prix mis en évidence | M |
| PRX-05 | Utilisateur | Voir l'historique du prix d'un produit (graphique) | C |
| PRX-06 | Utilisateur | Ajouter un produit en favori et être alerté quand son prix baisse | C |

### b) Contribution des utilisateurs

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| PRX-07 | Utilisateur | **Proposer un prix** constaté en magasin (produit, magasin, prix, date) | S |
| PRX-08 | Administrateur | Valider ou rejeter les prix proposés | S |

### c) Liste de courses

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| PRX-09 | Utilisateur | Créer, renommer et supprimer une liste de courses | M |
| PRX-10 | Utilisateur | Ajouter des produits avec une quantité, en retirer | M |
| PRX-11 | Système | Calculer le **total de la liste dans chaque magasin** | M |
| PRX-12 | Système | **Recommander le magasin le moins cher** et afficher l'économie réalisée par rapport au plus cher | M |
| PRX-13 | Système | Signaler les produits introuvables dans un magasin (total incomplet) | S |

### d) Administration du catalogue

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| ADM-01 | Administrateur | Ajouter, modifier ou supprimer un magasin | M |
| ADM-02 | Administrateur | Ajouter, modifier ou supprimer un produit (nom, marque, catégorie, code-barres, image) | M |
| ADM-03 | Administrateur | Gérer les catégories de produits | M |
| ADM-04 | Administrateur | Saisir ou mettre à jour le prix d'un produit dans un magasin | M |
| ADM-05 | Administrateur | Importer des prix en masse depuis un fichier CSV | C |

---

## 2.4 Liens entre les deux modules

Ces besoins justifient de réunir les deux modules dans une seule application.

| ID | Acteur | Besoin | Priorité |
|---|---|---|---|
| LNK-01 | Système | Comparer le total d'une liste de courses avec le **budget restant** de la catégorie concernée et avertir en cas de dépassement | M |
| LNK-02 | Utilisateur | Après l'achat, **transformer la liste en dépense** en un clic (montant, magasin et catégorie pré-remplis) | M |
| LNK-03 | Système | Calculer l'**économie réalisée** grâce à la comparaison et l'afficher dans les statistiques du budget | S |

---

## 2.5 Récapitulatif

| Module | Must | Should | Could | Total |
|---|---|---|---|---|
| Comptes (AUTH) | 4 | 1 | 1 | 6 |
| Smart Budget (BUD) | 10 | 4 | 2 | 16 |
| Comparaison des prix (PRX) | 7 | 4 | 2 | 13 |
| Administration (ADM) | 4 | 0 | 1 | 5 |
| Liens (LNK) | 2 | 1 | 0 | 3 |
| **Total** | **27** | **10** | **6** | **43** |

**Version minimale à livrer (MVP)** : les 27 besoins **M**. Ils suffisent pour la démonstration complète « je fais ma liste → je vois le magasin le moins cher → je vois si ça rentre dans mon budget → j'enregistre la dépense ».
