import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../data/prix_models.dart';
import '../data/prix_repository.dart';
import '../logic/listes_controller.dart';

/// PRX-10 : contenu d'une liste (ajouter, changer la quantité, retirer) puis lancer la comparaison.
class ListeDetailScreen extends StatefulWidget {
  const ListeDetailScreen({super.key, required this.listeId});
  final int listeId;
  @override
  State<ListeDetailScreen> createState() => _ListeDetailScreenState();
}

class _ListeDetailScreenState extends State<ListeDetailScreen> {
  ListeDetail? _liste;
  String? _erreur;

  @override
  void initState() {
    super.initState();
    _recharger();
  }

  Future<void> _recharger() async {
    try {
      final l = await context.read<ListesController>().detail(widget.listeId);
      if (mounted) {
        setState(() {
          _liste = l;
          _erreur = null;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _erreur = '$e');
    }
  }

  Future<void> _action(Future<ListeDetail> Function() f) async {
    try {
      final l = await f();
      if (mounted) setState(() => _liste = l);
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  Future<void> _ajouterProduit() async {
    final p = await showModalBottomSheet<Produit>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _ChoixProduit(repo: context.read<PrixRepository>(), univers: _liste!.univers),
    );
    if (p == null || !mounted) return;
    await _action(() => context.read<ListesController>().ajouter(widget.listeId, p, 1));
  }

  @override
  Widget build(BuildContext context) {
    final l = _liste;
    final c = context.read<ListesController>();
    if (_erreur != null) return Scaffold(appBar: AppBar(), body: MessageErreur(_erreur!, reessayer: _recharger));
    if (l == null) return Scaffold(appBar: AppBar(), body: const Chargement());
    return Scaffold(
      appBar: AppBar(
        title: Text(l.nom),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: Chip(avatar: Icon(l.univers.icone, size: 18), label: Text(l.univers.libelle)),
          ),
        ],
      ),
      floatingActionButton: l.achetee
          ? null
          : FloatingActionButton(tooltip: 'Ajouter un produit', onPressed: _ajouterProduit, child: const Icon(Icons.add)),
      bottomNavigationBar: l.lignes.isEmpty
          ? null
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: FilledButton.icon(
                  onPressed: () => context.go('/listes/${l.id}/comparaison'),
                  icon: const Icon(Icons.compare_arrows),
                  label: const Text('Comparer les magasins'),
                ),
              ),
            ),
      body: l.lignes.isEmpty
          ? const EtatVide(icone: Icons.add_shopping_cart, message: 'Liste vide. Ajoutez des produits avec le bouton +.')
          : ListView(children: [
              if (l.achetee)
                const ListTile(leading: Icon(Icons.check_circle), title: Text('Liste achetée : elle ne peut plus être modifiée.')),
              for (final ligne in l.lignes)
                ListTile(
                  title: Text(ligne.produitNom),
                  subtitle: Text(ligne.unite),
                  trailing: l.achetee
                      ? Text('× ${ligne.quantite.toStringAsFixed(0)}')
                      : Row(mainAxisSize: MainAxisSize.min, children: [
                          IconButton(
                            tooltip: 'Moins',
                            icon: const Icon(Icons.remove_circle_outline),
                            onPressed: () => _action(() => c.modifierQuantite(l.id, ligne, ligne.quantite - 1)),
                          ),
                          Text(ligne.quantite.toStringAsFixed(0), style: Theme.of(context).textTheme.titleMedium),
                          IconButton(
                            tooltip: 'Plus',
                            icon: const Icon(Icons.add_circle_outline),
                            onPressed: () => _action(() => c.modifierQuantite(l.id, ligne, ligne.quantite + 1)),
                          ),
                        ]),
                ),
              Padding(
                padding: const EdgeInsets.all(16),
                child: Text('Créée le ${Fmt.jour(l.dateCreation)}', style: Theme.of(context).textTheme.bodySmall),
              ),
            ]),
    );
  }
}

/// Recherche d'un produit à ajouter dans la liste : seulement ceux de l'univers de la liste
/// (le serveur refuserait les autres avec une erreur 400).
class _ChoixProduit extends StatefulWidget {
  const _ChoixProduit({required this.repo, required this.univers});
  final PrixRepository repo;
  final Univers univers;
  @override
  State<_ChoixProduit> createState() => _ChoixProduitState();
}

class _ChoixProduitState extends State<_ChoixProduit> {
  late Future<List<Produit>> _resultats = widget.repo.produits(univers: widget.univers);

  @override
  Widget build(BuildContext context) => SizedBox(
        height: MediaQuery.sizeOf(context).height * 0.75,
        child: Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
          child: Column(children: [
            Padding(
              padding: const EdgeInsets.all(16),
              child: TextField(
                autofocus: true,
                decoration: InputDecoration(
                  hintText: 'Rechercher en ${widget.univers.libelle}',
                  prefixIcon: Icon(widget.univers.icone),
                ),
                onChanged: (q) => setState(() => _resultats = widget.repo.produits(recherche: q, univers: widget.univers)),
              ),
            ),
            Expanded(
              child: FutureBuilder(
                future: _resultats,
                builder: (context, snap) {
                  if (!snap.hasData) return const Chargement();
                  return ListView(children: [
                    for (final p in snap.data!)
                      ListTile(
                        title: Text(p.nom),
                        subtitle: Text(p.marque),
                        trailing: Text(p.meilleurPrix == null ? '' : 'dès ${Fmt.montant(p.meilleurPrix)}'),
                        onTap: () => Navigator.pop(context, p),
                      ),
                  ]);
                },
              ),
            ),
          ]),
        ),
      );
}
