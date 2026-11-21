import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_exception.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../data/prix_models.dart';
import '../logic/catalogue_controller.dart';
import 'univers_widgets.dart';

/// Onglet Prix (PRX-02 / PRX-03, évolution v3) :
/// 1. un champ code-barres bien visible (catalogue, puis Open Food Facts côté serveur) ;
/// 2. trois univers (Alimentation, Vêtements, High-tech) ;
/// 3. les rayons et les produits de l'univers choisi.
class CatalogueScreen extends StatefulWidget {
  const CatalogueScreen({super.key});
  @override
  State<CatalogueScreen> createState() => _CatalogueScreenState();
}

class _CatalogueScreenState extends State<CatalogueScreen> {
  final _code = TextEditingController();
  bool _rechercheCode = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final c = context.read<CatalogueController>();
      if (c.magasins.isEmpty || c.resultats.isEmpty) c.charger();
    });
  }

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  Future<void> _chercherCode() async {
    final code = _code.text.trim();
    if (!RegExp(r'^\d{6,14}$').hasMatch(code)) {
      notifier(context, 'Saisissez les chiffres du code-barres (8 à 13 en général)', erreur: true);
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() => _rechercheCode = true);
    ResultatCodeBarres? r;
    Object? erreur;
    try {
      r = await context.read<CatalogueController>().parCodeBarres(code);
    } catch (e) {
      erreur = e;
    }
    if (!mounted) return;
    // L'indicateur s'arrête avant d'ouvrir un dialogue ou une autre page.
    setState(() => _rechercheCode = false);
    if (r != null) {
      if (r.ajouteAuCatalogue) notifier(context, 'Produit trouvé sur Open Food Facts et ajouté au catalogue');
      context.go('/prix/produit/${r.produit.id}');
    } else if (erreur is ApiException && erreur.status == 404) {
      await _codeInconnu(code);
    } else {
      notifier(context, erreur ?? 'Erreur inconnue', erreur: true);
    }
  }

  /// Code introuvable partout : on propose à l'utilisateur d'enrichir le catalogue lui-même.
  Future<void> _codeInconnu(String code) async {
    final ajouter = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        icon: const Icon(Icons.search_off),
        title: const Text('Code inconnu'),
        content: Text('Le code $code n\'est ni dans le catalogue SmartBuy ni sur Open Food Facts.\n\n'
            'Vous pouvez ajouter ce produit : il profitera aussi aux autres utilisateurs.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(c, true), child: const Text('Ajouter ce produit')),
        ],
      ),
    );
    if (ajouter == true && mounted) await _contribuer(code);
  }

  Future<void> _contribuer(String code) async {
    final c = context.read<CatalogueController>();
    final saisie = await showModalBottomSheet<_Contribution>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _FormulaireContribution(
        codeBarres: code,
        universInitial: c.univers ?? Univers.alimentation,
        rayons: c.repo.categoriesProduits(),
      ),
    );
    if (saisie == null || !mounted) return;
    try {
      final p = await c.contribuer(
        nom: saisie.nom, marque: saisie.marque, unite: saisie.unite,
        codeBarres: saisie.codeBarres, categorieProduitId: saisie.rayonId,
      );
      if (!mounted) return;
      _code.clear();
      notifier(context, 'Merci ! « ${p.nom} » a été ajouté au catalogue.');
      context.go('/prix/produit/${p.id}');
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<CatalogueController>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Comparer les prix'),
        actions: [
          IconButton(tooltip: 'Magasins', onPressed: () => context.go('/prix/magasins'), icon: const Icon(Icons.store_outlined)),
        ],
      ),
      body: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
          child: TextField(
            controller: _code,
            keyboardType: TextInputType.number,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            textInputAction: TextInputAction.search,
            onSubmitted: (_) => _chercherCode(),
            style: Theme.of(context).textTheme.titleMedium,
            decoration: InputDecoration(
              labelText: 'Code-barres (ex. 6194043001019)',
              prefixIcon: const Icon(Icons.qr_code_2),
              suffixIcon: Padding(
                padding: const EdgeInsets.all(4),
                child: _rechercheCode
                    ? const Padding(padding: EdgeInsets.all(10), child: SizedBox.square(dimension: 20, child: CircularProgressIndicator(strokeWidth: 2)))
                    : IconButton.filled(tooltip: 'Chercher ce code', onPressed: _chercherCode, icon: const Icon(Icons.search)),
              ),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 4, 16, 12),
          child: Text(
            'Pas besoin de caméra : tapez les chiffres imprimés sous le code-barres.',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: TuilesUnivers(selection: c.univers, compact: c.univers != null, onChoisir: c.choisirUnivers),
        ),
        const SizedBox(height: 8),
        if (c.univers == null)
          const Expanded(
            child: EtatVide(
              icone: Icons.touch_app_outlined,
              message: 'Choisissez un univers pour parcourir ses rayons, ou cherchez un produit par son code-barres.',
            ),
          )
        else
          ..._catalogue(context, c),
      ]),
    );
  }

  List<Widget> _catalogue(BuildContext context, CatalogueController c) => [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
          child: TextField(
            decoration: InputDecoration(hintText: 'Rechercher en ${c.univers!.libelle}', prefixIcon: const Icon(Icons.search)),
            onChanged: c.rechercher,
          ),
        ),
        SizedBox(
          height: 44,
          child: ListView(scrollDirection: Axis.horizontal, padding: const EdgeInsets.symmetric(horizontal: 16), children: [
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(label: const Text('Tous'), selected: c.categorieId == null, onSelected: (_) => c.filtrerCategorie(null)),
            ),
            for (final cat in c.categories)
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: Text(cat.nom),
                  selected: c.categorieId == cat.id,
                  onSelected: (_) => c.filtrerCategorie(cat.id),
                ),
              ),
          ]),
        ),
        if (c.chargement) const LinearProgressIndicator(),
        Expanded(
          child: c.erreur != null && c.resultats.isEmpty
              ? MessageErreur(c.erreur!, reessayer: c.charger)
              : c.resultats.isEmpty && !c.chargement
                  ? const EtatVide(icone: Icons.search_off, message: 'Aucun produit trouvé.')
                  : ListView.separated(
                      itemCount: c.resultats.length,
                      separatorBuilder: (_, _) => const Divider(height: 1, indent: 72),
                      itemBuilder: (context, i) {
                        final p = c.resultats[i];
                        return ListTile(
                          leading: CircleAvatar(child: Text(p.nom[0])),
                          title: Text(p.nom),
                          subtitle: Text('${p.marque} · ${p.categorieProduitNom}'),
                          trailing: p.meilleurPrix == null
                              ? const Text('—')
                              : Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.end, children: [
                                  Text('dès ${Fmt.montant(p.meilleurPrix)}', style: const TextStyle(fontWeight: FontWeight.w600)),
                                  Text(p.meilleurMagasinNom ?? '', style: Theme.of(context).textTheme.bodySmall),
                                ]),
                          onTap: () => context.go('/prix/produit/${p.id}'),
                        );
                      },
                    ),
        ),
      ];
}

typedef _Contribution = ({String nom, String marque, String unite, String? codeBarres, int rayonId});

/// Formulaire « Ajouter ce produit » (POST /produits/contribution).
class _FormulaireContribution extends StatefulWidget {
  const _FormulaireContribution({required this.codeBarres, required this.universInitial, required this.rayons});
  final String codeBarres;
  final Univers universInitial;
  final Future<List<CategorieProduit>> rayons;
  @override
  State<_FormulaireContribution> createState() => _FormulaireContributionState();
}

class _FormulaireContributionState extends State<_FormulaireContribution> {
  final _form = GlobalKey<FormState>();
  final _nom = TextEditingController();
  final _marque = TextEditingController();
  late final _code = TextEditingController(text: widget.codeBarres);
  late Univers _univers = widget.universInitial;
  String _unite = 'pièce';
  int? _rayonId;

  static const _unites = ['pièce', 'kg', 'g', 'L', 'ml'];

  @override
  Widget build(BuildContext context) => Padding(
        padding: EdgeInsets.fromLTRB(16, 16, 16, MediaQuery.viewInsetsOf(context).bottom + 16),
        child: Form(
          key: _form,
          child: SingleChildScrollView(
            child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              Text('Ajouter un produit', style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 16),
              TextFormField(
                controller: _nom,
                autofocus: true,
                textCapitalization: TextCapitalization.sentences,
                decoration: const InputDecoration(labelText: 'Nom du produit'),
                validator: (v) => (v ?? '').trim().isEmpty ? 'Le nom est obligatoire' : null,
              ),
              const SizedBox(height: 12),
              TextFormField(controller: _marque, decoration: const InputDecoration(labelText: 'Marque')),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _unite,
                decoration: const InputDecoration(labelText: 'Unité'),
                items: [for (final u in _unites) DropdownMenuItem(value: u, child: Text(u))],
                onChanged: (v) => setState(() => _unite = v ?? 'pièce'),
              ),
              const SizedBox(height: 12),
              ChoixUnivers(
                valeur: _univers,
                onChange: (u) => setState(() {
                  _univers = u;
                  _rayonId = null; // le rayon choisi appartenait à l'ancien univers
                }),
              ),
              const SizedBox(height: 12),
              FutureBuilder(
                future: widget.rayons,
                builder: (context, snap) {
                  if (snap.hasError) return Text('Rayons indisponibles : ${snap.error}');
                  if (!snap.hasData) return const LinearProgressIndicator();
                  final rayons = snap.data!.where((r) => r.univers == _univers).toList();
                  return DropdownButtonFormField<int>(
                    key: ValueKey(_univers), // repart de zéro quand l'univers change
                    isExpanded: true,
                    initialValue: _rayonId,
                    decoration: InputDecoration(labelText: 'Rayon (${_univers.libelle})'),
                    items: [for (final r in rayons) DropdownMenuItem(value: r.id, child: Text(r.nom))],
                    onChanged: (v) => setState(() => _rayonId = v),
                    validator: (v) => v == null ? 'Choisissez un rayon' : null,
                  );
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _code,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                decoration: const InputDecoration(labelText: 'Code-barres'),
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: () {
                  if (!_form.currentState!.validate()) return;
                  Navigator.pop<_Contribution>(context, (
                    nom: _nom.text.trim(),
                    marque: _marque.text.trim(),
                    unite: _unite,
                    codeBarres: _code.text.trim().isEmpty ? null : _code.text.trim(),
                    rayonId: _rayonId!,
                  ));
                },
                child: const Text('Ajouter au catalogue'),
              ),
            ]),
          ),
        ),
      );
}
