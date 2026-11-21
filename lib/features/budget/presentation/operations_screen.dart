import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../data/budget_models.dart';
import '../logic/budget_controller.dart';

/// BUD-05 / BUD-06 : historique filtré par mois, type et catégorie ; modification et suppression.
class OperationsScreen extends StatefulWidget {
  const OperationsScreen({super.key});
  @override
  State<OperationsScreen> createState() => _OperationsScreenState();
}

class _OperationsScreenState extends State<OperationsScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final c = context.read<BudgetController>();
      if (c.dashboard == null) c.charger();
    });
  }

  Future<void> _supprimer(Operation o) async {
    final c = context.read<BudgetController>();
    if (!await confirmer(context, 'Supprimer cette opération de ${Fmt.montant(o.montant)} ?')) return;
    try {
      await c.supprimerOperation(o.id);
      if (mounted) notifier(context, 'Opération supprimée');
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<BudgetController>();
    final categories = c.filtreType == null ? c.categories : c.categoriesDe(c.filtreType!);

    return Scaffold(
      appBar: AppBar(title: const Text('Opérations')),
      floatingActionButton: FloatingActionButton(
        tooltip: 'Nouvelle opération',
        onPressed: () => context.go('/operations/edition'),
        child: const Icon(Icons.add),
      ),
      body: Column(children: [
        SelecteurMois(mois: c.mois, onChange: c.changerMois),
        // ---- Filtres
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(children: [
            SegmentedButton<TypeOperation?>(
              segments: const [
                ButtonSegment(value: null, label: Text('Tout')),
                ButtonSegment(value: TypeOperation.depense, label: Text('Dépenses')),
                ButtonSegment(value: TypeOperation.revenu, label: Text('Revenus')),
              ],
              selected: {c.filtreType},
              onSelectionChanged: (s) => c.filtrer(type: s.first),
            ),
            const SizedBox(width: 8),
            DropdownButton<int?>(
              value: categories.any((x) => x.id == c.filtreCategorieId) ? c.filtreCategorieId : null,
              hint: const Text('Catégorie'),
              underline: const SizedBox(),
              items: [
                const DropdownMenuItem(value: null, child: Text('Toutes catégories')),
                for (final cat in categories) DropdownMenuItem(value: cat.id, child: Text(cat.nom)),
              ],
              onChanged: (id) => c.filtrer(type: c.filtreType, categorieId: id),
            ),
          ]),
        ),
        if (c.chargement) const LinearProgressIndicator(),
        Expanded(child: _liste(context, c)),
      ]),
    );
  }

  Widget _liste(BuildContext context, BudgetController c) {
    if (c.erreur != null && c.operations.isEmpty) return MessageErreur(c.erreur!, reessayer: c.charger);
    if (c.operations.isEmpty && !c.chargement) {
      return const EtatVide(icone: Icons.receipt_long_outlined, message: 'Aucune opération pour ce mois.');
    }
    return RefreshIndicator(
      onRefresh: c.charger,
      child: ListView.separated(
        padding: const EdgeInsets.only(bottom: 88),
        itemCount: c.operations.length,
        separatorBuilder: (_, _) => const Divider(height: 1, indent: 72),
        itemBuilder: (context, i) {
          final o = c.operations[i];
          final cat = c.categories.where((x) => x.id == o.categorieId).firstOrNull;
          final couleur = o.estDepense ? AppTheme.depense : AppTheme.revenu;
          return Dismissible(
            key: ValueKey(o.id),
            direction: DismissDirection.endToStart,
            background: Container(
              color: AppTheme.danger,
              alignment: Alignment.centerRight,
              padding: const EdgeInsets.only(right: 24),
              child: const Icon(Icons.delete_outline, color: Colors.white),
            ),
            confirmDismiss: (_) async {
              await _supprimer(o);
              return false; // la liste est rechargée par le contrôleur
            },
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: couleur.withValues(alpha: 0.12),
                child: Icon(iconeCategorie(cat?.icone), color: couleur),
              ),
              title: Text(o.note?.isNotEmpty == true ? o.note! : o.categorieNom),
              subtitle: Text([
                Fmt.jour(o.dateOperation),
                o.categorieNom,
                if (o.magasinNom != null) o.magasinNom!,
                if (o.recurrente) 'récurrente',
              ].join(' · ')),
              trailing: Text(
                '${o.estDepense ? '−' : '+'} ${Fmt.montant(o.montant)}',
                style: TextStyle(color: couleur, fontWeight: FontWeight.w600),
              ),
              onTap: () => context.go('/operations/edition', extra: o),
            ),
          );
        },
      ),
    );
  }
}
