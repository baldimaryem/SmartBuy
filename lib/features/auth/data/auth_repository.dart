import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../../../shared/mock/mock_store.dart';
import 'utilisateur.dart';

/// Contrat d'accès aux comptes. Deux implémentations :
/// - [ApiAuthRepository] : la vraie API Spring Boot (suivi 2) ;
/// - [MockAuthRepository] : données en mémoire (suivi 1).
///
/// Pourquoi une interface ? Les écrans et le contrôleur ne savent pas d'où
/// viennent les données : on change de source sans toucher à l'interface graphique.
///
/// Inscription en 2 étapes (e-mail réel obligatoire) :
///   [register] crée le compte et envoie un code par e-mail → [verifierEmail] confirme et connecte.
abstract interface class AuthRepository {
  /// Lance [ApiException] 403 si l'e-mail n'est pas encore confirmé.
  Future<Session> login(String email, String motDePasse);

  /// Crée le compte (non confirmé) et envoie un code. Renvoie l'adresse normalisée.
  Future<String> register(String nom, String email, String motDePasse);

  /// Confirme l'adresse avec le code reçu, puis connecte.
  Future<Session> verifierEmail(String email, String code);

  /// Renvoie un nouveau code de confirmation.
  Future<String> renvoyerCode(String email);

  Future<Utilisateur> modifierProfil({required String nom, required String devise, double? revenuMensuel});
  void deconnecter();

  /// AUTH-05 étape 1 : envoie un code à 6 chiffres par e-mail. Renvoie le message du serveur.
  Future<String> demanderCodeReinitialisation(String email);

  /// AUTH-05 étape 2 : remplace le mot de passe si le code est bon.
  Future<String> reinitialiserMotDePasse({required String email, required String code, required String nouveauMotDePasse});
}

class ApiAuthRepository implements AuthRepository {
  ApiAuthRepository(this._api);
  final ApiClient _api;

  Session _session(dynamic json) {
    final session = Session(
      json['token'] as String,
      Utilisateur.fromJson(json['utilisateur'] as Map<String, dynamic>),
    );
    _api.token = session.token;
    return session;
  }

  @override
  Future<Session> login(String email, String motDePasse) async =>
      _session(await _api.post('/auth/login', body: {'email': email, 'motDePasse': motDePasse}));

  @override
  Future<String> register(String nom, String email, String motDePasse) async =>
      (await _api.post('/auth/register', body: {'nom': nom, 'email': email, 'motDePasse': motDePasse}))['email'] as String;

  @override
  Future<Session> verifierEmail(String email, String code) async =>
      _session(await _api.post('/auth/verifier-email', body: {'email': email.trim(), 'code': code.trim()}));

  @override
  Future<String> renvoyerCode(String email) async =>
      (await _api.post('/auth/renvoyer-code', body: {'email': email.trim()}))['message'] as String;

  @override
  Future<Utilisateur> modifierProfil({required String nom, required String devise, double? revenuMensuel}) async =>
      Utilisateur.fromJson(await _api.put('/auth/me',
          body: {'nom': nom, 'devise': devise, 'revenuMensuel': revenuMensuel}) as Map<String, dynamic>);

  @override
  void deconnecter() => _api.token = null;

  @override
  Future<String> demanderCodeReinitialisation(String email) async =>
      (await _api.post('/auth/mot-de-passe-oublie', body: {'email': email.trim()}))['message'] as String;

  @override
  Future<String> reinitialiserMotDePasse({required String email, required String code, required String nouveauMotDePasse}) async =>
      (await _api.post('/auth/reinitialiser-mot-de-passe',
          body: {'email': email.trim(), 'code': code.trim(), 'nouveauMotDePasse': nouveauMotDePasse}))['message'] as String;
}

/// En maquette, aucun e-mail n'est envoyé : le code est toujours [MockStore.codeMaquette] (123456).
class MockAuthRepository implements AuthRepository {
  MockAuthRepository(this._db);
  final MockStore _db;

  static const _codeInvalide = 'Code invalide ou expiré. Demandez un nouveau code.';

  @override
  Future<Session> login(String email, String motDePasse) async {
    await _db.latence();
    final compte = _db.comptes[email.trim().toLowerCase()];
    if (compte == null || compte.motDePasse != motDePasse) {
      throw ApiException('E-mail ou mot de passe incorrect', status: 401);
    }
    if (!compte.verifie) {
      throw ApiException('Adresse e-mail non confirmée : saisissez le code reçu par e-mail.', status: 403);
    }
    _db.utilisateurCourant = compte.utilisateur;
    return Session('jeton-simule', compte.utilisateur);
  }

  @override
  Future<String> register(String nom, String email, String motDePasse) async {
    await _db.latence();
    final cle = email.trim().toLowerCase();
    final existant = _db.comptes[cle];
    if (existant != null && existant.verifie) {
      throw ApiException('Un compte existe déjà avec cet e-mail', status: 409);
    }
    final role = cle == _db.emailAdmin.trim().toLowerCase() ? 'ADMIN' : 'USER';
    final u = Utilisateur(id: existant?.utilisateur.id ?? _db.nextId(), nom: nom, email: cle, role: role);
    _db.comptes[cle] = (utilisateur: u, motDePasse: motDePasse, verifie: false);
    return cle;
  }

  @override
  Future<Session> verifierEmail(String email, String code) async {
    await _db.latence();
    final cle = email.trim().toLowerCase();
    final compte = _db.comptes[cle];
    if (compte == null || code.trim() != MockStore.codeMaquette) throw ApiException(_codeInvalide, status: 400);
    _db.comptes[cle] = (utilisateur: compte.utilisateur, motDePasse: compte.motDePasse, verifie: true);
    _db.utilisateurCourant = compte.utilisateur;
    return Session('jeton-simule', compte.utilisateur);
  }

  @override
  Future<String> renvoyerCode(String email) async {
    await _db.latence();
    return 'Mode maquette : aucun e-mail envoyé, utilisez le code ${MockStore.codeMaquette}.';
  }

  @override
  Future<Utilisateur> modifierProfil({required String nom, required String devise, double? revenuMensuel}) async {
    await _db.latence();
    final u = _db.utilisateurCourant!.copyWith(nom: nom, devise: devise, revenuMensuel: revenuMensuel);
    final compte = _db.comptes[u.email]!;
    _db.comptes[u.email] = (utilisateur: u, motDePasse: compte.motDePasse, verifie: compte.verifie);
    _db.utilisateurCourant = u;
    return u;
  }

  @override
  void deconnecter() => _db.utilisateurCourant = null;

  @override
  Future<String> demanderCodeReinitialisation(String email) => renvoyerCode(email);

  @override
  Future<String> reinitialiserMotDePasse({required String email, required String code, required String nouveauMotDePasse}) async {
    await _db.latence();
    final cle = email.trim().toLowerCase();
    final compte = _db.comptes[cle];
    if (compte == null || code.trim() != MockStore.codeMaquette) throw ApiException(_codeInvalide, status: 400);
    _db.comptes[cle] = (utilisateur: compte.utilisateur, motDePasse: nouveauMotDePasse, verifie: true);
    return 'Mot de passe modifié. Vous pouvez vous connecter.';
  }
}
