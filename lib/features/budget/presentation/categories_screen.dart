import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/communs.dart';
import '../data/budget_models.dart';
import '../logic/budget_controller.dart';

/// BUD-01 / BUD-02 : catégories par défaut + catégories personnelles.
class CategoriesScreen extends StatelessWidget {
  const CategoriesScreen({super.key});

  Future<void> _editer(BuildContext context, TypeOperation type, {Categorie? categorie}) async {
    final c = context.read<BudgetController>();
    final resultat = await showDialog<(String, String)>(
      context: context,
      builder: (_) => _DialogueCategorie(categorie: categorie),
    );
    if (resultat == null || resultat.$1.isEmpty) return;
    try {
      await c.enregistrerCategorie(id: categorie?.id, nom: resultat.$1, type: type, icone: resultat.$2);
    } catch (e) {
      if (context.mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<BudgetController>();
    return DefaultTabController(
      length: 2,
      child: Builder(builder: (context) {
        return Scaffold(
          appBar: AppBar(
            title: const Text('Catégories'),
            bottom: const TabBar(tabs: [Tab(text: 'Dépenses'), Tab(text: 'Revenus')]),
          ),
          floatingActionButton: FloatingActionButton(
            tooltip: 'Nouvelle catégorie',
            onPressed: () => _editer(
                context, DefaultTabController.of(context).index == 0 ? TypeOperation.depense : TypeOperation.revenu),
            child: const Icon(Icons.add),
          ),
          body: TabBarView(children: [
            for (final type in TypeOperation.values)
              ListView(children: [
                for (final cat in c.categoriesDe(type))
                  ListTile(
                    leading: Icon(iconeCategorie(cat.icone)),
                    title: Text(cat.nom),
                    subtitle: Text(cat.personnalisee ? 'Personnelle' : 'Par défaut'),
                    trailing: cat.personnalisee
                        ? IconButton(
                            tooltip: 'Supprimer',
                            icon: const Icon(Icons.delete_outline),
                            onPressed: () async {
                              if (!await confirmer(context, 'Supprimer « ${cat.nom} » ?')) return;
                              try {
                                await c.supprimerCategorie(cat.id);
                              } catch (e) {
                                if (context.mounted) notifier(context, e, erreur: true);
                              }
                            },
                          )
                        : null,
                    onTap: cat.personnalisee ? () => _editer(context, type, categorie: cat) : null,
                  ),
              ]),
          ]),
        );
      }),
    );
  }
}

class _DialogueCategorie extends StatefulWidget {
  const _DialogueCategorie({this.categorie});
  final Categorie? categorie;
  @override
  State<_DialogueCategorie> createState() => _DialogueCategorieState();
}

class _DialogueCategorieState extends State<_DialogueCategorie> {
  late final _nom = TextEditingController(text: widget.categorie?.nom ?? '');
  late String _icone = widget.categorie?.icone ?? 'category';

  @override
  Widget build(BuildContext context) => AlertDialog(
        title: Text(widget.categorie == null ? 'Nouvelle catégorie' : 'Renommer'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          TextField(controller: _nom, autofocus: true, decoration: const InputDecoration(labelText: 'Nom')),
          const SizedBox(height: 12),
          Wrap(spacing: 4, children: [
            for (final i in iconesDisponibles)
              IconButton(
                isSelected: i == _icone,
                icon: Icon(iconeCategorie(i)),
                style: i == _icone ? IconButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.primaryContainer) : null,
                onPressed: () => setState(() => _icone = i),
              ),
          ]),
        ]),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(context, (_nom.text.trim(), _icone)), child: const Text('Valider')),
        ],
      );
}
