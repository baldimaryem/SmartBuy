# SmartBuy : dossier de conception

**SmartBuy** est une application mobile qui aide à **maîtriser son budget** et à **acheter au meilleur prix**. Elle réunit deux modules :

- **Smart Budget** : revenus, dépenses, budgets mensuels par catégorie, alertes, statistiques ;
- **Comparaison des prix** : recherche de produits, comparaison entre magasins, listes de courses.

Les deux modules sont reliés : une liste de courses est comparée au budget restant, puis enregistrée comme dépense.

**Technologies :** Flutter (mobile) · Spring Boot (API REST) · Oracle Database

> **Phase actuelle : développement terminé.** Les documents 1 à 6 sont la conception. La réalisation est décrite par le contrat d'API (doc 7), l'[architecture](architecture.md) et le [guide d'utilisation](guide-utilisation.md). Le bilan est dans [../reflexion.md](../reflexion.md).
>
> Écart avec le modèle de données du §4.3 : la table `ALERTE` a une colonne `BUDGET_ID` en plus (une alerte par seuil et par budget), et `OPERATION` a une colonne `ECONOMIE` (LNK-03).

## Documents, dans l'ordre de lecture

| # | Document | Contenu | Question à laquelle il répond |
|---|---|---|---|
| 1 | [Présentation et problématique](01-presentation-et-problematique.md) | Contexte, problématique, objectifs, périmètre, acteurs | Pourquoi ce projet ? |
| 2 | [Besoins fonctionnels](02-besoins-fonctionnels.md) | 43 besoins numérotés et priorisés (MoSCoW) | Que doit faire l'application ? |
| 3 | [Besoins non fonctionnels](03-besoins-non-fonctionnels.md) | Sécurité, performance, ergonomie, contraintes techniques | Comment doit-elle se comporter ? |
| 4 | [Architecture et données](04-architecture-et-donnees.md) | Architecture 3-tiers, organisation, modèle de données Oracle | Comment est-elle organisée ? |
| 5 | [UML à produire](05-uml-a-produire.md) | Cas d'utilisation, classes, séquences, activité | Quels diagrammes dessiner ? |
| 6 | [Planning](06-planning.md) | Phases, jalons, livrables, risques | Quand et dans quel ordre ? |

## Le fil conducteur : la traçabilité

Chaque besoin a un identifiant (ex. `BUD-11`). On le retrouve :

```
Besoin (doc 02)  →  Cas d'utilisation (doc 05)  →  Itération du planning (doc 06)  →  Test
     BUD-11      →  « Envoyer une alerte »      →  Itération 2 (S9)                →  Test alerte 80 %
```

Cela permet de prouver au professeur que **tout ce qui est demandé est conçu, planifié et testé**, et que rien n'est fait « au hasard ».

## Points à compléter ou valider avec le professeur

- [ ] **Critères des suivis** : quels livrables à quelle date ? (pour aligner les jalons du [planning](06-planning.md))
- [ ] **Dates** : démarrage, suivis, soutenance
- [ ] **Équipe** : nombre de membres et répartition
- [ ] **Devise** de l'application (dinar tunisien, euro…)
- [ ] **Back-end** : le projet Spring Boot sera-t-il dans ce même dossier ou dans un dossier voisin ?
- [ ] Le professeur exige-t-il une partie « IA » plus poussée que la prévision simple (BUD-15) ?
