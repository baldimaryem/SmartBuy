# 5. Diagrammes UML à produire

> Ce document liste les diagrammes à dessiner et ce que chacun doit contenir. Les diagrammes eux-mêmes se font dans un outil (StarUML, draw.io ou PlantUML) et seront ajoutés dans `docs/uml/`.

## 5.1 Vue d'ensemble : quel diagramme répond à quelle question ?

| Diagramme | Question à laquelle il répond | Basé sur |
|---|---|---|
| **Cas d'utilisation** | *Qui* fait *quoi* avec l'application ? | Acteurs (§1.6) et besoins fonctionnels (doc 02) |
| **Classes** | Quelles données, et comment sont-elles liées ? | Modèle de données (§4.3) |
| **Séquence** | Dans quel *ordre* les composants échangent-ils pour réaliser un scénario ? | Flux (§4.5) |
| **Activité** | Quelles sont les *étapes* et les *décisions* d'un processus ? | Scénarios clés |

> **Conseil :** l'ordre logique est **cas d'utilisation → classes → séquences → activité**. Chaque diagramme s'appuie sur le précédent, et le professeur vérifiera souvent qu'ils sont **cohérents entre eux** (mêmes noms, mêmes acteurs).

## 5.2 Diagrammes de cas d'utilisation

À produire : **3 diagrammes** (un global, un par module), pour rester lisible.

**a) Global** : les 4 acteurs et les grands paquets (Comptes, Smart Budget, Comparaison des prix, Administration).

**b) Smart Budget** : acteur Utilisateur (et Système pour les alertes).

- Gérer les transactions (ajouter, modifier, supprimer) ← `BUD-03..05`
- Consulter l'historique ← `BUD-06`
- Définir un budget ← `BUD-08, 09`
- Consulter le tableau de bord ← `BUD-10`
- Consulter les statistiques ← `BUD-13, 14`
- *Système* : Envoyer une alerte de budget ← `BUD-11`
- Relation **«include»** : chaque cas « Gérer… » inclut « S'authentifier ».
- Relation **«extend»** : « Recevoir une alerte » étend « Ajouter une dépense » (elle n'a lieu que si un seuil est dépassé).

**c) Comparaison des prix** : acteurs Utilisateur et Administrateur.

- Rechercher un produit ← `PRX-02` ; *«extend»* Scanner un code-barres ← `PRX-03`
- Comparer les prix d'un produit ← `PRX-04`
- Gérer une liste de courses ← `PRX-09, 10`
- Comparer une liste de courses ← `PRX-11, 12` ; *«include»* Vérifier le budget restant ← `LNK-01`
- Enregistrer la liste comme dépense ← `LNK-02`
- Proposer un prix ← `PRX-07`
- *Admin* : Gérer magasins, produits, prix ; Valider les prix proposés ← `ADM-*`, `PRX-08`

> **«include» ou «extend» ?** *«include»* : l'action est **toujours** réalisée (on vérifie toujours le budget quand on compare une liste). *«extend»* : l'action est **parfois** réalisée (on ne scanne pas à chaque recherche).

## 5.3 Fiches de cas d'utilisation (description textuelle)

Un diagramme ne suffit pas : chaque cas **important** doit avoir une fiche. Voici le modèle, rempli pour le cas central du projet. À faire de la même manière pour « Ajouter une dépense », « Définir un budget » et « Comparer les prix d'un produit ».

| Rubrique | Contenu |
|---|---|
| **Nom** | Comparer une liste de courses |
| **ID / besoins couverts** | UC-07 / PRX-11, PRX-12, PRX-13, LNK-01 |
| **Acteur principal** | Utilisateur |
| **Préconditions** | L'utilisateur est connecté ; la liste contient au moins un produit |
| **Scénario nominal** | 1. L'utilisateur ouvre une liste de courses. 2. Il appuie sur « Comparer ». 3. Le système calcule le total dans chaque magasin. 4. Le système affiche les magasins du moins cher au plus cher et met en avant le premier. 5. Le système affiche l'économie par rapport au magasin le plus cher. 6. Le système compare le total au budget « Alimentation » restant et indique s'il est suffisant. |
| **Scénarios alternatifs** | 3a. Un produit n'a pas de prix dans un magasin : ce magasin est marqué « liste incomplète ». 6a. Le total dépasse le budget restant : le système affiche un avertissement avec le montant du dépassement. |
| **Scénario d'erreur** | Pas de connexion réseau : un message invite à réessayer. |
| **Postconditions** | Aucune donnée modifiée (consultation seule) |

## 5.4 Diagramme de classes

Basé sur les 11 entités du §4.3. Il doit montrer :

- les **attributs** avec leur type (`montant : Decimal`, `date : Date`…) ;
- les **associations** avec leurs **multiplicités** (1, *, 0..1) ;
- les **énumérations** : `Role`, `TypeOperation`, `StatutPrix`, `SourcePrix`, `StatutListe`, `TypeAlerte` ;
- **PRIX** comme classe d'association entre PRODUIT et MAGASIN ;
- quelques **méthodes métier** clés, par exemple `Budget.resteADepenser()`, `ListeCourses.totalParMagasin()`, `Budget.tauxConsommation()`.

## 5.5 Diagrammes de séquence

À produire : **3 scénarios**, ceux que l'on montrera pendant la démonstration.

| # | Scénario | Participants |
|---|---|---|
| SD-1 | S'authentifier (connexion, obtention du jeton JWT) | Utilisateur, App Flutter, API, Oracle |
| SD-2 | Ajouter une dépense **et** déclencher l'alerte à 80 % | Utilisateur, App Flutter, OperationController, BudgetService, Oracle |
| SD-3 | Comparer une liste de courses avec vérification du budget | Utilisateur, App Flutter, ListeController, ComparaisonService, BudgetService, Oracle |

> Pour SD-2, il faut utiliser un fragment **`alt`** : `[consommé ≥ 80 %]` créer une alerte, `[sinon]` rien. Pour SD-3, un fragment **`loop`** : pour chaque magasin, calculer le total.

## 5.6 Diagramme d'activité

À produire : **1 diagramme** sur le parcours complet des courses, qui relie les deux modules.

```
(début) → Créer/ouvrir une liste → Ajouter des produits → Comparer
   → <le total dépasse le budget ?>
        oui → Afficher un avertissement → <modifier la liste ?> oui → retour à « Ajouter des produits »
                                                                non → continuer
        non → continuer
   → Choisir le magasin recommandé → Faire les courses
   → Enregistrer la liste comme dépense → Mettre à jour le budget
   → <seuil de 80 % ou 100 % atteint ?> oui → Envoyer une alerte
   → (fin)
```

## 5.7 Bonus : diagramme de déploiement

Facultatif, mais apprécié : téléphone Android (application Flutter) → serveur (Spring Boot) → serveur de base de données (Oracle 19c).
