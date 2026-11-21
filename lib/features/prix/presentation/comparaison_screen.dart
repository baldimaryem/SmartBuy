import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../../budget/logic/budget_controller.dart';
import '../data/prix_models.dart';
import '../logic/listes_controller.dart';

/// PRX-11..13 + LNK-01..03 : le scénario clé de l'application.
/// « Ma liste → le magasin le moins cher → est-ce que ça rentre dans mon budget → j'enregistre la dépense ».
class ComparaisonScreen extends StatefulWidget {
  const ComparaisonScreen({super.key, required this.listeId});
  final int listeId;
  @override
  State<ComparaisonScreen> createState() => _ComparaisonScreenState();
}

class _ComparaisonScreenState extends State<ComparaisonScreen> {
  late Future<(Comparaison, ListeDetail)> _comparaison = _charger();
  bool _achat = false;

  /// La liste est relue pour connaître son univers : il donne la catégorie de budget affichée (LNK-01).
  Future<(Comparaison, ListeDetail)> _charger() async {
    final c = context.read<ListesController>();
    final r = await Future.wait([c.comparer(widget.listeId), c.detail(widget.listeId)]);
    return (r[0] as Comparaison, r[1] as ListeDetail);
  }

  Future<void> _acheter(TotalMagasin m, String categorie) async {
    if (!await confirmer(context, 'Enregistrer une dépense de ${Fmt.montant(m.total)} chez ${m.magasinNom} ?', action: 'Enregistrer')) return;
    if (!mounted) return;
    setState(() => _achat = true);
    try {
      final op = await context.read<ListesController>().acheter(widget.listeId, m.magasinId);
      if (!mounted) return;
      context.read<BudgetController>().charger(); // le tableau de bord tient compte de la nouvelle dépense
      notifier(context, op.economie > 0
          ? 'Dépense enregistrée. Économie réalisée : ${Fmt.montant(op.economie)} 🎉'
          : 'Dépense enregistrée dans $categorie');
      context.go('/listes');
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    } finally {
      if (mounted) setState(() => _achat = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Comparaison')),
        body: FutureBuilder(
          future: _comparaison,
          builder: (context, snap) {
            if (snap.hasError) {
              return MessageErreur('${snap.error}',
                  reessayer: () => setState(() => _comparaison = _charger()));
            }
            if (!snap.hasData) return const Chargement();
            final (c, liste) = snap.data!;
            final categorie = liste.univers.categorieBudget;
            if (c.magasins.isEmpty) return const EtatVide(icone: Icons.price_change_outlined, message: 'Aucun prix connu pour ces produits.');
            final reco = c.recommande;
            return ListView(padding: const EdgeInsets.all(16), children: [
              if (reco != null)
                Card(
                  color: Theme.of(context).colorScheme.primaryContainer,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      const Row(children: [Icon(Icons.emoji_events), SizedBox(width: 8), Text('Magasin recommandé')]),
                      const SizedBox(height: 8),
                      Text(reco.magasinNom, style: Theme.of(context).textTheme.headlineSmall),
                      Text(Fmt.montant(reco.total), style: Theme.of(context).textTheme.titleLarge),
                      if (c.economie > 0) Text('Vous économisez ${Fmt.montant(c.economie)} par rapport au plus cher.'),
                    ]),
                  ),
                )
              else
                const Card(child: ListTile(leading: Icon(Icons.info_outline), title: Text('Aucun magasin ne propose tous les produits.'))),
              const SizedBox(height: 12),
              // LNK-01 : rapprochement avec le budget
              if (c.budgetRestant != null)
                Card(
                  child: ListTile(
                    leading: Icon(c.depasseBudget ? Icons.warning_amber : Icons.check_circle_outline,
                        color: c.depasseBudget ? AppTheme.danger : AppTheme.succes),
                    title: Text(c.depasseBudget ? 'Attention : dépasse votre budget $categorie' : 'Compatible avec votre budget $categorie'),
                    subtitle: Text('Reste en $categorie ce mois-ci : ${Fmt.montant(c.budgetRestant)}'),
                  ),
                ),
              const SizedBox(height: 12),
              Text('Classement des magasins', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              for (final (i, m) in c.magasins.indexed)
                Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  child: ExpansionTile(
                    leading: CircleAvatar(child: Text('${i + 1}')),
                    title: Text(m.magasinNom),
                    subtitle: Text(m.complet ? 'Liste complète' : 'Incomplet : ${m.produitsManquants.join(', ')}',
                        style: TextStyle(color: m.complet ? null : AppTheme.attention)),
                    trailing: Text(Fmt.montant(m.total), style: const TextStyle(fontWeight: FontWeight.bold)),
                    children: [
                      for (final d in m.details)
                        ListTile(
                          dense: true,
                          title: Text(d.produitNom),
                          subtitle: Text('${d.quantite.toStringAsFixed(0)} × ${Fmt.montant(d.prixUnitaire)}'),
                          trailing: Text(Fmt.montant(d.sousTotal)),
                        ),
                      Padding(
                        padding: const EdgeInsets.all(12),
                        child: FilledButton.tonalIcon(
                          onPressed: _achat ? null : () => _acheter(m, categorie),
                          icon: const Icon(Icons.shopping_bag_outlined),
                          label: const Text('J\'ai acheté ici'),
                        ),
                      ),
                    ],
                  ),
                ),
            ]);
          },
        ),
      );
}
