# Guide d'utilisation de SmartBuy, étape par étape

## 0. Démarrer (à chaque séance)

1. Vérifier qu'Oracle tourne : le service Windows `OracleServiceORCL` est démarré (c'est automatique au démarrage du PC).
2. **Terminal 1 : l'API**, à laisser ouvert.
   ```bat
   cd /d C:\workspace\flutter_app\backend
   set "JAVA_HOME=C:\Program Files\Java\jdk-17.0.8"
   "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.10-bin\53h08a94dg6djh6umvruv7q564\apache-maven-3.9.10\bin\mvn.cmd" spring-boot:run
   ```
   Attendre la ligne `Started SmartBuyApplication`.
3. **Terminal 2 : l'application.**
   ```bat
   cd /d C:\workspace\flutter_app
   flutter run -d chrome
   ```
   Sur PC, l'application s'affiche dans un **cadre de téléphone** : c'est voulu, l'application est conçue pour le mobile.
4. Pour arrêter : `q` dans le terminal 2, puis `Ctrl+C` dans le terminal 1.

## Où sont les comptes et les mots de passe ?

- Les comptes sont **dans Oracle**, table `UTILISATEUR` du schéma `SMARTBUY` (visible dans SQL Developer, connexion SmartBuy).
- Les mots de passe y sont **hachés avec BCrypt** (colonne `MOT_DE_PASSE_HACHE`, du type `$2a$10$...`). Personne ne peut les relire, même l'administrateur.
- **Aucun compte fictif** : on s'inscrit avec une **adresse e-mail réelle**. Les adresses dont le domaine ne reçoit pas d'e-mails (ex. `@smartbuy.tn`, `@example.com`) sont refusées.
- À l'inscription, un **code à 6 chiffres** est envoyé à l'adresse : sans ce code, impossible de se connecter. C'est la preuve que l'e-mail existe et vous appartient.
- **L'administrateur** est l'adresse déclarée dans `backend/config/application.yml` (`smartbuy.admin.email`). Il s'inscrit normalement et reçoit le rôle ADMIN.

## 1. Comptes

| Étape | Action | Ce qu'on voit |
|---|---|---|
| 1.1 | **Créer un compte** : nom, e-mail **réel**, mot de passe (8 caractères dont un chiffre) | Écran « Confirmer mon e-mail » |
| 1.1b | Ouvrir sa boîte mail (Gmail…), copier le code à 6 chiffres, le saisir puis **Confirmer** | Arrivée sur l'accueil (vide pour un nouveau compte) |
| 1.2 | Se déconnecter (icône profil, puis **Se déconnecter**) et se reconnecter | Retour à l'accueil |
| 1.3 | Se connecter avec un mauvais mot de passe | « E-mail ou mot de passe incorrect » |
| 1.4 | **Mot de passe oublié ?** puis saisir l'e-mail et **Recevoir un code** | Étape 2 : saisie du code |
| 1.5 | Récupérer le code dans **sa boîte mail**. Si Gmail n'est pas encore configuré : dans le **terminal 1**, ligne `>>> CODE VERIFICATION pour ... : 123456` | Code à 6 chiffres, valable 15 minutes |
| 1.6 | Saisir le code et le nouveau mot de passe, puis **Changer mon mot de passe** | « Mot de passe modifié », retour à la connexion |
| 1.7 | Profil : modifier le nom ou le revenu mensuel | « Profil mis à jour » |

## 2. Gérer son budget

| Étape | Action | Ce qu'on voit |
|---|---|---|
| 2.1 | **Accueil** | Solde du mois, reste à dépenser (jauge), prévision de fin de mois, budgets, répartition |
| 2.2 | Flèches ‹ › autour du mois | Les mois précédents |
| 2.3 | Accueil → **Budgets** → **+ Budget** : global 1 200 DT, puis Transport 120 DT | Jauges vertes / orange / rouges |
| 2.4 | **+ Dépense** : 60 DT, Transport, magasin facultatif, puis Enregistrer | La dépense est dans **Opérations** ; le reste à dépenser baisse |
| 2.5 | Ajouter une dépense qui dépasse 80 % puis 100 % d'un budget | Message d'alerte + badge sur la cloche + e-mail d'alerte (si Gmail est configuré) |
| 2.6 | **Opérations** : filtres Dépenses / Revenus / catégorie | Liste filtrée |
| 2.7 | Toucher une opération pour la modifier ; glisser vers la gauche pour la supprimer | Liste et accueil mis à jour |
| 2.8 | Cocher « Récurrente » (loyer, salaire) | Recopiée automatiquement le 1er du mois suivant |
| 2.9 | Accueil → **Statistiques** | Anneau par catégorie, barres sur 6 mois, taux d'épargne, économies |
| 2.10 | Profil → **Mes catégories** → + | Nouvelle catégorie disponible dans le formulaire |

## 3. Comparer les prix

| Étape | Action | Ce qu'on voit |
|---|---|---|
| 3.1 | Onglet **Prix**, taper « lait » | Produits avec « dès X DT » et le magasin le moins cher |
| 3.2 | Puces de rayon (Produits laitiers, Boissons…) | Filtre |
| 3.3 | Icône code-barres, taper `6191234500009` | Fiche du lait demi-écrémé |
| 3.4 | Fiche produit | Prix dans chaque magasin (trophée vert = le moins cher), courbe d'historique |
| 3.5 | **Signaler un prix** : magasin + prix | « Sera vérifié par un administrateur » |
| 3.6 | Icône magasin | Les 5 magasins |

## 4. Liste de courses → meilleur magasin → dépense

| Étape | Action | Ce qu'on voit |
|---|---|---|
| 4.1 | Onglet **Listes** → **+ Liste** : « Test » | Liste vide |
| 4.2 | **+** : ajouter lait, pâtes, eau… ; régler les quantités avec − / + | Lignes de la liste |
| 4.3 | **Comparer les magasins** | Magasin recommandé, économie, magasins incomplets en orange, comparaison avec le budget Alimentation |
| 4.4 | Déplier un magasin | Détail produit par produit |
| 4.5 | **J'ai acheté ici** | Dépense créée dans Alimentation ; liste « achetée » ; économie dans Statistiques |

## 5. Assistant

| Étape | Action | Ce qu'on voit |
|---|---|---|
| 5.1 | Onglet **Assistant** | Conseils du mois (pastille « Règles », ou « IA » si une clé Claude est configurée) |
| 5.2 | Puces « Combien me reste-t-il ? », « Un conseil… » ou une question libre | Réponse chiffrée |
| 5.3 | Icône ✉ en haut | Bilan du mois envoyé par e-mail à l'adresse du compte |

## 6. Administrateur (adresse déclarée dans `smartbuy.admin.email`)

| Étape | Action | Ce qu'on voit |
|---|---|---|
| 6.1 | Profil → **Espace administrateur** → Prix à valider | Le prix signalé à l'étape 3.5 ; ✓ pour valider, ✗ pour rejeter |
| 6.2 | Après validation, rouvrir la fiche du produit | Le nouveau prix est pris en compte |
| 6.3 | Onglets Magasins / Produits / Rayons | Ajouter, modifier, supprimer |
| 6.4 | Connecté avec un compte simple, essayer d'ouvrir `/admin` | Renvoyé à l'accueil (et le serveur répond 403) |

## Vérifier dans Oracle (SQL Developer, connexion SmartBuy)

```sql
SELECT nom, email, role FROM utilisateur;
SELECT * FROM operation ORDER BY id DESC FETCH FIRST 5 ROWS ONLY;
SELECT * FROM alerte ORDER BY id DESC;
SELECT * FROM reinitialisation_mdp ORDER BY id DESC;
```
