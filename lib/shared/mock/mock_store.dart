import 'dart:math';

import '../../core/network/api_exception.dart';
import '../../features/auth/data/utilisateur.dart';
import '../../features/budget/data/budget_models.dart';
import '../../features/prix/data/prix_models.dart';

/// « Fausse base de données » en mémoire, utilisée en mode maquette (suivi 1).
///
/// Elle contient le même jeu de données de démonstration que le script Oracle
/// `database/02_data.sql`, pour que l'application se comporte de la même façon
/// avec ou sans serveur. Les données sont perdues à la fermeture de l'application.
class MockStore {
  MockStore({this.delai = const Duration(milliseconds: 250), this.emailAdmin = ''}) {
    _initialiser();
  }

  /// Délai artificiel : rend visibles les indicateurs de chargement, comme avec un vrai réseau.
  final Duration delai;

  /// Adresse qui reçoit le rôle ADMIN à l'inscription (comme `smartbuy.admin.email` côté serveur).
  final String emailAdmin;

  /// En maquette, aucun e-mail n'est envoyé : le code de confirmation est toujours celui-ci.
  static const codeMaquette = '123456';

  int _sequence = 1000;
  int nextId() => ++_sequence;
  Future<void> latence() => Future.delayed(delai);

  // ---------------------------------------------------------------- comptes
  /// Aucun compte au départ : on s'inscrit avec une adresse réelle, comme avec l'API.
  final Map<String, ({Utilisateur utilisateur, String motDePasse, bool verifie})> comptes = {};
  Utilisateur? utilisateurCourant;

  // ---------------------------------------------------------------- budget
  final List<Categorie> categories = [];
  final List<Operation> operations = [];
  final List<({int id, String mois, int? categorieId, double plafond})> budgets = [];
  final List<Alerte> alertes = [];

  // ---------------------------------------------------------------- prix
  final List<Magasin> magasins = [];
  final List<CategorieProduit> categoriesProduits = [];
  final List<Produit> produits = [];
  final List<Prix> prix = [];
  final List<({int id, String nom, DateTime date, String statut, Univers univers, List<LigneListe> lignes})> listes = [];

  Categorie categorie(int id) => categories.firstWhere((c) => c.id == id);
  Magasin? magasin(int? id) => magasins.where((m) => m.id == id).firstOrNull;
  Produit produit(int id) =>
      produits.firstWhere((p) => p.id == id, orElse: () => throw ApiException('Produit introuvable', status: 404));

  /// Catégorie de budget débitée à l'achat d'une liste de cet univers (« Alimentation », « Vêtements », « High-tech »).
  Categorie categorieDeUnivers(Univers u) =>
      categories.firstWhere((c) => c.type == TypeOperation.depense && c.nom == u.categorieBudget);

  /// Prix actuel = dernier relevé VALIDE pour ce produit dans ce magasin.
  double? prixActuel(int produitId, int magasinId) {
    final releves = prix
        .where((p) => p.produitId == produitId && p.magasinId == magasinId && p.statut == 'VALIDE')
        .toList()
      ..sort((a, b) => b.dateReleve.compareTo(a.dateReleve));
    return releves.isEmpty ? null : releves.first.montant;
  }

  // ---------------------------------------------------------------- données de démonstration
  void _initialiser() {
    const contributeur = 'Sarra'; // auteur fictif d'une proposition de prix en attente

    categories.addAll(const [
      Categorie(id: 1, nom: 'Alimentation', type: TypeOperation.depense, icone: 'shopping_cart'),
      Categorie(id: 2, nom: 'Transport', type: TypeOperation.depense, icone: 'directions_car'),
      Categorie(id: 3, nom: 'Logement', type: TypeOperation.depense, icone: 'home'),
      Categorie(id: 4, nom: 'Loisirs', type: TypeOperation.depense, icone: 'sports_esports'),
      Categorie(id: 5, nom: 'Santé', type: TypeOperation.depense, icone: 'local_hospital'),
      Categorie(id: 6, nom: 'Factures', type: TypeOperation.depense, icone: 'receipt'),
      Categorie(id: 7, nom: 'Autres', type: TypeOperation.depense, icone: 'category'),
      Categorie(id: 8, nom: 'Salaire', type: TypeOperation.revenu, icone: 'work'),
      Categorie(id: 9, nom: 'Bourse', type: TypeOperation.revenu, icone: 'school'),
      Categorie(id: 10, nom: 'Autres revenus', type: TypeOperation.revenu, icone: 'attach_money'),
      Categorie(id: 11, nom: 'Freelance', type: TypeOperation.revenu, icone: 'laptop'),
      Categorie(id: 12, nom: 'Carte resto / tickets', type: TypeOperation.revenu, icone: 'restaurant'),
      Categorie(id: 13, nom: 'Prime / bonus', type: TypeOperation.revenu, icone: 'card_giftcard'),
      Categorie(id: 14, nom: 'Aides et allocations', type: TypeOperation.revenu, icone: 'volunteer_activism'),
      Categorie(id: 15, nom: 'Remboursement', type: TypeOperation.revenu, icone: 'replay'),
      // v3 : une catégorie de dépense par univers de courses (achat d'une liste Vêtements / High-tech).
      Categorie(id: 16, nom: 'Vêtements', type: TypeOperation.depense, icone: 'checkroom'),
      Categorie(id: 17, nom: 'High-tech', type: TypeOperation.depense, icone: 'laptop'),
    ]);

    magasins.addAll(const [
      Magasin(id: 1, nom: 'Carrefour La Marsa', enseigne: 'Carrefour', adresse: 'Zone commerciale La Marsa', ville: 'La Marsa'),
      Magasin(id: 2, nom: 'Monoprix Lafayette', enseigne: 'Monoprix', adresse: 'Avenue de Paris', ville: 'Tunis'),
      Magasin(id: 3, nom: 'Magasin Général Ariana', enseigne: 'Magasin Général', adresse: 'Avenue Habib Bourguiba', ville: 'Ariana'),
      Magasin(id: 4, nom: 'Géant Tunis City', enseigne: 'Géant', adresse: 'Tunis City, route de Bizerte', ville: 'Tunis'),
      Magasin(id: 5, nom: 'Aziza Menzah 6', enseigne: 'Aziza', adresse: 'Rue Hédi Nouira', ville: 'Ariana'),
    ]);

    categoriesProduits.addAll(const [
      CategorieProduit(1, 'Produits laitiers'),
      CategorieProduit(2, 'Épicerie'),
      CategorieProduit(3, 'Boissons'),
      CategorieProduit(4, 'Fruits et légumes'),
      CategorieProduit(5, 'Hygiène'),
      CategorieProduit(6, 'Entretien'),
      CategorieProduit(7, 'Vêtements homme et femme', univers: Univers.vetements),
      CategorieProduit(8, 'Chaussures', univers: Univers.vetements),
      CategorieProduit(9, 'Smartphones', univers: Univers.highTech),
      CategorieProduit(10, 'Ordinateurs portables', univers: Univers.highTech),
      CategorieProduit(11, 'Audio', univers: Univers.highTech),
    ]);

    // nom, marque, code-barres, unité, catégorie, prix de base, magasins sans ce produit
    const catalogue = <(String, String, String, String, int, double, List<int>)>[
      ('Lait demi-écrémé 1L', 'Délice', '6194043001019', 'L', 1, 1.350, []), // vrai code-barres Délice
      ('Yaourt nature x4', 'Vitalait', '6191234500029', 'pièce', 1, 2.100, []),
      ('Beurre 200g', 'Président', '6191234500036', 'pièce', 1, 4.900, [5]),
      ('Fromage Gouda 400g', 'Jadida', '6191234500043', 'pièce', 1, 9.800, [5]),
      ('Œufs x12', 'Mazraa', '6191234500050', 'pièce', 1, 5.400, []),
      ('Pâtes spaghetti 500g', 'Warda', '6191234500067', 'pièce', 2, 1.150, []),
      ('Couscous moyen 1kg', 'Diari', '6191234500074', 'kg', 2, 1.900, []),
      ('Huile d\'olive 1L', 'Zitouna', '6191234500081', 'L', 2, 15.500, [2]),
      ('Sucre 1kg', 'Sucre de Tunisie', '6191234500098', 'kg', 2, 1.400, []),
      ('Tomate concentrée 400g', 'Sicam', '6191234500104', 'pièce', 2, 2.350, []),
      ('Café moulu 250g', 'Bondin', '6191234500111', 'pièce', 2, 6.900, [3]),
      ('Thon à l\'huile 160g', 'El Manar', '6191234500128', 'pièce', 2, 3.600, []),
      ('Eau minérale 1,5L x6', 'Safia', '6191234500135', 'pièce', 3, 4.200, []),
      ('Jus d\'orange 1L', 'Vitalait', '6191234500142', 'L', 3, 3.250, [1]),
      ('Boisson gazeuse 1L', 'Boga', '6191234500159', 'L', 3, 1.700, []),
      ('Pommes de terre 1kg', '—', '6191234500166', 'kg', 4, 1.600, [2]),
      ('Tomates 1kg', '—', '6191234500173', 'kg', 4, 1.900, [2]),
      ('Bananes 1kg', '—', '6191234500180', 'kg', 4, 5.800, []),
      ('Dentifrice 75ml', 'Signal', '6191234500197', 'pièce', 5, 3.900, []),
      ('Shampooing 400ml', 'Elsève', '6191234500203', 'pièce', 5, 11.500, [5]),
      ('Savon x4', 'Palmolive', '6191234500210', 'pièce', 5, 4.300, []),
      ('Lessive liquide 3L', 'Ariel', '6191234500227', 'pièce', 6, 24.900, [5]),
      ('Liquide vaisselle 1L', 'Pril', '6191234500234', 'pièce', 6, 3.700, []),
      ('Eau de javel 1L', 'Javel Lacroix', '6191234500241', 'pièce', 6, 1.250, []),
    ];

    // Écart de prix moyen par enseigne (Aziza = discount, Monoprix = proximité, plus cher).
    const coefMagasin = {1: 1.00, 2: 1.08, 3: 0.97, 4: 0.99, 5: 0.93};
    final hasard = Random(42); // graine fixe : mêmes données à chaque lancement
    final aujourdHui = DateTime.now();
    var idPrix = 1;

    for (var i = 0; i < catalogue.length; i++) {
      final (nom, marque, code, unite, cat, base, absents) = catalogue[i];
      final produit = Produit(
        id: i + 1,
        nom: nom,
        marque: marque,
        codeBarres: code,
        unite: unite,
        categorieProduitId: cat,
        categorieProduitNom: categoriesProduits.firstWhere((c) => c.id == cat).nom,
        // Le lait Délice existe vraiment sur Open Food Facts : montre l'attribution ODbL sur sa fiche.
        source: i == 0 ? 'OPEN_FOOD_FACTS' : 'CATALOGUE',
      );
      produits.add(produit);
      for (final m in magasins) {
        if (absents.contains(m.id)) continue;
        final montant = _millimes(base * coefMagasin[m.id]! * (0.95 + hasard.nextDouble() * 0.10));
        // Ancien relevé (il y a 2 mois), légèrement plus bas : montre l'historique (PRX-05).
        prix.add(Prix(
          id: idPrix++, produitId: produit.id, produitNom: nom, magasinId: m.id, magasinNom: m.nom,
          montant: _millimes(montant * 0.96), dateReleve: aujourdHui.subtract(const Duration(days: 60)),
          source: 'ADMIN', statut: 'VALIDE',
        ));
        prix.add(Prix(
          id: idPrix++, produitId: produit.id, produitNom: nom, magasinId: m.id, magasinNom: m.nom,
          montant: montant, dateReleve: aujourdHui.subtract(Duration(days: 3 + hasard.nextInt(10))),
          source: 'ADMIN', statut: 'VALIDE',
        ));
      }
    }
    // Une proposition d'utilisateur en attente de validation (PRX-07 / PRX-08).
    prix.add(Prix(
      id: idPrix++, produitId: 1, produitNom: produits[0].nom, magasinId: 4, magasinNom: magasins[3].nom,
      montant: 1.250, dateReleve: aujourdHui.subtract(const Duration(days: 1)), proposePar: contributeur,
    ));

    // Ajouts v3 placés APRÈS les données historiques : les identifiants et les prix existants ne bougent pas.
    _ajouterUniversV3(aujourdHui, idPrix);

    _genererOperations(aujourdHui);

    final mois = _mois(aujourdHui);
    budgets.addAll([
      (id: 1, mois: mois, categorieId: null, plafond: 1300.0),
      (id: 2, mois: mois, categorieId: 1, plafond: 420.0),
      (id: 3, mois: mois, categorieId: 2, plafond: 120.0),
      (id: 4, mois: mois, categorieId: 4, plafond: 150.0),
    ]);

    alertes.add(Alerte(
      id: 1,
      type: 'BUDGET_80',
      message: 'Alimentation : 80 % du budget de ${_mois(aujourdHui)} atteint',
      dateAlerte: aujourdHui.subtract(const Duration(hours: 5)),
    ));

    listes.add((
      id: 1,
      nom: 'Courses de la semaine',
      date: aujourdHui.subtract(const Duration(days: 1)),
      statut: 'EN_COURS',
      univers: Univers.alimentation,
      lignes: [
        for (final (pid, q) in const [(1, 6.0), (5, 1.0), (6, 3.0), (8, 1.0), (13, 2.0), (16, 2.0), (23, 1.0)])
          LigneListe(id: nextId(), produitId: pid, produitNom: produits[pid - 1].nom, unite: produits[pid - 1].unite, quantite: q),
      ],
    ));
  }

  /// Univers Vêtements et High-tech, et deux magasins « en ligne » dont les prix sont « collectés ».
  void _ajouterUniversV3(DateTime aujourdHui, int idPrix) {
    magasins.addAll(const [
      Magasin(id: 6, nom: 'Carrefour Tunisie (en ligne)', enseigne: 'Carrefour', enLigne: true, siteWeb: 'https://www.carrefour.tn'),
      Magasin(id: 7, nom: 'Spacenet (en ligne)', enseigne: 'Spacenet', enLigne: true, siteWeb: 'https://spacenet.tn'),
    ]);

    // nom, marque, code-barres, unité, rayon, source, url, prix par magasin
    const catalogue = <(String, String, String?, int, String, String?, Map<int, double>)>[
      ('Jean homme coupe droite', 'Denim Tunis', '6190000000017', 7, 'CATALOGUE', null, {1: 59.900, 2: 69.000, 4: 55.500}),
      ('T-shirt coton col rond', 'Basic', '6190000000024', 7, 'CATALOGUE', null, {1: 14.900, 2: 17.500, 4: 12.900}),
      ('Baskets running', 'Sportix', '6190000000031', 8, 'CATALOGUE', null, {1: 119.000, 4: 109.000}),
      ('Smartphone Galaxy A15 128 Go', 'Samsung', '8806095299999', 9, 'COLLECTE',
          'https://spacenet.tn/smartphone-samsung-galaxy-a15', {1: 649.000, 4: 629.000, 7: 599.000}),
      ('PC portable IdeaPad 15" i5 / 8 Go', 'Lenovo', '0197529999998', 10, 'COLLECTE',
          'https://spacenet.tn/pc-portable-lenovo-ideapad', {3: 1899.000, 4: 1949.000, 7: 1799.000}),
      ('Écouteurs Bluetooth', 'JBL', '6925281999997', 11, 'CATALOGUE', null, {1: 89.000, 4: 85.000, 7: 79.000}),
    ];
    for (final (nom, marque, code, cat, source, url, prixParMagasin) in catalogue) {
      final rayon = categoriesProduits.firstWhere((c) => c.id == cat);
      final produit = Produit(
        id: produits.length + 1, nom: nom, marque: marque, codeBarres: code, unite: 'pièce',
        categorieProduitId: cat, categorieProduitNom: rayon.nom, univers: rayon.univers, source: source, urlSource: url,
      );
      produits.add(produit);
      for (final MapEntry(key: magasinId, value: montant) in prixParMagasin.entries) {
        final enLigne = magasin(magasinId)!.enLigne;
        prix.add(Prix(
          id: idPrix++, produitId: produit.id, produitNom: nom, magasinId: magasinId, magasinNom: magasin(magasinId)!.nom,
          montant: montant, dateReleve: aujourdHui.subtract(Duration(days: enLigne ? 0 : 5)),
          source: enLigne ? 'COLLECTE' : 'ADMIN', statut: 'VALIDE',
        ));
      }
    }

    // Quelques prix « collectés » chez Carrefour en ligne pour des produits alimentaires.
    for (final (produitId, montant) in const [(1, 1.290), (2, 2.050), (6, 1.090), (13, 4.150)]) {
      prix.add(Prix(
        id: idPrix++, produitId: produitId, produitNom: produit(produitId).nom, magasinId: 6, magasinNom: magasin(6)!.nom,
        montant: montant, dateReleve: aujourdHui, source: 'COLLECTE', statut: 'VALIDE',
      ));
    }
  }

  /// Six mois d'historique réaliste pour les graphiques et la prévision.
  void _genererOperations(DateTime aujourdHui) {
    final hasard = Random(7);
    var id = 1;
    for (var decalage = 5; decalage >= 0; decalage--) {
      final debut = DateTime(aujourdHui.year, aujourdHui.month - decalage);
      final dernierJour = decalage == 0 ? aujourdHui.day : DateTime(debut.year, debut.month + 1, 0).day;
      DateTime jour(int j) => DateTime(debut.year, debut.month, min(j, dernierJour));
      void ajouter(TypeOperation t, int cat, double montant, int j, String note, {int? magasinId, bool rec = false}) {
        if (j > dernierJour) return; // pas d'opération dans le futur
        operations.add(Operation(
          id: id++, type: t, montant: _millimes(montant), dateOperation: jour(j), categorieId: cat,
          categorieNom: categorie(cat).nom, note: note, magasinId: magasinId, magasinNom: magasin(magasinId)?.nom,
          recurrente: rec,
        ));
      }

      ajouter(TypeOperation.revenu, 8, 1800, 1, 'Salaire', rec: true);
      if (decalage % 2 == 0) ajouter(TypeOperation.revenu, 10, 150 + hasard.nextInt(100).toDouble(), 12, 'Cours particuliers');
      ajouter(TypeOperation.depense, 3, 550, 2, 'Loyer', rec: true);
      ajouter(TypeOperation.depense, 6, 65 + hasard.nextInt(30).toDouble(), 5, 'STEG / SONEDE');
      ajouter(TypeOperation.depense, 6, 35, 6, 'Abonnement Internet', rec: true);
      for (final j in [3, 8, 13, 17]) {
        ajouter(TypeOperation.depense, 1, 70 + hasard.nextDouble() * 40, j, 'Courses', magasinId: 1 + hasard.nextInt(5));
      }
      for (final j in [4, 11, 19, 26]) {
        ajouter(TypeOperation.depense, 2, 20 + hasard.nextDouble() * 15, j, 'Carburant / taxi');
      }
      // Les loisirs augmentent ce mois-ci : déclenche un conseil de l'IA (BUD-16).
      ajouter(TypeOperation.depense, 4, (decalage == 0 ? 95 : 45) + hasard.nextDouble() * 20, 9, 'Sortie');
      if (decalage == 0) ajouter(TypeOperation.depense, 4, 38, 10, 'Cinéma');
      if (hasard.nextBool()) ajouter(TypeOperation.depense, 5, 25 + hasard.nextDouble() * 40, 15, 'Pharmacie');
      ajouter(TypeOperation.depense, 1, 60 + hasard.nextDouble() * 30, 22, 'Courses', magasinId: 5);
    }
  }

  static String _mois(DateTime d) => '${d.year}-${d.month.toString().padLeft(2, '0')}';
  static double _millimes(double v) => (v * 1000).roundToDouble() / 1000;
}
