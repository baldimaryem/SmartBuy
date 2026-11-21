/// Erreur métier ou technique, avec un message lisible par l'utilisateur.
///
/// Le back-end renvoie toujours `{ "status": 400, "message": "..." }` : on garde
/// ce message tel quel pour l'afficher dans un SnackBar.
class ApiException implements Exception {
  ApiException(this.message, {this.status});

  final String message;
  final int? status;

  bool get nonAutorise => status == 401;

  @override
  String toString() => message;
}
