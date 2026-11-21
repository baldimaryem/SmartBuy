import 'package:flutter/material.dart';

/// Sur un grand écran (Chrome, Windows), affiche l'application dans un cadre de téléphone centré.
///
/// Pourquoi ? SmartBuy est conçue pour le mobile. Dans un navigateur plein écran, les listes et les
/// boutons s'étirent sur 1 900 px et l'interface paraît vide. Le cadre garde la mise en page prévue
/// (≈ 400 px de large) pendant la démonstration sur PC. Sur un vrai téléphone, il est invisible.
class CadreMobile extends StatelessWidget {
  const CadreMobile({super.key, required this.child});
  final Widget child;

  static const double largeurTelephone = 412; // largeur logique d'un smartphone Android courant
  static const double seuil = 700; // en dessous, on est déjà sur un écran de téléphone ou de petite tablette

  @override
  Widget build(BuildContext context) {
    final media = MediaQuery.of(context);
    if (media.size.width < seuil) return child;

    final hauteur = (media.size.height - 48).clamp(560.0, 915.0);
    final schema = Theme.of(context).colorScheme;
    return ColoredBox(
      color: schema.surfaceContainerHighest,
      child: Center(
        child: Container(
          width: largeurTelephone,
          height: hauteur,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(36),
            border: Border.all(color: Colors.black87, width: 10),
            boxShadow: const [BoxShadow(blurRadius: 30, color: Colors.black26, offset: Offset(0, 12))],
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(26),
            // On fait croire à l'application qu'elle tourne sur un écran de téléphone :
            // les feuilles du bas, dialogues et mises en page utilisent cette taille.
            child: MediaQuery(
              data: media.copyWith(size: Size(largeurTelephone - 20, hauteur - 20), padding: EdgeInsets.zero),
              child: child,
            ),
          ),
        ),
      ),
    );
  }
}
