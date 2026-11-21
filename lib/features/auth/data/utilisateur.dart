import '../../../shared/utils/formatters.dart';

/// Modèle « Utilisateur » : miroir de l'objet JSON renvoyé par `/auth/me`.
class Utilisateur {
  const Utilisateur({
    required this.id,
    required this.nom,
    required this.email,
    required this.role,
    this.devise = 'TND',
    this.revenuMensuel,
  });

  final int id;
  final String nom;
  final String email;
  final String role;
  final String devise;
  final double? revenuMensuel;

  bool get estAdmin => role == 'ADMIN';

  factory Utilisateur.fromJson(Map<String, dynamic> json) => Utilisateur(
        id: json['id'] as int,
        nom: json['nom'] as String,
        email: json['email'] as String,
        role: json['role'] as String? ?? 'USER',
        devise: json['devise'] as String? ?? 'TND',
        revenuMensuel: toDoubleOrNull(json['revenuMensuel']),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'nom': nom,
        'email': email,
        'role': role,
        'devise': devise,
        'revenuMensuel': revenuMensuel,
      };

  Utilisateur copyWith({String? nom, String? devise, double? revenuMensuel}) => Utilisateur(
        id: id,
        nom: nom ?? this.nom,
        email: email,
        role: role,
        devise: devise ?? this.devise,
        revenuMensuel: revenuMensuel ?? this.revenuMensuel,
      );
}

/// Réponse de `/auth/login` et `/auth/register`.
class Session {
  const Session(this.token, this.utilisateur);
  final String token;
  final Utilisateur utilisateur;
}
