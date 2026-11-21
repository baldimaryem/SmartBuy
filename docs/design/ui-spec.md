# Spécification UI : SmartBuy

Style : Material 3, couleur de base vert canard `#0F766E`, cartes arrondies (16 px), thème clair et sombre.
Langue : français. Monnaie : dinar tunisien, 3 décimales (`1 234,500 DT`).
Chaque écran de liste gère trois états : **chargement** (indicateur), **vide** (icône + message + action), **erreur** (message + « Réessayer »).

## Connexion
Logo, titre « SmartBuy », sous-titre. Champs e-mail et mot de passe (œil pour afficher). Bouton « Se connecter ». Lien « Créer un compte ». Carte « Comptes de démonstration » avec deux puces.

## Inscription
Nom, e-mail, mot de passe (8 caractères dont un chiffre), confirmation. Bouton « Créer mon compte ».

## Accueil (onglet 1)
Barre : « Bonjour {prénom} », cloche avec badge, profil. Sélecteur de mois ‹ novembre 2026 ›.
1. Carte colorée « Solde du mois » : grand montant, revenus (vert) et dépenses (rouge).
2. « Reste à dépenser » : montant, jauge (verte, orange à 80 %, rouge à 100 %), ligne de prévision de fin de mois.
3. « Budgets par catégorie » : les 4 plus consommés, avec leurs jauges.
4. « Répartition des dépenses » : anneau et légende en %.
5. Carte « X DT économisés grâce à la comparaison ».
Bouton flottant « + Dépense ».

## Opérations (onglet 2)
Sélecteur de mois, filtre segmenté Tout / Dépenses / Revenus, liste déroulante de catégorie.
Chaque ligne : icône de catégorie, note, date · catégorie · magasin, montant signé et coloré. On glisse vers la gauche pour supprimer.

## Formulaire d'opération
Segment Dépense / Revenu, montant en grand, catégorie (avec icône), date, magasin (dépense), note, interrupteur « Récurrente chaque mois ».

## Prix (onglet 3)
Barre de recherche, puces de rayons défilantes. Chaque produit : nom, marque · rayon, « dès 1,350 DT » et le magasin le moins cher. Actions : code-barres, magasins.

## Fiche produit
Infos produit. Section « Prix par magasin » triée, le meilleur avec un trophée vert. Bouton « Signaler un prix ». Courbe d'historique. Bouton flottant « Ajouter à une liste ».

## Listes (onglet 4)
Listes avec le nombre d'articles et le statut (achetée). Menu ⋮ renommer / supprimer.
Le détail affiche les produits avec − quantité +, et en bas le bouton « Comparer les magasins ».

## Comparaison
Carte « Magasin recommandé » (nom, total, économie). Carte budget : « Compatible » ou « Dépasse votre budget Alimentation ».
Classement : chaque magasin se déplie pour montrer le détail ligne par ligne et le bouton « J'ai acheté ici ». Un magasin incomplet est signalé en orange, avec les produits manquants.

## Assistant (onglet 5)
Carte « Conseils du mois » avec une pastille IA ou Règles. Questions suggérées en puces. Bulles de discussion. Champ de saisie en bas. Action « Recevoir le bilan par e-mail ».

## Administration
Onglets : Prix à valider (✓ / ✗), Magasins, Produits, Rayons (CRUD).
