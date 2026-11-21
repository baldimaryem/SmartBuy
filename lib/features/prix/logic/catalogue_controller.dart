import 'dart:async';

import 'package:flutter/foundation.dart';

import '../data/prix_models.dart';
import '../data/prix_repository.dart';

/// État du catalogue : magasins, univers choisi, rayons et résultats de recherche (PRX-01, PRX-02, PRX-03).
class CatalogueController extends ChangeNotifier {
  CatalogueController(this.repo);
  final PrixRepository repo;

  List<Magasin> magasins = [];
  List<CategorieProduit> categories = [];
  List<Produit> resultats = [];

  /// Univers affiché. `null` = aucun choix encore (écran d'accueil de l'onglet Prix) ;
  /// pour l'administration, `null` signifie « tous les univers ».
  Univers? univers;
  String recherche = '';
  int? categorieId;
  bool chargement = false;
  String? erreur;

  Timer? _anti;

  Future<void> charger() async {
    chargement = true;
    erreur = null;
    notifyListeners();
    try {
      final r = await Future.wait([repo.magasins(), repo.categoriesProduits(univers: univers)]);
      magasins = r[0] as List<Magasin>;
      categories = r[1] as List<CategorieProduit>;
      await _rechercher();
    } catch (e) {
      erreur = e.toString();
    } finally {
      chargement = false;
      notifyListeners();
    }
  }

  /// Changer d'univers recharge ses rayons : les rayons d'un autre univers n'ont pas de sens ici.
  Future<void> choisirUnivers(Univers? u) async {
    if (u == univers && categories.isNotEmpty) return;
    univers = u;
    categorieId = null;
    await charger();
  }

  /// Recherche « au fil de la frappe » avec un petit délai (anti-rebond) :
  /// on n'interroge pas le serveur à chaque lettre, seulement après 300 ms sans frappe.
  void rechercher(String texte) {
    recherche = texte;
    _anti?.cancel();
    _anti = Timer(const Duration(milliseconds: 300), _rechercherEtNotifier);
  }

  void filtrerCategorie(int? id) {
    categorieId = id;
    _rechercherEtNotifier();
  }

  /// PRX-03 : recherche par code-barres (catalogue, puis Open Food Facts côté serveur).
  /// Lève une `ApiException` 404 si le code est inconnu partout.
  Future<ResultatCodeBarres> parCodeBarres(String code) async {
    final r = await repo.produitParCodeBarres(code.trim());
    // Un produit qui vient d'entrer au catalogue doit apparaître dans la liste affichée.
    if (r.ajouteAuCatalogue) unawaited(_rechercherEtNotifier());
    return r;
  }

  /// Contribution d'un utilisateur : ajoute un produit introuvable au catalogue.
  Future<Produit> contribuer({
    required String nom,
    required String marque,
    required String unite,
    required String? codeBarres,
    required int categorieProduitId,
  }) async {
    final p = await repo.contribuerProduit(
      nom: nom, marque: marque, unite: unite, codeBarres: codeBarres, categorieProduitId: categorieProduitId,
    );
    unawaited(_rechercherEtNotifier());
    return p;
  }

  Future<void> _rechercherEtNotifier() async {
    try {
      await _rechercher();
      erreur = null;
    } catch (e) {
      erreur = e.toString();
    }
    notifyListeners();
  }

  Future<void> _rechercher() async {
    resultats = await repo.produits(recherche: recherche, categorieId: categorieId, univers: univers);
  }

  @override
  void dispose() {
    _anti?.cancel();
    super.dispose();
  }
}
