import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import 'api_exception.dart';

/// Client HTTP unique de l'application.
///
/// Responsabilités (et seulement celles-ci) :
/// - préfixer les routes avec l'URL de l'API ;
/// - ajouter l'en-tête `Authorization: Bearer <jeton>` ;
/// - encoder / décoder le JSON ;
/// - transformer toute erreur en [ApiException] avec un message en français.
///
/// Les écrans ne l'utilisent jamais directement : ils passent par les
/// repositories de chaque fonctionnalité (couche « data »).
class ApiClient {
  ApiClient({required this.baseUrl, http.Client? client}) : _client = client ?? http.Client();

  final String baseUrl;
  final http.Client _client;

  /// Jeton JWT reçu à la connexion (gardé en mémoire).
  String? token;

  /// Appelé quand le serveur répond 401 (jeton expiré) pour déconnecter l'utilisateur.
  void Function()? onNonAutorise;

  static const _timeout = Duration(seconds: 15);

  /// [timeout] (ici et dans [post]) : délai propre à cet appel. Utile quand le serveur interroge
  /// des sites marchands à 1 requête/s : la réponse dépasse alors largement les 15 s habituelles.
  Future<dynamic> get(String path, {Map<String, Object?>? query, Duration? timeout}) =>
      _send('GET', path, query: query, timeout: timeout);

  Future<dynamic> post(String path, {Object? body, Map<String, Object?>? query, Duration? timeout}) =>
      _send('POST', path, body: body, query: query, timeout: timeout);

  Future<dynamic> put(String path, {Object? body}) => _send('PUT', path, body: body);

  Future<dynamic> delete(String path) => _send('DELETE', path);

  Future<dynamic> _send(String method, String path,
      {Object? body, Map<String, Object?>? query, Duration? timeout}) async {
    final params = <String, String>{
      for (final e in (query ?? const {}).entries)
        if (e.value != null && '${e.value}'.isNotEmpty) e.key: '${e.value}',
    };
    final uri = Uri.parse('$baseUrl$path').replace(queryParameters: params.isEmpty ? null : params);

    final request = http.Request(method, uri)
      ..headers['Content-Type'] = 'application/json; charset=utf-8'
      ..headers['Accept'] = 'application/json';
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    if (body != null) request.body = jsonEncode(body);

    final http.Response response;
    try {
      response = await http.Response.fromStream(await _client.send(request).timeout(timeout ?? _timeout));
    } on TimeoutException {
      throw ApiException('Le serveur ne répond pas. Vérifiez que l\'API est démarrée.');
    } on http.ClientException {
      // Le paquet http transforme les erreurs réseau (serveur éteint, pas de Wi-Fi) en ClientException.
      throw ApiException('Impossible de joindre le serveur ($baseUrl).');
    }

    final texte = utf8.decode(response.bodyBytes);
    final donnees = texte.isEmpty ? null : jsonDecode(texte);

    if (response.statusCode >= 200 && response.statusCode < 300) return donnees;

    if (response.statusCode == 401 && token != null) onNonAutorise?.call();
    final message = donnees is Map && donnees['message'] is String
        ? donnees['message'] as String
        : 'Erreur ${response.statusCode}';
    throw ApiException(message, status: response.statusCode);
  }
}
