# 3. Besoins non fonctionnels et contraintes

Un **besoin non fonctionnel** décrit **comment** l'application doit se comporter : qualité, sécurité, performance. Il ne s'agit pas d'une action de l'utilisateur, mais d'une exigence sur l'ensemble du système.

> **Pourquoi c'est important pour l'évaluation :** un cahier des charges sans besoins non fonctionnels est incomplet. Chaque exigence doit être **mesurable** : « l'application doit être rapide » ne veut rien dire, alors que « la recherche répond en moins de 2 secondes » peut être testé.

## 3.1 Besoins non fonctionnels

| ID | Catégorie | Exigence | Comment on la vérifie |
|---|---|---|---|
| NF-01 | **Sécurité** | Les mots de passe sont stockés **hachés** (BCrypt), jamais en clair | Inspection de la table `UTILISATEUR` |
| NF-02 | Sécurité | Chaque appel à l'API est authentifié par un **jeton JWT** ; un utilisateur ne voit que **ses** données | Test : appeler l'API avec le jeton d'un autre utilisateur, accès refusé (403) |
| NF-03 | Sécurité | Les échanges entre l'application et le serveur passent en **HTTPS** | Configuration du serveur |
| NF-04 | Sécurité | Seul un administrateur peut accéder aux fonctions `ADM-*` | Test avec un compte utilisateur, accès refusé |
| NF-05 | **Performance** | La recherche de produit et la comparaison de prix répondent en moins de **2 s** | Mesure sur un jeu de 1 000 produits |
| NF-06 | Performance | Le calcul d'une liste de 30 produits sur 10 magasins prend moins de **3 s** | Mesure |
| NF-07 | **Ergonomie** | Une dépense peut être saisie en moins de **3 écrans** et **15 s** | Test utilisateur |
| NF-08 | Ergonomie | L'interface est en **français**, lisible, et suit les règles Material Design | Revue des maquettes |
| NF-09 | **Fiabilité** | Les montants sont exacts au centime près (aucune erreur d'arrondi) | Tests unitaires sur les calculs |
| NF-10 | Fiabilité | Une dépense saisie sans réseau n'est pas perdue : elle est mise en attente puis envoyée | Test en mode avion |
| NF-11 | **Maintenabilité** | Le code est organisé par module (budget, prix) et par couche ; il est versionné avec Git | Revue du dépôt |
| NF-12 | Maintenabilité | Les calculs importants (budget restant, total d'une liste, prévision) sont couverts par des tests | Rapport de tests |
| NF-13 | **Portabilité** | L'application fonctionne sur Android 8 et plus (iOS possible grâce à Flutter) | Test sur émulateur et sur téléphone |
| NF-14 | **Confidentialité** | Les données financières ne sont jamais partagées ; l'utilisateur peut supprimer son compte et ses données | Fonction de suppression du compte |

> **Pourquoi NF-09 compte autant :** en informatique, `0.1 + 0.2` ne donne pas exactement `0.3` avec les nombres « flottants ». Pour de l'argent, on utilise donc des types **décimaux exacts** : `NUMBER(12,3)` dans Oracle (3 décimales, pour gérer aussi les devises à millimes comme le dinar tunisien), `BigDecimal` en Java. Côté Flutter, il faut rester prudent : `double` sert uniquement à l'affichage, les calculs se font sur le serveur.

## 3.2 Contraintes techniques (imposées ou choisies)

| Contrainte | Choix | Justification |
|---|---|---|
| Application mobile | **Flutter** (Dart) | Un seul code pour Android et iOS ; le projet est déjà créé |
| Base de données | **Oracle Database** (Oracle Database 19c installée sur le poste de développement) | Exigence du projet ; SGBD relationnel robuste, très répandu en entreprise |
| Back-end (API) | **Spring Boot** (Java) | S'intègre très bien avec Oracle (JDBC, JPA/Hibernate) ; standard en entreprise |
| Échanges | API **REST** en **JSON** | Simple, standard, facile à tester avec Postman |
| Versionnement | **Git** | Historique du travail, preuve d'avancement pour les suivis |
| Maquettes | **Figma** | Gratuit, maquettes cliquables pour présenter les écrans |
| Diagrammes UML | **StarUML**, **draw.io** ou **PlantUML** | Au choix |

## 3.3 Contraintes de projet

| Contrainte | Détail |
|---|---|
| Délai | À compléter selon les dates du professeur (voir [06-planning.md](06-planning.md)) |
| Équipe | À compléter (nombre de membres, rôles) |
| Données de prix | Aucune source automatique : catalogue saisi par l'administrateur et enrichi par les utilisateurs |
| Coût | 0 € : uniquement des outils gratuits (Oracle déjà installé, Flutter, Spring Boot, Figma gratuit) |
