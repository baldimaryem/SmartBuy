import '../../../core/network/api_client.dart';
import '../../../shared/mock/mock_store.dart';
import '../../../shared/utils/formatters.dart';
import '../../budget/data/budget_repository.dart';
import '../logic/conseiller_regles.dart';

enum NiveauConseil { info, attention, alerte }

class Conseil {
  const Conseil({required this.titre, required this.message, this.niveau = NiveauConseil.info});
  final String titre;
  final String message;
  final NiveauConseil niveau;

  factory Conseil.fromJson(Map<String, dynamic> j) => Conseil(
        titre: j['titre'] as String,
        message: j['message'] as String,
        niveau: switch (j['niveau']) {
          'ALERTE' => NiveauConseil.alerte,
          'ATTENTION' => NiveauConseil.attention,
          _ => NiveauConseil.info,
        },
      );
}

/// Résultat produit soit par le modèle d'IA (`IA`), soit par le moteur de règles (`REGLES`).
class ReponseConseils {
  const ReponseConseils(this.source, this.conseils);
  final String source;
  final List<Conseil> conseils;
  bool get parIa => source == 'IA';
}

class ReponseAssistant {
  const ReponseAssistant(this.source, this.reponse);
  final String source;
  final String reponse;
}

/// Fonctions avancées (validation 3) : conseils intelligents, assistant, e-mail du bilan.
abstract interface class IaRepository {
  Future<ReponseConseils> conseils(String mois);
  Future<ReponseAssistant> poserQuestion(String question);
  Future<String> envoyerRapportMensuel(String mois);
}

class ApiIaRepository implements IaRepository {
  ApiIaRepository(this._api);
  final ApiClient _api;

  @override
  Future<ReponseConseils> conseils(String mois) async {
    final j = await _api.get('/ia/conseils', query: {'mois': mois}) as Map<String, dynamic>;
    return ReponseConseils(
      j['source'] as String? ?? 'REGLES',
      [for (final c in j['conseils'] as List) Conseil.fromJson(c as Map<String, dynamic>)],
    );
  }

  @override
  Future<ReponseAssistant> poserQuestion(String question) async {
    final j = await _api.post('/ia/assistant', body: {'question': question}) as Map<String, dynamic>;
    return ReponseAssistant(j['source'] as String? ?? 'REGLES', j['reponse'] as String);
  }

  @override
  Future<String> envoyerRapportMensuel(String mois) async {
    final j = await _api.post('/rapports/mensuel', query: {'mois': mois}) as Map<String, dynamic>;
    return j['destinataire'] as String? ?? '';
  }
}

/// En mode maquette, seul le moteur de règles est disponible (pas de serveur, donc pas de modèle d'IA).
class MockIaRepository implements IaRepository {
  MockIaRepository(this._db, this._budget);
  final MockStore _db;
  final BudgetRepository _budget;

  Future<ConseillerRegles> _conseiller(String mois) async {
    final historique = [for (var i = 1; i <= 3; i++) await _budget.dashboard(Fmt.decalerMois(mois, -i))];
    return ConseillerRegles(
      courant: await _budget.dashboard(mois),
      budgets: await _budget.budgets(mois),
      moisPrecedents: historique,
    );
  }

  @override
  Future<ReponseConseils> conseils(String mois) async =>
      ReponseConseils('REGLES', (await _conseiller(mois)).conseils());

  @override
  Future<ReponseAssistant> poserQuestion(String question) async =>
      ReponseAssistant('REGLES', (await _conseiller(Fmt.isoMois(DateTime.now()))).repondre(question));

  @override
  Future<String> envoyerRapportMensuel(String mois) async {
    await _db.latence();
    return _db.utilisateurCourant?.email ?? '';
  }
}
