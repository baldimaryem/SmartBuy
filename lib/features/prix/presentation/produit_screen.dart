import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../../../shared/widgets/graphiques.dart';
import '../../auth/logic/auth_controller.dart';
import '../data/prix_models.dart';
import '../data/prix_repository.dart';
import '../logic/catalogue_controller.dart';
import '../logic/listes_controller.dart';
import 'univers_widgets.dart';

typedef _Donnees = (Produit, List<PrixMagasin>, List<PointHistorique>, Map<int, Magasin>);

/// PRX-04 (prix dans tous les magasins), PRX-05 (historique), PRX-07 (proposer un prix), PRX-10 (ajouter à une liste).
/// v3 : univers et origine de la fiche, prix « en ligne » / « collectés », actualisation des prix en ligne.
class ProduitScreen extends StatefulWidget {
  const ProduitScreen({super.key, required this.produitId});
  final int produitId;
  @override
  State<ProduitScreen> createState() => _ProduitScreenState();
}

class _ProduitScreenState extends State<ProduitScreen> {
  late Future<_Donnees> _donnees;
  bool _actualisation = false;

  @override
  void initState() {
    super.initState();
    _donnees = _charger();
  }

  /// Les magasins sont rechargés ici : la fiche peut s'ouvrir directement (code-barres) avant le catalogue,
  /// et il faut savoir quels magasins sont « en ligne ».
  Future<_Donnees> _charger() async {
    final repo = context.read<PrixRepository>();
    final r = await Future.wait([
      repo.produit(widget.produitId),
      repo.prixParMagasin(widget.produitId),
      repo.historique(widget.produitId),
      repo.magasins(),
    ]);
    return (
      r[0] as Produit,
      r[1] as List<PrixMagasin>,
      r[2] as List<PointHistorique>,
      {for (final m in r[3] as List<Magasin>) m.id: m},
    );
  }

  /// Relance la collecte en ligne pour ce produit (peut prendre plusieurs secondes côté serveur).
  Future<void> _actualiserPrix(Produit p) async {
    setState(() => _actualisation = true);
    try {
      final prix = await context.read<PrixRepository>().actualiserPrix(p.id);
      if (!mounted) return;
      notifier(context, 'Prix actualisés (${prix.length} magasin(s))');
      setState(() => _donnees = _charger());
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    } finally {
      if (mounted) setState(() => _actualisation = false);
    }
  }

  Future<void> _proposerPrix(Produit p) async {
    final magasins = context.read<CatalogueController>().magasins;
    final estAdmin = context.read<AuthController>().estAdmin;
    final r = await showModalBottomSheet<(int, double)>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _FormulairePrix(magasins: magasins, estAdmin: estAdmin),
    );
    if (r == null || !mounted) return;
    try {
      final prix = await context.read<PrixRepository>().proposerPrix(produitId: p.id, magasinId: r.$1, montant: r.$2, date: DateTime.now());
      if (!mounted) return;
      notifier(context, prix.statut == 'VALIDE' ? 'Prix enregistré' : 'Merci ! Votre prix sera vérifié par un administrateur.');
      setState(() => _donnees = _charger());
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  Future<void> _ajouterAListe(Produit p) async {
    final listes = context.read<ListesController>();
    if (listes.listes.isEmpty) await listes.charger();
    if (!mounted) return;
    // Seules les listes du même univers peuvent recevoir ce produit (le serveur répondrait 400).
    final enCours = listes.listes.where((l) => !l.achetee && l.univers == p.univers).toList();
    final choix = await showModalBottomSheet<int>(
      context: context,
      builder: (c) => SafeArea(
        child: ListView(shrinkWrap: true, children: [
          ListTile(title: Text('Ajouter à une liste ${p.univers.libelle}…')),
          for (final l in enCours) ListTile(leading: Icon(l.univers.icone), title: Text(l.nom), onTap: () => Navigator.pop(c, l.id)),
          ListTile(leading: const Icon(Icons.add), title: Text('Nouvelle liste ${p.univers.libelle}'), onTap: () => Navigator.pop(c, -1)),
        ]),
      ),
    );
    if (choix == null || !mounted) return;
    try {
      var id = choix;
      if (id == -1) {
        final r = await demanderNouvelleListe(context, univers: p.univers);
        if (r == null) return;
        id = (await listes.creer(r.$1, univers: r.$2)).id;
      }
      await listes.ajouter(id, p, 1);
      if (mounted) notifier(context, '${p.nom} ajouté à la liste');
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) => FutureBuilder(
        future: _donnees,
        builder: (context, snap) {
          if (snap.hasError) return Scaffold(appBar: AppBar(), body: MessageErreur('${snap.error}', reessayer: () => setState(() => _donnees = _charger())));
          if (!snap.hasData) return Scaffold(appBar: AppBar(), body: const Chargement());
          final (p, prix, historique, magasins) = snap.data!;
          final petit = Theme.of(context).textTheme.bodySmall;
          final ecart = prix.length < 2 ? 0.0 : prix.last.montant - prix.first.montant;
          return Scaffold(
            appBar: AppBar(title: Text(p.nom)),
            floatingActionButton: FloatingActionButton.extended(
              onPressed: () => _ajouterAListe(p),
              icon: const Icon(Icons.playlist_add),
              label: const Text('Ajouter à une liste'),
            ),
            body: ListView(padding: const EdgeInsets.fromLTRB(16, 8, 16, 96), children: [
              Text('${p.marque} · ${p.categorieProduitNom} · ${p.unite}', style: Theme.of(context).textTheme.bodyMedium),
              if (p.codeBarres != null) Text('Code-barres : ${p.codeBarres}', style: petit),
              const SizedBox(height: 8),
              Wrap(spacing: 8, runSpacing: 4, children: [
                Etiquette(p.univers.libelle, icone: p.univers.icone),
                if (p.source != 'CATALOGUE') Etiquette(p.libelleSource, icone: _iconeSource(p.source)),
              ]),
              if (p.urlSource != null) ...[
                const SizedBox(height: 8),
                Text('Page source :', style: petit),
                SelectableText(p.urlSource!, style: petit),
              ],
              const SizedBox(height: 16),
              Section(
                titre: 'Prix par magasin',
                action: TextButton.icon(onPressed: () => _proposerPrix(p), icon: const Icon(Icons.add, size: 18), label: const Text('Signaler un prix')),
                child: prix.isEmpty
                    ? const Text('Aucun prix connu pour ce produit.')
                    : Column(children: [
                        for (final m in prix)
                          ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: Icon(m.meilleur ? Icons.emoji_events : Icons.store_outlined, color: m.meilleur ? AppTheme.succes : null),
                            title: Text(m.magasinNom),
                            subtitle: Wrap(spacing: 6, runSpacing: 4, crossAxisAlignment: WrapCrossAlignment.center, children: [
                              Text('Relevé le ${Fmt.jour(m.dateReleve)}'),
                              if (magasins[m.magasinId]?.enLigne ?? false) const Etiquette('en ligne', icone: Icons.language),
                              if (m.collecte) const Etiquette('prix réel collecté', icone: Icons.verified_outlined),
                            ]),
                            trailing: Text(Fmt.montant(m.montant),
                                style: TextStyle(fontWeight: FontWeight.bold, color: m.meilleur ? AppTheme.succes : null)),
                          ),
                        if (ecart > 0)
                          Text('Écart entre le moins cher et le plus cher : ${Fmt.montant(ecart)}', style: petit),
                      ]),
              ),
              const SizedBox(height: 8),
              OutlinedButton.icon(
                onPressed: _actualisation ? null : () => _actualiserPrix(p),
                icon: _actualisation
                    ? const SizedBox.square(dimension: 18, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.sync),
                label: Text(_actualisation ? 'Actualisation en cours…' : 'Actualiser les prix en ligne'),
              ),
              const SizedBox(height: 12),
              if (historique.length >= 2)
                Section(
                  titre: 'Historique du prix (tous magasins)',
                  child: CourbePrix(points: [for (final h in historique) (h.dateReleve, h.montant)]),
                ),
              // Attribution exigée par la licence ODbL d'Open Food Facts.
              if (p.deOpenFoodFacts)
                Padding(
                  padding: const EdgeInsets.only(top: 16),
                  child: Text('Données produit : Open Food Facts (ODbL)', style: petit, textAlign: TextAlign.center),
                ),
            ]),
          );
        },
      );
}

IconData _iconeSource(String source) => switch (source) {
      'OPEN_FOOD_FACTS' => Icons.public,
      'COLLECTE' => Icons.travel_explore,
      'UTILISATEUR' => Icons.person_add_alt,
      _ => Icons.inventory_2_outlined,
    };

class _FormulairePrix extends StatefulWidget {
  const _FormulairePrix({required this.magasins, required this.estAdmin});
  final List<Magasin> magasins;
  final bool estAdmin;
  @override
  State<_FormulairePrix> createState() => _FormulairePrixState();
}

class _FormulairePrixState extends State<_FormulairePrix> {
  final _form = GlobalKey<FormState>();
  final _montant = TextEditingController();
  int? _magasinId;

  @override
  Widget build(BuildContext context) => Padding(
        padding: EdgeInsets.fromLTRB(16, 16, 16, MediaQuery.viewInsetsOf(context).bottom + 16),
        child: Form(
          key: _form,
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Text(widget.estAdmin ? 'Saisir un prix' : 'Signaler un prix constaté', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            DropdownButtonFormField<int>(
              isExpanded: true,
              initialValue: _magasinId,
              decoration: const InputDecoration(labelText: 'Magasin'),
              items: [for (final m in widget.magasins) DropdownMenuItem(value: m.id, child: Text(m.nom))],
              onChanged: (v) => setState(() => _magasinId = v),
              validator: (v) => v == null ? 'Choisissez un magasin' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _montant,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'Prix', suffixText: 'DT'),
              validator: validerMontant,
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: () {
                if (_form.currentState!.validate()) Navigator.pop(context, (_magasinId!, lireMontant(_montant.text)!));
              },
              child: const Text('Envoyer'),
            ),
          ]),
        ),
      );
}
