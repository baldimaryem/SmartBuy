import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';
import '../utils/formatters.dart';

/// Petits composants réutilisés dans toute l'application.

class Chargement extends StatelessWidget {
  const Chargement({super.key});
  @override
  Widget build(BuildContext context) => const Center(child: Padding(padding: EdgeInsets.all(32), child: CircularProgressIndicator()));
}

class MessageErreur extends StatelessWidget {
  const MessageErreur(this.message, {super.key, this.reessayer});
  final String message;
  final VoidCallback? reessayer;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Icon(Icons.cloud_off_outlined, size: 48, color: Theme.of(context).colorScheme.error),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center),
            if (reessayer != null) ...[
              const SizedBox(height: 16),
              OutlinedButton.icon(onPressed: reessayer, icon: const Icon(Icons.refresh), label: const Text('Réessayer')),
            ],
          ]),
        ),
      );
}

class EtatVide extends StatelessWidget {
  const EtatVide({super.key, required this.icone, required this.message, this.action});
  final IconData icone;
  final String message;
  final Widget? action;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Icon(icone, size: 56, color: Theme.of(context).colorScheme.outline),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyLarge),
            if (action != null) ...[const SizedBox(height: 16), action!],
          ]),
        ),
      );
}

/// Carte avec titre, utilisée pour structurer l'accueil et les statistiques.
class Section extends StatelessWidget {
  const Section({super.key, required this.titre, required this.child, this.action});
  final String titre;
  final Widget child;
  final Widget? action;

  @override
  Widget build(BuildContext context) => Card(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Row(children: [
              Expanded(child: Text(titre, style: Theme.of(context).textTheme.titleMedium)),
              ?action,
            ]),
            const SizedBox(height: 12),
            child,
          ]),
        ),
      );
}

/// Barre de progression d'un budget : verte, orange à 80 %, rouge à 100 % (BUD-10, BUD-11).
class JaugeBudget extends StatelessWidget {
  const JaugeBudget({super.key, required this.depense, required this.plafond, this.hauteur = 10});
  final double depense;
  final double plafond;
  final double hauteur;

  static Color couleur(double pourcentage) =>
      pourcentage >= 100 ? AppTheme.danger : (pourcentage >= 80 ? AppTheme.attention : AppTheme.succes);

  @override
  Widget build(BuildContext context) {
    final p = plafond <= 0 ? 0.0 : depense / plafond * 100;
    return ClipRRect(
      borderRadius: BorderRadius.circular(hauteur),
      child: LinearProgressIndicator(
        value: (p / 100).clamp(0, 1),
        minHeight: hauteur,
        color: couleur(p),
        backgroundColor: Theme.of(context).colorScheme.surfaceContainerHighest,
      ),
    );
  }
}

/// « ‹ novembre 2026 › » : navigation entre les mois.
class SelecteurMois extends StatelessWidget {
  const SelecteurMois({super.key, required this.mois, required this.onChange});
  final String mois;
  final void Function(int delta) onChange;

  @override
  Widget build(BuildContext context) {
    final estFutur = mois.compareTo(Fmt.isoMois(DateTime.now())) >= 0;
    return Row(mainAxisAlignment: MainAxisAlignment.center, children: [
      IconButton(tooltip: 'Mois précédent', onPressed: () => onChange(-1), icon: const Icon(Icons.chevron_left)),
      Text(Fmt.moisLong(mois), style: Theme.of(context).textTheme.titleMedium),
      IconButton(tooltip: 'Mois suivant', onPressed: estFutur ? null : () => onChange(1), icon: const Icon(Icons.chevron_right)),
    ]);
  }
}

/// Affiche une erreur (ou un succès) en bas de l'écran.
void notifier(BuildContext context, Object message, {bool erreur = false}) {
  final schema = Theme.of(context).colorScheme;
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(
      content: Text('$message'),
      backgroundColor: erreur ? schema.error : null,
    ));
}

Future<bool> confirmer(BuildContext context, String question, {String action = 'Supprimer'}) async =>
    await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        content: Text(question),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(c, true), child: Text(action)),
        ],
      ),
    ) ??
    false;

Future<String?> saisirTexte(BuildContext context, {required String titre, String initial = '', String libelle = 'Nom'}) {
  final ctrl = TextEditingController(text: initial);
  return showDialog<String>(
    context: context,
    builder: (c) => AlertDialog(
      title: Text(titre),
      content: TextField(
        controller: ctrl,
        autofocus: true,
        decoration: InputDecoration(labelText: libelle),
        onSubmitted: (v) => Navigator.pop(c, v.trim()),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(c), child: const Text('Annuler')),
        FilledButton(onPressed: () => Navigator.pop(c, ctrl.text.trim()), child: const Text('Valider')),
      ],
    ),
  );
}

/// Validation des montants saisis (accepte la virgule française).
double? lireMontant(String? texte) => double.tryParse((texte ?? '').trim().replaceAll(' ', '').replaceAll(',', '.'));

String? validerMontant(String? v) {
  final m = lireMontant(v);
  if (m == null) return 'Montant invalide';
  if (m <= 0) return 'Le montant doit être positif';
  return null;
}
