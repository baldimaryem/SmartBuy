import 'package:flutter/foundation.dart';

/// Configuration de l'application, choisie au lancement avec `--dart-define`.
///
/// Exemples :
///   flutter run --dart-define=USE_MOCK=true                       (suivi 1 : données simulées)
///   flutter run --dart-define=API_URL=http://10.0.2.2:8080/api    (suivi 2 : vraie API + Oracle)
///
/// Pourquoi `--dart-define` ? La valeur est fixée à la compilation : aucun fichier
/// à modifier pour passer du mode maquette au mode réel, et pas de secret dans le code.
class AppConfig {
  AppConfig._();

  /// `true` : les dépôts (repositories) lisent des données en mémoire, sans serveur.
  static const bool useMock = bool.fromEnvironment('USE_MOCK', defaultValue: false);

  static const String _apiUrl = String.fromEnvironment('API_URL');

  /// URL de l'API Spring Boot. Sur l'émulateur Android, `localhost` désigne le
  /// téléphone lui-même : la machine hôte est joignable via `10.0.2.2`.
  static String get apiUrl {
    if (_apiUrl.isNotEmpty) return _apiUrl;
    final estAndroid = !kIsWeb && defaultTargetPlatform == TargetPlatform.android;
    return estAndroid ? 'http://10.0.2.2:8080/api' : 'http://localhost:8080/api';
  }

  static const String devise = 'TND';

  /// Mode maquette uniquement : adresse qui devient administrateur à l'inscription
  /// (avec l'API, c'est `smartbuy.admin.email` dans backend/config/application.yml).
  static const String adminEmail = String.fromEnvironment('ADMIN_EMAIL');
}
