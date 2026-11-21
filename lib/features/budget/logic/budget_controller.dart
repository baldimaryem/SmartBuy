import 'package:flutter/foundation.dart';

import '../../../shared/utils/formatters.dart';
import '../data/budget_models.dart';
import '../data/budget_repository.dart';

/// État du module Smart Budget pour le **mois sélectionné**.
///
/// Les écrans (accueil, transactions, budgets, statistiques) lisent cet état avec
/// `context.watch<BudgetController>()` et se redessinent à chaque `notifyListeners()`.
/// Après chaque modification, on recharge depuis la source (API ou maquette) :
/// c'est le serveur qui fait foi pour les calculs (reste, alertes…).
class BudgetController extends ChangeNotifier {
  BudgetController(this._repo);
  final BudgetRepository _repo;

  String mois = Fmt.isoMois(DateTime.now());
  Dashboard? dashboard;
  List<Operation> operations = [];
  List<Budget> budgets = [];
  List<Categorie> categories = [];
  List<Alerte> alertes = [];

  bool chargement = false;
  String? erreur;

  // Filtres de l'historique (BUD-06)
  TypeOperation? filtreType;
  int? filtreCategorieId;

  List<Categorie> categoriesDe(TypeOperation type) => categories.where((c) => c.type == type).toList();
  int get alertesNonLues => alertes.where((a) => !a.lue).length;

  Future<void> charger() async {
    chargement = true;
    erreur = null;
    notifyListeners();
    try {
      // Les appels indépendants partent en parallèle : l'écran s'affiche plus vite.
      final resultats = await Future.wait([
        _repo.dashboard(mois),
        _repo.operations(mois: mois, type: filtreType, categorieId: filtreCategorieId),
        _repo.budgets(mois),
        if (categories.isEmpty) _repo.categories(),
        _repo.alertes(),
      ]);
      dashboard = resultats[0] as Dashboard;
      operations = resultats[1] as List<Operation>;
      budgets = resultats[2] as List<Budget>;
      if (resultats.length == 5) categories = resultats[3] as List<Categorie>;
      alertes = resultats.last as List<Alerte>;
    } catch (e) {
      erreur = e.toString();
    } finally {
      chargement = false;
      notifyListeners();
    }
  }

  Future<void> changerMois(int delta) {
    mois = Fmt.decalerMois(mois, delta);
    return charger();
  }

  Future<void> filtrer({TypeOperation? type, int? categorieId}) {
    filtreType = type;
    filtreCategorieId = categorieId;
    return charger();
  }

  /// Ajout ou modification (BUD-03, BUD-04, BUD-05). Renvoie les nouvelles alertes éventuelles.
  Future<List<Alerte>> enregistrerOperation(Operation o) async {
    final avant = alertes.map((a) => a.id).toSet();
    await _repo.enregistrerOperation(o);
    await charger();
    return alertes.where((a) => !avant.contains(a.id)).toList();
  }

  Future<void> supprimerOperation(int id) async {
    await _repo.supprimerOperation(id);
    await charger();
  }

  Future<void> definirBudget({int? categorieId, required double plafond}) async {
    await _repo.definirBudget(mois: mois, categorieId: categorieId, montantPlafond: plafond);
    await charger();
  }

  Future<void> supprimerBudget(int id) async {
    await _repo.supprimerBudget(id);
    await charger();
  }

  Future<void> enregistrerCategorie({int? id, required String nom, required TypeOperation type, required String icone}) async {
    await _repo.enregistrerCategorie(id: id, nom: nom, type: type, icone: icone);
    categories = await _repo.categories();
    notifyListeners();
  }

  Future<void> supprimerCategorie(int id) async {
    await _repo.supprimerCategorie(id);
    categories = await _repo.categories();
    notifyListeners();
  }

  Future<void> marquerLue(Alerte a) async {
    if (a.lue) return;
    final maj = await _repo.marquerAlerteLue(a.id);
    alertes = [for (final x in alertes) x.id == a.id ? maj : x];
    notifyListeners();
  }

  /// Appelé à la déconnexion : on ne garde rien de l'utilisateur précédent.
  void reinitialiser() {
    mois = Fmt.isoMois(DateTime.now());
    dashboard = null;
    operations = [];
    budgets = [];
    categories = [];
    alertes = [];
    filtreType = null;
    filtreCategorieId = null;
    erreur = null;
  }
}
