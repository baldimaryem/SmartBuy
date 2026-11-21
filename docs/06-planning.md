# 6. Planning du projet

> Les dates exactes dépendent du calendrier du professeur. Le planning est exprimé en **semaines relatives (S1, S2…)** sur environ **14 semaines** : il suffira de remplacer S1 par la date de démarrage réelle et d'aligner les jalons sur les dates de suivi.

## 6.1 Méthode de travail

On travaille par **itérations courtes** de 2 semaines (approche inspirée de Scrum). Chaque itération se termine par quelque chose de **montrable**.

> **Pourquoi ?** Pour les suivis, le professeur veut voir un **avancement régulier**. Une itération = une preuve concrète (document, maquette, écran fonctionnel).

## 6.2 Phases et livrables

| Phase | Semaines | Contenu | Livrables (preuves pour le suivi) |
|---|---|---|---|
| **0. Cadrage** | S1 | Choix du sujet, problématique, objectifs, périmètre | `01-presentation-et-problematique.md` |
| **1. Analyse des besoins** | S2 | Besoins fonctionnels et non fonctionnels, priorités MoSCoW | `02-besoins-fonctionnels.md`, `03-besoins-non-fonctionnels.md` |
| **2. Conception** | S3–S4 | Diagrammes UML, architecture, modèle de données Oracle | Diagrammes dans `docs/uml/`, `04-architecture-et-donnees.md` |
| **3. Maquettes UI/UX** | S4–S5 | Maquettes Figma des écrans principaux, parcours utilisateur | Lien Figma et captures dans `docs/maquettes/` |
| **4. Mise en place technique** | S6 | Création de l'utilisateur et des tables Oracle, projet Spring Boot, structure Flutter, dépôt Git | Script SQL de création, première API « ping », application qui démarre |
| **5. Itération 1 : comptes + Smart Budget** | S7–S8 | AUTH-01..03, BUD-01, 03..06, 08..10 | Démo : inscription, ajout de dépenses, tableau de bord |
| **6. Itération 2 : budget avancé** | S9 | BUD-11 (alertes), BUD-13 (graphiques) | Démo : alerte à 80 %, graphique par catégorie |
| **7. Itération 3 : Comparaison des prix** | S10–S11 | ADM-01..04, PRX-01, 02, 04, 09..12 | Démo : catalogue, comparaison d'un produit, liste de courses |
| **8. Itération 4 : liens + fonctions « S »** | S12 | LNK-01..03, puis selon le temps PRX-03, PRX-07, BUD-07, BUD-15 | Démo du scénario complet |
| **9. Tests et correction** | S13 | Tests des calculs, tests utilisateurs, vérification des besoins NF | Rapport de tests |
| **10. Finalisation** | S14 | Rapport final, présentation, vidéo de démonstration | Rapport, diapositives, application installable (`.apk`) |

## 6.3 Jalons (points de contrôle)

| Jalon | Fin de semaine | Ce qui doit être validé |
|---|---|---|
| **J1** : cahier des charges validé | S2 | Problématique, besoins, priorités |
| **J2** : conception validée | S5 | UML, modèle de données, maquettes |
| **J3** : premier prototype | S9 | Module Smart Budget utilisable |
| **J4** : prototype complet | S12 | Les deux modules et leurs liens |
| **J5** : soutenance | S14 | Application finale et rapport |

> **Aligner les jalons sur les suivis du professeur :** si le professeur fait par exemple 3 suivis, on fait correspondre chaque suivi à un jalon (par exemple suivi 1 = J2, suivi 2 = J3, suivi 3 = J4).

## 6.4 Répartition des tâches (si travail en équipe)

À adapter selon le nombre de membres. Exemple pour 2 personnes :

| Membre | Responsabilité principale | Responsabilité partagée |
|---|---|---|
| Membre A | Module Smart Budget (écrans Flutter et services) | UML, base Oracle, rapport |
| Membre B | Module Comparaison des prix et espace administrateur | Maquettes, tests, présentation |

## 6.5 Risques et solutions

| Risque | Probabilité | Impact | Solution prévue |
|---|---|---|---|
| Pas assez de données de prix pour une démo convaincante | Élevée | Élevé | Préparer un jeu de données de test : 5 magasins, 50 produits, environ 250 prix |
| Accès à la base Oracle | Faible | Moyen | Utiliser l'Oracle 19c local (service `orcl`) dans un schéma dédié SMARTBUY, jamais une base client |
| Retard sur le planning | Moyenne | Élevé | Les priorités MoSCoW : on livre d'abord les « M », les « C » sont abandonnables |
| Problème d'accès à l'API depuis l'émulateur Android | Moyenne | Faible | Utiliser l'adresse `10.0.2.2` (alias de la machine hôte dans l'émulateur Android) |
| Fonction « intelligente » trop ambitieuse | Moyenne | Moyen | Commencer par une prévision simple (moyenne, extrapolation), le *machine learning* reste une évolution future |

## 6.6 Suivi de l'avancement

Pour chaque suivi, préparer :

1. **ce qui a été fait** depuis le dernier suivi (avec les ID des besoins réalisés) ;
2. **une démonstration** ou un document à montrer ;
3. **ce qui reste à faire** jusqu'au prochain suivi ;
4. **les difficultés** rencontrées et les solutions trouvées.

Un tableau de suivi des besoins (ID → statut : à faire / en cours / terminé / testé) sera tenu à jour dans `docs/suivi-avancement.md` dès la phase de développement.
