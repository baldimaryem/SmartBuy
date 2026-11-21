import 'package:flutter/foundation.dart';

import '../data/auth_repository.dart';
import '../data/utilisateur.dart';

/// État de la session : qui est connecté ?
///
/// C'est un [ChangeNotifier] : quand l'utilisateur se connecte ou se déconnecte,
/// `notifyListeners()` prévient les widgets **et** le routeur (qui redirige
/// automatiquement vers l'écran de connexion ou l'accueil).
class AuthController extends ChangeNotifier {
  AuthController(this._repo);
  final AuthRepository _repo;

  Utilisateur? _utilisateur;
  bool _chargement = false;

  Utilisateur? get utilisateur => _utilisateur;
  bool get connecte => _utilisateur != null;
  bool get chargement => _chargement;
  bool get estAdmin => _utilisateur?.estAdmin ?? false;

  Future<void> login(String email, String motDePasse) =>
      _executer(() async => _utilisateur = (await _repo.login(email.trim(), motDePasse)).utilisateur);

  /// Étape 1 de l'inscription : le compte est créé mais PAS connecté. Renvoie l'adresse à confirmer.
  Future<String> register(String nom, String email, String motDePasse) async {
    late String adresse;
    await _executer(() async => adresse = await _repo.register(nom.trim(), email.trim(), motDePasse));
    return adresse;
  }

  /// Étape 2 : le bon code confirme l'e-mail et connecte (le routeur ouvre alors l'accueil).
  Future<void> verifierEmail(String email, String code) =>
      _executer(() async => _utilisateur = (await _repo.verifierEmail(email, code)).utilisateur);

  Future<String> renvoyerCode(String email) => _repo.renvoyerCode(email);

  Future<void> modifierProfil({required String nom, required String devise, double? revenuMensuel}) =>
      _executer(() async => _utilisateur =
          await _repo.modifierProfil(nom: nom, devise: devise, revenuMensuel: revenuMensuel));

  Future<String> demanderCodeReinitialisation(String email) => _repo.demanderCodeReinitialisation(email);

  Future<String> reinitialiserMotDePasse({required String email, required String code, required String nouveauMotDePasse}) =>
      _repo.reinitialiserMotDePasse(email: email, code: code, nouveauMotDePasse: nouveauMotDePasse);

  void deconnecter() {
    _repo.deconnecter();
    _utilisateur = null;
    notifyListeners();
  }

  /// Exécute une action en gérant l'indicateur de chargement.
  /// Les erreurs sont relancées : l'écran les affiche dans un SnackBar.
  Future<void> _executer(Future<void> Function() action) async {
    _chargement = true;
    notifyListeners();
    try {
      await action();
    } finally {
      _chargement = false;
      notifyListeners();
    }
  }
}
