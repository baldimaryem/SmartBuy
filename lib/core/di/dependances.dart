import '../../features/auth/data/auth_repository.dart';
import '../../features/budget/data/budget_repository.dart';
import '../../features/ia/data/ia_repository.dart';
import '../../features/prix/data/prix_repository.dart';
import '../../shared/mock/mock_store.dart';
import '../config/app_config.dart';
import '../network/api_client.dart';

/// Point unique où l'on choisit **d'où viennent les données**.
///
/// - `USE_MOCK=true`  → dépôts « Mock » (mémoire)  : suivi 1, démo sans serveur ;
/// - sinon            → dépôts « Api » (HTTP → Spring Boot → Oracle) : suivi 2.
///
/// C'est l'« injection de dépendances » : les contrôleurs reçoivent un
/// `BudgetRepository` sans savoir lequel. Le reste de l'application est identique.
class Dependances {
  Dependances._(this.auth, this.budget, this.prix, this.ia, this.api);

  final AuthRepository auth;
  final BudgetRepository budget;
  final PrixRepository prix;
  final IaRepository ia;
  final ApiClient? api;

  factory Dependances.creer({bool? mock}) {
    if (mock ?? AppConfig.useMock) return Dependances.maquette();
    final api = ApiClient(baseUrl: AppConfig.apiUrl);
    return Dependances._(
      ApiAuthRepository(api),
      ApiBudgetRepository(api),
      ApiPrixRepository(api),
      ApiIaRepository(api),
      api,
    );
  }

  factory Dependances.maquette({MockStore? store}) {
    final db = store ?? MockStore(emailAdmin: AppConfig.adminEmail);
    final budget = MockBudgetRepository(db);
    return Dependances._(MockAuthRepository(db), budget, MockPrixRepository(db), MockIaRepository(db, budget), null);
  }
}
