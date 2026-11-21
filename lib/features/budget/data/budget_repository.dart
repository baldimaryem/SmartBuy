import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../../../shared/mock/mock_store.dart';
import '../../../shared/utils/formatters.dart';
import '../logic/budget_calculs.dart';
import 'budget_models.dart';

/// Accès aux données du module Smart Budget (BUD-01..16).
abstract interface class BudgetRepository {
  Future<List<Categorie>> categories({TypeOperation? type});
  Future<Categorie> enregistrerCategorie({int? id, required String nom, required TypeOperation type, required String icone});
  Future<void> supprimerCategorie(int id);

  Future<List<Operation>> operations({required String mois, TypeOperation? type, int? categorieId});
  Future<Operation> enregistrerOperation(Operation operation);
  Future<void> supprimerOperation(int id);

  Future<List<Budget>> budgets(String mois);
  Future<Budget> definirBudget({required String mois, int? categorieId, required double montantPlafond});
  Future<void> supprimerBudget(int id);

  Future<Dashboard> dashboard(String mois);

  Future<List<Alerte>> alertes();
  Future<Alerte> marquerAlerteLue(int id);
}

// =========================================================================== API
class ApiBudgetRepository implements BudgetRepository {
  ApiBudgetRepository(this._api);
  final ApiClient _api;

  List<T> _liste<T>(dynamic json, T Function(Map<String, dynamic>) f) =>
      [for (final e in json as List) f(e as Map<String, dynamic>)];

  @override
  Future<List<Categorie>> categories({TypeOperation? type}) async =>
      _liste(await _api.get('/categories', query: {'type': type?.code}), Categorie.fromJson);

  @override
  Future<Categorie> enregistrerCategorie({int? id, required String nom, required TypeOperation type, required String icone}) async {
    final corps = {'nom': nom, 'type': type.code, 'icone': icone};
    final json = id == null ? await _api.post('/categories', body: corps) : await _api.put('/categories/$id', body: corps);
    return Categorie.fromJson(json as Map<String, dynamic>);
  }

  @override
  Future<void> supprimerCategorie(int id) => _api.delete('/categories/$id');

  @override
  Future<List<Operation>> operations({required String mois, TypeOperation? type, int? categorieId}) async => _liste(
      await _api.get('/operations', query: {'mois': mois, 'type': type?.code, 'categorieId': categorieId}),
      Operation.fromJson);

  @override
  Future<Operation> enregistrerOperation(Operation o) async {
    final json = o.id == 0
        ? await _api.post('/operations', body: o.toRequete())
        : await _api.put('/operations/${o.id}', body: o.toRequete());
    return Operation.fromJson(json as Map<String, dynamic>);
  }

  @override
  Future<void> supprimerOperation(int id) => _api.delete('/operations/$id');

  @override
  Future<List<Budget>> budgets(String mois) async => _liste(await _api.get('/budgets', query: {'mois': mois}), Budget.fromJson);

  @override
  Future<Budget> definirBudget({required String mois, int? categorieId, required double montantPlafond}) async =>
      Budget.fromJson(await _api.post('/budgets',
          body: {'mois': mois, 'categorieId': categorieId, 'montantPlafond': montantPlafond}) as Map<String, dynamic>);

  @override
  Future<void> supprimerBudget(int id) => _api.delete('/budgets/$id');

  @override
  Future<Dashboard> dashboard(String mois) async =>
      Dashboard.fromJson(await _api.get('/dashboard', query: {'mois': mois}) as Map<String, dynamic>);

  @override
  Future<List<Alerte>> alertes() async => _liste(await _api.get('/alertes'), Alerte.fromJson);

  @override
  Future<Alerte> marquerAlerteLue(int id) async =>
      Alerte.fromJson(await _api.put('/alertes/$id/lue') as Map<String, dynamic>);
}

// =========================================================================== MAQUETTE
/// Implémentation en mémoire : reproduit les règles du serveur sur [MockStore].
class MockBudgetRepository implements BudgetRepository {
  MockBudgetRepository(this._db);
  final MockStore _db;

  bool _duMois(Operation o, String mois) => Fmt.isoMois(o.dateOperation) == mois;

  double _depense(String mois, {int? categorieId}) => _db.operations
      .where((o) => o.estDepense && _duMois(o, mois) && (categorieId == null || o.categorieId == categorieId))
      .fold(0.0, (s, o) => s + o.montant);

  @override
  Future<List<Categorie>> categories({TypeOperation? type}) async {
    await _db.latence();
    return _db.categories.where((c) => type == null || c.type == type).toList();
  }

  @override
  Future<Categorie> enregistrerCategorie({int? id, required String nom, required TypeOperation type, required String icone}) async {
    await _db.latence();
    final c = Categorie(id: id ?? _db.nextId(), nom: nom, type: type, icone: icone, personnalisee: true);
    final i = _db.categories.indexWhere((x) => x.id == id);
    if (i >= 0) {
      if (!_db.categories[i].personnalisee) throw ApiException('Les catégories par défaut ne sont pas modifiables');
      _db.categories[i] = c;
    } else {
      _db.categories.add(c);
    }
    return c;
  }

  @override
  Future<void> supprimerCategorie(int id) async {
    await _db.latence();
    if (_db.operations.any((o) => o.categorieId == id)) {
      throw ApiException('Catégorie utilisée par des opérations : suppression impossible', status: 409);
    }
    _db.categories.removeWhere((c) => c.id == id && c.personnalisee);
  }

  @override
  Future<List<Operation>> operations({required String mois, TypeOperation? type, int? categorieId}) async {
    await _db.latence();
    return _db.operations
        .where((o) => _duMois(o, mois) && (type == null || o.type == type) && (categorieId == null || o.categorieId == categorieId))
        .toList()
      ..sort((a, b) => b.dateOperation.compareTo(a.dateOperation));
  }

  @override
  Future<Operation> enregistrerOperation(Operation o) async {
    await _db.latence();
    if (o.montant <= 0) throw ApiException('Le montant doit être positif', status: 400);
    final mois = Fmt.isoMois(o.dateOperation);
    final avant = {for (final b in _budgetsDuMois(mois)) b.id: _depense(mois, categorieId: b.categorieId)};

    final complete = o.copyWith(
      id: o.id == 0 ? _db.nextId() : o.id,
      categorieNom: _db.categorie(o.categorieId).nom,
      magasinNom: _db.magasin(o.magasinId)?.nom,
    );
    final i = _db.operations.indexWhere((x) => x.id == complete.id);
    i >= 0 ? _db.operations[i] = complete : _db.operations.add(complete);

    if (complete.estDepense) _verifierSeuils(mois, avant);
    return complete;
  }

  /// BUD-11 : une alerte par seuil franchi (80 % puis 100 %).
  void _verifierSeuils(String mois, Map<int, double> avant) {
    for (final b in _budgetsDuMois(mois)) {
      final apres = _depense(mois, categorieId: b.categorieId);
      for (final seuil in BudgetCalculs.nouveauxSeuils(avant: avant[b.id] ?? 0, apres: apres, plafond: b.plafond)) {
        final nom = b.categorieId == null ? 'Budget global' : _db.categorie(b.categorieId!).nom;
        _db.alertes.insert(0, Alerte(
          id: _db.nextId(),
          type: seuil == 100 ? 'BUDGET_100' : 'BUDGET_80',
          message: seuil == 100 ? '$nom : budget de $mois dépassé' : '$nom : 80 % du budget de $mois atteint',
          dateAlerte: DateTime.now(),
        ));
      }
    }
  }

  @override
  Future<void> supprimerOperation(int id) async {
    await _db.latence();
    _db.operations.removeWhere((o) => o.id == id);
  }

  Iterable<({int id, String mois, int? categorieId, double plafond})> _budgetsDuMois(String mois) =>
      _db.budgets.where((b) => b.mois == mois);

  @override
  Future<List<Budget>> budgets(String mois) async {
    await _db.latence();
    final liste = [
      for (final b in _budgetsDuMois(mois))
        Budget(
          id: b.id,
          mois: b.mois,
          categorieId: b.categorieId,
          categorieNom: b.categorieId == null ? null : _db.categorie(b.categorieId!).nom,
          montantPlafond: b.plafond,
          depense: _depense(mois, categorieId: b.categorieId),
        ),
    ];
    liste.sort((a, b) => a.estGlobal ? -1 : (b.estGlobal ? 1 : (a.categorieNom ?? '').compareTo(b.categorieNom ?? '')));
    return liste;
  }

  @override
  Future<Budget> definirBudget({required String mois, int? categorieId, required double montantPlafond}) async {
    if (montantPlafond <= 0) throw ApiException('Le plafond doit être positif', status: 400);
    final i = _db.budgets.indexWhere((b) => b.mois == mois && b.categorieId == categorieId);
    final id = i >= 0 ? _db.budgets[i].id : _db.nextId();
    final nouveau = (id: id, mois: mois, categorieId: categorieId, plafond: montantPlafond);
    i >= 0 ? _db.budgets[i] = nouveau : _db.budgets.add(nouveau);
    return (await budgets(mois)).firstWhere((b) => b.id == id);
  }

  @override
  Future<void> supprimerBudget(int id) async {
    await _db.latence();
    _db.budgets.removeWhere((b) => b.id == id);
  }

  @override
  Future<Dashboard> dashboard(String mois) async {
    await _db.latence();
    final revenus = _db.operations.where((o) => !o.estDepense && _duMois(o, mois)).fold(0.0, (s, o) => s + o.montant);
    final depenses = _depense(mois);
    final global = _budgetsDuMois(mois).where((b) => b.categorieId == null).firstOrNull;

    // Prévision (BUD-15) : extrapolation pour le mois en cours, total réel pour un mois passé.
    final debut = Fmt.parseMois(mois);
    final maintenant = DateTime.now();
    final estMoisCourant = Fmt.isoMois(maintenant) == mois;
    final prevision = BudgetCalculs.prevoirFinDeMois(
      depense: depenses,
      jourCourant: estMoisCourant ? maintenant.day : 31,
      joursDansMois: BudgetCalculs.joursDansMois(debut.year, debut.month),
    );

    final parCategorie = <int, double>{};
    for (final o in _db.operations.where((o) => o.estDepense && _duMois(o, mois))) {
      parCategorie[o.categorieId] = (parCategorie[o.categorieId] ?? 0) + o.montant;
    }
    final repartition = [
      for (final e in parCategorie.entries) MontantCategorie(e.key, _db.categorie(e.key).nom, e.value),
    ]..sort((a, b) => b.montant.compareTo(a.montant));

    final evolution = [
      for (var i = 5; i >= 0; i--)
        () {
          final m = Fmt.decalerMois(mois, -i);
          final rev = _db.operations.where((o) => !o.estDepense && _duMois(o, m)).fold(0.0, (s, o) => s + o.montant);
          return MoisResume(m, _depense(m), rev);
        }(),
    ];

    return Dashboard(
      mois: mois,
      totalRevenus: revenus,
      totalDepenses: depenses,
      budgetGlobal: global?.plafond,
      resteADepenser: global == null ? null : global.plafond - depenses,
      prevision: Prevision(prevision, global != null && prevision > global.plafond),
      repartition: repartition,
      evolution: evolution,
      economiesRealisees: _db.operations.where((o) => _duMois(o, mois)).fold(0.0, (s, o) => s + o.economie),
      alertesNonLues: _db.alertes.where((a) => !a.lue).length,
    );
  }

  @override
  Future<List<Alerte>> alertes() async {
    await _db.latence();
    return List.of(_db.alertes)..sort((a, b) => b.dateAlerte.compareTo(a.dateAlerte));
  }

  @override
  Future<Alerte> marquerAlerteLue(int id) async {
    final i = _db.alertes.indexWhere((a) => a.id == id);
    _db.alertes[i] = _db.alertes[i].marquerLue();
    return _db.alertes[i];
  }
}
