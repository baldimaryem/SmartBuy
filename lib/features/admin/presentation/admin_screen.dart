import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../../prix/data/prix_models.dart';
import '../../prix/data/prix_repository.dart';
import '../../prix/logic/catalogue_controller.dart';

/// Espace administrateur : ADM-01 (magasins), ADM-02 (produits), ADM-03 (rayons), PRX-08 (validation des prix),
/// et (v3) la collecte des prix en ligne.
/// Accès protégé deux fois : le routeur redirige les non-admins, et le serveur répond 403.
class AdminScreen extends StatelessWidget {
  const AdminScreen({super.key});

  @override
  Widget build(BuildContext context) => DefaultTabController(
        length: 5,
        child: Scaffold(
          appBar: AppBar(
            title: const Text('Administration'),
            bottom: const TabBar(isScrollable: true, tabs: [
              Tab(text: 'Prix à valider'),
              Tab(text: 'Magasins'),
              Tab(text: 'Produits'),
              Tab(text: 'Rayons'),
              Tab(text: 'Collecte'),
            ]),
          ),
          body: const TabBarView(children: [_PrixEnAttente(), _Magasins(), _Produits(), _Rayons(), _Collecte()]),
        ),
      );
}

class _PrixEnAttente extends StatefulWidget {
  const _PrixEnAttente();
  @override
  State<_PrixEnAttente> createState() => _PrixEnAttenteState();
}

class _PrixEnAttenteState extends State<_PrixEnAttente> {
  late Future<List<Prix>> _prix = context.read<PrixRepository>().prixEnAttente();

  Future<void> _decider(Prix p, bool valider) async {
    try {
      await context.read<PrixRepository>().deciderPrix(p.id, valider: valider);
      if (!mounted) return;
      notifier(context, valider ? 'Prix validé' : 'Prix rejeté');
      setState(() => _prix = context.read<PrixRepository>().prixEnAttente());
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) => FutureBuilder(
        future: _prix,
        builder: (context, snap) {
          if (snap.hasError) return MessageErreur('${snap.error}');
          if (!snap.hasData) return const Chargement();
          if (snap.data!.isEmpty) return const EtatVide(icone: Icons.task_alt, message: 'Aucune proposition en attente.');
          return ListView(children: [
            for (final p in snap.data!)
              ListTile(
                title: Text('${p.produitNom} : ${Fmt.montant(p.montant)}'),
                subtitle: Text('${p.magasinNom} · ${Fmt.jour(p.dateReleve)} · par ${p.proposePar ?? '?'}'),
                trailing: Row(mainAxisSize: MainAxisSize.min, children: [
                  IconButton(tooltip: 'Rejeter', icon: const Icon(Icons.close, color: Colors.red), onPressed: () => _decider(p, false)),
                  IconButton(tooltip: 'Valider', icon: const Icon(Icons.check, color: Colors.green), onPressed: () => _decider(p, true)),
                ]),
              ),
          ]);
        },
      );
}

/// Formulaire générique : une liste de champs texte → valeurs saisies.
Future<List<String>?> _formulaire(BuildContext context, String titre, List<(String, String)> champs) {
  final ctrls = [for (final c in champs) TextEditingController(text: c.$2)];
  return showDialog<List<String>>(
    context: context,
    builder: (c) => AlertDialog(
      title: Text(titre),
      content: SingleChildScrollView(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          for (var i = 0; i < champs.length; i++)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: TextField(controller: ctrls[i], decoration: InputDecoration(labelText: champs[i].$1)),
            ),
        ]),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(c), child: const Text('Annuler')),
        FilledButton(onPressed: () => Navigator.pop(c, [for (final t in ctrls) t.text.trim()]), child: const Text('Enregistrer')),
      ],
    ),
  );
}

Future<void> _executer(BuildContext context, Future<void> Function() action) async {
  try {
    await action();
    if (context.mounted) await context.read<CatalogueController>().charger();
  } catch (e) {
    if (context.mounted) notifier(context, e, erreur: true);
  }
}

class _Magasins extends StatelessWidget {
  const _Magasins();

  Future<void> _editer(BuildContext context, [Magasin? m]) async {
    final v = await _formulaire(context, m == null ? 'Nouveau magasin' : 'Modifier', [
      ('Nom', m?.nom ?? ''),
      ('Enseigne', m?.enseigne ?? ''),
      ('Adresse', m?.adresse ?? ''),
      ('Ville', m?.ville ?? ''),
    ]);
    if (v == null || v[0].isEmpty || !context.mounted) return;
    final repo = context.read<PrixRepository>();
    await _executer(
      context,
      () => repo.enregistrerMagasin(Magasin(
        id: m?.id ?? 0, nom: v[0], enseigne: v[1], adresse: v[2], ville: v[3],
        enLigne: m?.enLigne ?? false, siteWeb: m?.siteWeb, // non modifiables ici : on les conserve
      )),
    );
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<CatalogueController>();
    return Scaffold(
      floatingActionButton: FloatingActionButton(tooltip: 'Ajouter', onPressed: () => _editer(context), child: const Icon(Icons.add)),
      body: ListView(children: [
        for (final m in c.magasins)
          ListTile(
            title: Text(m.nom),
            subtitle: Text(m.enLigne ? '${m.enseigne} · en ligne · ${m.siteWeb ?? ''}' : '${m.enseigne} · ${m.ville}'),
            onTap: () => _editer(context, m),
            trailing: IconButton(
              tooltip: 'Supprimer',
              icon: const Icon(Icons.delete_outline),
              onPressed: () async {
                if (!await confirmer(context, 'Supprimer ${m.nom} et ses prix ?') || !context.mounted) return;
                final repo = context.read<PrixRepository>();
                await _executer(context, () => repo.supprimerMagasin(m.id));
              },
            ),
          ),
      ]),
    );
  }
}

/// Filtre par univers (« Tous » = null). Il pilote le CatalogueController partagé :
/// l'onglet Prix suit le même choix, ce qui reste cohérent pour l'administrateur.
class _FiltreUnivers extends StatelessWidget {
  const _FiltreUnivers();

  @override
  Widget build(BuildContext context) {
    final c = context.watch<CatalogueController>();
    return SizedBox(
      height: 52,
      child: ListView(scrollDirection: Axis.horizontal, padding: const EdgeInsets.fromLTRB(16, 8, 16, 4), children: [
        Padding(
          padding: const EdgeInsets.only(right: 8),
          child: ChoiceChip(label: const Text('Tous'), selected: c.univers == null, onSelected: (_) => c.choisirUnivers(null)),
        ),
        for (final u in Univers.values)
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              avatar: Icon(u.icone, size: 18),
              showCheckmark: false,
              label: Text(u.libelle),
              selected: c.univers == u,
              onSelected: (_) => c.choisirUnivers(u),
            ),
          ),
      ]),
    );
  }
}

class _Produits extends StatelessWidget {
  const _Produits();

  Future<void> _editer(BuildContext context, [Produit? p]) async {
    final c = context.read<CatalogueController>();
    final v = await _formulaire(context, p == null ? 'Nouveau produit' : 'Modifier', [
      ('Nom', p?.nom ?? ''),
      ('Marque', p?.marque ?? ''),
      ('Code-barres', p?.codeBarres ?? ''),
      ('Unité (kg, L, pièce)', p?.unite ?? 'pièce'),
      ('Rayon : ${c.categories.map((x) => '${x.id}=${x.nom}').join(', ')}', '${p?.categorieProduitId ?? c.categories.firstOrNull?.id ?? ''}'),
    ]);
    if (v == null || v[0].isEmpty || !context.mounted) return;
    final repo = context.read<PrixRepository>();
    await _executer(
      context,
      () => repo.enregistrerProduit(Produit(
        id: p?.id ?? 0,
        nom: v[0],
        marque: v[1],
        codeBarres: v[2].isEmpty ? null : v[2],
        unite: v[3].isEmpty ? 'pièce' : v[3],
        categorieProduitId: int.tryParse(v[4]),
      )),
    );
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<CatalogueController>();
    return Scaffold(
      floatingActionButton: FloatingActionButton(tooltip: 'Ajouter', onPressed: () => _editer(context), child: const Icon(Icons.add)),
      body: ListView(children: [
        const _FiltreUnivers(),
        for (final p in c.resultats)
          ListTile(
            leading: Icon(p.univers.icone),
            title: Text(p.nom),
            subtitle: Text('${p.marque} · ${p.codeBarres ?? 'sans code'} · ${p.categorieProduitNom}'),
            onTap: () => _editer(context, p),
            trailing: IconButton(
              tooltip: 'Supprimer',
              icon: const Icon(Icons.delete_outline),
              onPressed: () async {
                if (!await confirmer(context, 'Supprimer ${p.nom} ?') || !context.mounted) return;
                final repo = context.read<PrixRepository>();
                await _executer(context, () => repo.supprimerProduit(p.id));
              },
            ),
          ),
      ]),
    );
  }
}

class _Rayons extends StatelessWidget {
  const _Rayons();

  @override
  Widget build(BuildContext context) {
    final c = context.watch<CatalogueController>();
    final repo = context.read<PrixRepository>();
    return Scaffold(
      floatingActionButton: FloatingActionButton(
        tooltip: 'Ajouter',
        onPressed: () async {
          // Le rayon est créé dans l'univers sélectionné par le filtre (Alimentation si « Tous »).
          final univers = c.univers ?? Univers.alimentation;
          final nom = await saisirTexte(context, titre: 'Nouveau rayon (${univers.libelle})');
          if (nom != null && nom.isNotEmpty && context.mounted) {
            await _executer(context, () => repo.enregistrerCategorieProduit(nom: nom, univers: univers));
          }
        },
        child: const Icon(Icons.add),
      ),
      body: ListView(children: [
        const _FiltreUnivers(),
        for (final r in c.categories)
          ListTile(
            leading: Icon(r.univers.icone),
            title: Text(r.nom),
            subtitle: Text(r.univers.libelle),
            onTap: () async {
              final nom = await saisirTexte(context, titre: 'Renommer', initial: r.nom);
              if (nom != null && nom.isNotEmpty && context.mounted) {
                await _executer(context, () => repo.enregistrerCategorieProduit(id: r.id, nom: nom));
              }
            },
            trailing: IconButton(
              tooltip: 'Supprimer',
              icon: const Icon(Icons.delete_outline),
              onPressed: () => _executer(context, () => repo.supprimerCategorieProduit(r.id)),
            ),
          ),
      ]),
    );
  }
}

/// Collecte des prix en ligne (Carrefour Tunisie, Spacenet). Côté serveur, robots.txt est respecté
/// et les sites sont interrogés à 1 requête par seconde : une collecte complète prend environ une minute.
class _Collecte extends StatefulWidget {
  const _Collecte();
  @override
  State<_Collecte> createState() => _CollecteState();
}

class _CollecteState extends State<_Collecte> {
  String? _enCours;
  ResultatCollecte? _resultat;

  Future<void> _lancer(String source) async {
    setState(() {
      _enCours = source;
      _resultat = null;
    });
    try {
      final r = await context.read<PrixRepository>().lancerCollecte(source);
      if (!mounted) return;
      setState(() => _resultat = r);
      notifier(context, 'Collecte terminée : ${r.prixEnregistres} prix enregistré(s)');
      context.read<CatalogueController>().charger(); // nouveaux produits et meilleurs prix
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    } finally {
      if (mounted) setState(() => _enCours = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final r = _resultat;
    final petit = Theme.of(context).textTheme.bodySmall;
    return ListView(padding: const EdgeInsets.all(16), children: [
      Text(
        'Les prix sont lus sur les sites des enseignes (robots.txt respecté, 1 requête par seconde). '
        'Une collecte peut durer environ une minute.',
        style: petit,
      ),
      const SizedBox(height: 16),
      FilledButton.icon(
        onPressed: _enCours == null ? () => _lancer('CARREFOUR') : null,
        icon: const Icon(Icons.shopping_cart_outlined),
        label: const Text('Collecter les prix Carrefour'),
      ),
      const SizedBox(height: 12),
      FilledButton.tonalIcon(
        onPressed: _enCours == null ? () => _lancer('SPACENET') : null,
        icon: const Icon(Icons.devices_outlined),
        label: const Text('Collecter High-tech Spacenet'),
      ),
      if (_enCours != null) ...[
        const SizedBox(height: 24),
        Row(children: [
          const SizedBox.square(dimension: 24, child: CircularProgressIndicator(strokeWidth: 3)),
          const SizedBox(width: 16),
          Expanded(child: Text('Collecte ${_enCours == 'CARREFOUR' ? 'Carrefour' : 'Spacenet'} en cours… ne quittez pas cet écran.')),
        ]),
      ],
      if (r != null) ...[
        const SizedBox(height: 24),
        Section(
          titre: 'Résultat ${r.source}${r.duree.isEmpty ? '' : ' (${r.duree})'}',
          child: Column(children: [
            _ligne('Produits analysés', r.produitsAnalyses),
            _ligne('Produits créés', r.produitsCrees),
            _ligne('Prix enregistrés', r.prixEnregistres),
            _ligne('Erreurs', r.erreurs.length),
            for (final e in r.erreurs.take(10))
              Padding(padding: const EdgeInsets.only(top: 4), child: Text('• $e', style: petit)),
            if (r.erreurs.length > 10) Text('… et ${r.erreurs.length - 10} autre(s)', style: petit),
          ]),
        ),
      ],
    ]);
  }

  Widget _ligne(String libelle, int valeur) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(children: [
          Expanded(child: Text(libelle)),
          Text('$valeur', style: const TextStyle(fontWeight: FontWeight.bold)),
        ]),
      );
}
