import 'package:flutter/material.dart';

import '../data/prix_models.dart';

/// Composants partagés par les écrans qui manipulent les univers de courses
/// (catalogue, fiche produit, listes, administration).

/// Petite étiquette arrondie : « en ligne », « prix réel collecté », source d'un produit…
class Etiquette extends StatelessWidget {
  const Etiquette(this.texte, {super.key, this.icone, this.couleur});
  final String texte;
  final IconData? icone;
  final Color? couleur;

  @override
  Widget build(BuildContext context) {
    final schema = Theme.of(context).colorScheme;
    final fond = couleur ?? schema.secondaryContainer;
    final texteCouleur = couleur == null ? schema.onSecondaryContainer : Colors.white;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(color: fond, borderRadius: BorderRadius.circular(12)),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        if (icone != null) ...[Icon(icone, size: 14, color: texteCouleur), const SizedBox(width: 4)],
        Text(texte, style: Theme.of(context).textTheme.labelSmall?.copyWith(color: texteCouleur)),
      ]),
    );
  }
}

/// Sélecteur d'univers sous forme de puces (tient sur un écran de téléphone, contrairement
/// à un SegmentedButton à trois libellés longs). [verrouille] : univers imposé, affiché seulement.
class ChoixUnivers extends StatelessWidget {
  const ChoixUnivers({super.key, required this.valeur, required this.onChange, this.verrouille = false});
  final Univers valeur;
  final ValueChanged<Univers> onChange;
  final bool verrouille;

  @override
  Widget build(BuildContext context) => Wrap(spacing: 8, runSpacing: 4, children: [
        for (final u in Univers.values)
          if (!verrouille || u == valeur)
            ChoiceChip(
              avatar: Icon(u.icone, size: 18),
              label: Text(u.libelle),
              selected: u == valeur,
              showCheckmark: false,
              onSelected: verrouille ? null : (_) => onChange(u),
            ),
      ]);
}

/// Les trois grandes tuiles de l'onglet Prix. En mode [compact] (un univers est déjà choisi),
/// elles rétrécissent pour laisser la place aux rayons et aux produits.
class TuilesUnivers extends StatelessWidget {
  const TuilesUnivers({super.key, required this.selection, required this.onChoisir, this.compact = false});
  final Univers? selection;
  final ValueChanged<Univers> onChoisir;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final schema = Theme.of(context).colorScheme;
    return Row(children: [
      for (final (i, u) in Univers.values.indexed) ...[
        if (i > 0) const SizedBox(width: 8),
        Expanded(
          child: Material(
            color: u == selection ? schema.primaryContainer : schema.surfaceContainerLow,
            borderRadius: BorderRadius.circular(16),
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () => onChoisir(u),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                height: compact ? 56 : 104,
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                  Icon(u.icone, size: compact ? 22 : 36, color: u == selection ? schema.onPrimaryContainer : schema.primary),
                  const SizedBox(height: 4),
                  Text(
                    u.libelle,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: (compact ? Theme.of(context).textTheme.labelMedium : Theme.of(context).textTheme.titleSmall)
                        ?.copyWith(fontWeight: u == selection ? FontWeight.bold : null),
                  ),
                ]),
              ),
            ),
          ),
        ),
      ],
    ]);
  }
}

/// Dialogue « Nouvelle liste » : nom + univers. Si [univers] est fourni (ajout depuis une fiche produit),
/// il est imposé : la liste doit accueillir ce produit.
Future<(String, Univers)?> demanderNouvelleListe(BuildContext context, {Univers? univers}) {
  final nom = TextEditingController(text: univers == null || univers == Univers.alimentation ? 'Courses' : univers.libelle);
  var choix = univers ?? Univers.alimentation;
  return showDialog<(String, Univers)>(
    context: context,
    builder: (c) => StatefulBuilder(
      builder: (c, setState) => AlertDialog(
        title: const Text('Nouvelle liste'),
        content: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
          TextField(controller: nom, autofocus: true, decoration: const InputDecoration(labelText: 'Nom')),
          const SizedBox(height: 16),
          Text('Univers', style: Theme.of(c).textTheme.labelLarge),
          const SizedBox(height: 8),
          ChoixUnivers(valeur: choix, verrouille: univers != null, onChange: (u) => setState(() => choix = u)),
          const SizedBox(height: 8),
          Text('À l\'achat, la dépense ira dans « ${choix.categorieBudget} ».', style: Theme.of(c).textTheme.bodySmall),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c), child: const Text('Annuler')),
          FilledButton(
            onPressed: () {
              if (nom.text.trim().isNotEmpty) Navigator.pop(c, (nom.text.trim(), choix));
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    ),
  );
}
