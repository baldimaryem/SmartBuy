import 'dart:math';

import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';
import '../utils/formatters.dart';

/// Graphiques dessinés à la main avec `CustomPainter`.
///
/// Pourquoi pas une bibliothèque (fl_chart…) ? Trois graphiques simples suffisent,
/// et les dessiner soi-même montre comment Flutter peint à l'écran (Canvas).
/// Cela évite aussi une dépendance de plus.

class Part {
  const Part(this.libelle, this.valeur);
  final String libelle;
  final double valeur;
}

/// BUD-13 : répartition des dépenses (anneau + légende).
class GraphiqueCirculaire extends StatelessWidget {
  const GraphiqueCirculaire({super.key, required this.parts, this.taille = 150});
  final List<Part> parts;
  final double taille;

  @override
  Widget build(BuildContext context) {
    final total = parts.fold(0.0, (s, p) => s + p.valeur);
    if (total <= 0) return const SizedBox(height: 80, child: Center(child: Text('Aucune dépense')));
    return Row(children: [
      SizedBox(
        width: taille,
        height: taille,
        child: CustomPaint(
          painter: _AnneauPainter(parts.map((p) => p.valeur / total).toList()),
          child: Center(
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Text('Total', style: Theme.of(context).textTheme.labelSmall),
              Text(Fmt.montant(total), style: Theme.of(context).textTheme.labelLarge),
            ]),
          ),
        ),
      ),
      const SizedBox(width: 16),
      Expanded(
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          for (var i = 0; i < parts.length; i++)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 3),
              child: Row(children: [
                Container(width: 10, height: 10, decoration: BoxDecoration(color: _couleur(i), shape: BoxShape.circle)),
                const SizedBox(width: 8),
                Expanded(child: Text(parts[i].libelle, overflow: TextOverflow.ellipsis)),
                Text('${(parts[i].valeur / total * 100).toStringAsFixed(0)} %'),
              ]),
            ),
        ]),
      ),
    ]);
  }
}

Color _couleur(int i) => AppTheme.graphique[i % AppTheme.graphique.length];

class _AnneauPainter extends CustomPainter {
  _AnneauPainter(this.fractions);
  final List<double> fractions;

  @override
  void paint(Canvas canvas, Size size) {
    const epaisseur = 22.0;
    final rect = Rect.fromLTWH(epaisseur / 2, epaisseur / 2, size.width - epaisseur, size.height - epaisseur);
    var depart = -pi / 2; // on commence en haut (midi)
    for (var i = 0; i < fractions.length; i++) {
      final angle = fractions[i] * 2 * pi;
      canvas.drawArc(
        rect,
        depart,
        max(angle - 0.03, 0.001), // petit espace entre les parts
        false,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = epaisseur
          ..color = _couleur(i),
      );
      depart += angle;
    }
  }

  @override
  bool shouldRepaint(_AnneauPainter old) => old.fractions != fractions;
}

class Barre {
  const Barre(this.libelle, this.valeurs);
  final String libelle;
  final List<double> valeurs; // une valeur par série
}

/// BUD-14 : évolution sur plusieurs mois (barres groupées, ex. revenus / dépenses).
class GraphiqueBarres extends StatelessWidget {
  const GraphiqueBarres({super.key, required this.barres, required this.series, required this.couleurs, this.hauteur = 180});
  final List<Barre> barres;
  final List<String> series;
  final List<Color> couleurs;
  final double hauteur;

  @override
  Widget build(BuildContext context) {
    final style = Theme.of(context).textTheme.labelSmall!;
    return Column(children: [
      SizedBox(
        height: hauteur,
        child: CustomPaint(
          size: Size.infinite,
          painter: _BarresPainter(barres, couleurs, style, Theme.of(context).colorScheme.outlineVariant),
        ),
      ),
      const SizedBox(height: 8),
      Wrap(spacing: 16, children: [
        for (var i = 0; i < series.length; i++)
          Row(mainAxisSize: MainAxisSize.min, children: [
            Container(width: 10, height: 10, color: couleurs[i]),
            const SizedBox(width: 6),
            Text(series[i], style: style),
          ]),
      ]),
    ]);
  }
}

class _BarresPainter extends CustomPainter {
  _BarresPainter(this.barres, this.couleurs, this.style, this.grille);
  final List<Barre> barres;
  final List<Color> couleurs;
  final TextStyle style;
  final Color grille;

  @override
  void paint(Canvas canvas, Size size) {
    if (barres.isEmpty) return;
    const basLibelles = 18.0;
    final hauteurUtile = size.height - basLibelles;
    final maxi = barres.expand((b) => b.valeurs).fold(0.0, max);
    if (maxi <= 0) return;

    // Lignes de repère horizontales
    final pinceauGrille = Paint()..color = grille..strokeWidth = 1;
    for (var i = 0; i <= 3; i++) {
      final y = hauteurUtile * i / 3;
      canvas.drawLine(Offset(0, y), Offset(size.width, y), pinceauGrille);
    }

    final largeurGroupe = size.width / barres.length;
    final nbSeries = barres.first.valeurs.length;
    final largeurBarre = min(18.0, largeurGroupe * 0.7 / nbSeries);
    for (var g = 0; g < barres.length; g++) {
      final debut = g * largeurGroupe + (largeurGroupe - largeurBarre * nbSeries) / 2;
      for (var s = 0; s < nbSeries; s++) {
        final h = barres[g].valeurs[s] / maxi * hauteurUtile;
        final rect = RRect.fromRectAndCorners(
          Rect.fromLTWH(debut + s * largeurBarre, hauteurUtile - h, largeurBarre - 2, h),
          topLeft: const Radius.circular(4),
          topRight: const Radius.circular(4),
        );
        canvas.drawRRect(rect, Paint()..color = couleurs[s % couleurs.length]);
      }
      final texte = TextPainter(text: TextSpan(text: barres[g].libelle, style: style), textDirection: TextDirection.ltr)
        ..layout(maxWidth: largeurGroupe);
      texte.paint(canvas, Offset(g * largeurGroupe + (largeurGroupe - texte.width) / 2, hauteurUtile + 4));
    }
  }

  @override
  bool shouldRepaint(_BarresPainter old) => old.barres != barres;
}

/// PRX-05 : courbe d'évolution d'un prix dans le temps.
class CourbePrix extends StatelessWidget {
  const CourbePrix({super.key, required this.points, this.hauteur = 140});
  final List<(DateTime, double)> points;
  final double hauteur;

  @override
  Widget build(BuildContext context) => SizedBox(
        height: hauteur,
        child: CustomPaint(
          size: Size.infinite,
          painter: _CourbePainter(points, Theme.of(context).colorScheme.primary, Theme.of(context).textTheme.labelSmall!),
        ),
      );
}

class _CourbePainter extends CustomPainter {
  _CourbePainter(this.points, this.couleur, this.style);
  final List<(DateTime, double)> points;
  final Color couleur;
  final TextStyle style;

  @override
  void paint(Canvas canvas, Size size) {
    if (points.length < 2) return;
    final t0 = points.first.$1.millisecondsSinceEpoch.toDouble();
    final t1 = points.last.$1.millisecondsSinceEpoch.toDouble();
    final valeurs = points.map((p) => p.$2);
    final mini = valeurs.reduce(min) * 0.95;
    final maxi = valeurs.reduce(max) * 1.05;
    Offset pos((DateTime, double) p) => Offset(
          t1 == t0 ? 0 : (p.$1.millisecondsSinceEpoch - t0) / (t1 - t0) * size.width,
          size.height - (p.$2 - mini) / (maxi - mini) * size.height,
        );

    final chemin = Path()..moveTo(pos(points.first).dx, pos(points.first).dy);
    for (final p in points.skip(1)) {
      chemin.lineTo(pos(p).dx, pos(p).dy);
    }
    canvas.drawPath(chemin, Paint()..color = couleur..style = PaintingStyle.stroke..strokeWidth = 2.5);
    for (final p in points) {
      canvas.drawCircle(pos(p), 3.5, Paint()..color = couleur);
    }
    for (final (texte, y) in [(Fmt.montant(maxi), 0.0), (Fmt.montant(mini), size.height - 14)]) {
      (TextPainter(text: TextSpan(text: texte, style: style), textDirection: TextDirection.ltr)..layout())
          .paint(canvas, Offset(0, y));
    }
  }

  @override
  bool shouldRepaint(_CourbePainter old) => old.points != points;
}
