import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../logic/listes_controller.dart';
import 'univers_widgets.dart';

/// PRX-09 : créer, renommer, supprimer des listes de courses.
class ListesScreen extends StatefulWidget {
  const ListesScreen({super.key});
  @override
  State<ListesScreen> createState() => _ListesScreenState();
}

class _ListesScreenState extends State<ListesScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => context.read<ListesController>().charger());
  }

  Future<void> _creer() async {
    final r = await demanderNouvelleListe(context);
    if (r == null || !mounted) return;
    try {
      final l = await context.read<ListesController>().creer(r.$1, univers: r.$2);
      if (mounted) context.go('/listes/${l.id}');
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<ListesController>();
    return Scaffold(
      appBar: AppBar(title: const Text('Mes listes de courses')),
      floatingActionButton: FloatingActionButton.extended(onPressed: _creer, icon: const Icon(Icons.add), label: const Text('Liste')),
      body: c.erreur != null && c.listes.isEmpty
          ? MessageErreur(c.erreur!, reessayer: c.charger)
          : c.listes.isEmpty
              ? (c.chargement
                  ? const Chargement()
                  : const EtatVide(icone: Icons.checklist, message: 'Préparez une liste : SmartBuy trouvera le magasin le moins cher.'))
              : RefreshIndicator(
                  onRefresh: c.charger,
                  child: ListView(padding: const EdgeInsets.only(bottom: 88), children: [
                    for (final l in c.listes)
                      ListTile(
                        // L'icône de l'univers dit d'un coup d'œil quel budget la liste débitera.
                        leading: Badge(
                          isLabelVisible: l.achetee,
                          backgroundColor: Theme.of(context).colorScheme.primary,
                          label: const Icon(Icons.check, size: 10, color: Colors.white),
                          child: Icon(l.univers.icone, color: l.achetee ? Theme.of(context).colorScheme.primary : null),
                        ),
                        title: Text(l.nom),
                        subtitle: Text('${l.univers.libelle} · ${l.nbArticles} article(s) · ${Fmt.jour(l.dateCreation)}'
                            '${l.achetee ? ' · achetée' : ''}'),
                        trailing: PopupMenuButton<String>(
                          onSelected: (action) async {
                            try {
                              if (action == 'renommer') {
                                final nom = await saisirTexte(context, titre: 'Renommer', initial: l.nom);
                                if (nom != null && nom.isNotEmpty) await c.renommer(l.id, nom);
                              } else if (await confirmer(context, 'Supprimer « ${l.nom} » ?')) {
                                await c.supprimer(l.id);
                              }
                            } catch (e) {
                              if (context.mounted) notifier(context, e, erreur: true);
                            }
                          },
                          itemBuilder: (_) => const [
                            PopupMenuItem(value: 'renommer', child: Text('Renommer')),
                            PopupMenuItem(value: 'supprimer', child: Text('Supprimer')),
                          ],
                        ),
                        onTap: () => context.go('/listes/${l.id}'),
                      ),
                  ]),
                ),
    );
  }
}
