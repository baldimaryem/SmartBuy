import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../../prix/data/prix_models.dart';
import '../../prix/logic/catalogue_controller.dart';
import '../data/budget_models.dart';
import '../logic/budget_controller.dart';

/// BUD-03 / BUD-04 / BUD-05 / BUD-07 : ajout ou modification d'un revenu ou d'une dépense.
class OperationFormScreen extends StatefulWidget {
  const OperationFormScreen({super.key, this.operation, this.typeInitial = TypeOperation.depense});
  final Operation? operation;
  final TypeOperation typeInitial;

  @override
  State<OperationFormScreen> createState() => _OperationFormScreenState();
}

class _OperationFormScreenState extends State<OperationFormScreen> {
  final _form = GlobalKey<FormState>();
  late TypeOperation _type;
  late final TextEditingController _montant;
  late final TextEditingController _note;
  late DateTime _date;
  int? _categorieId;
  int? _magasinId;
  bool _recurrente = false;
  bool _envoi = false;

  bool get _edition => widget.operation != null;

  @override
  void initState() {
    super.initState();
    final o = widget.operation;
    _type = o?.type ?? widget.typeInitial;
    _montant = TextEditingController(text: o?.montant.toStringAsFixed(3) ?? '');
    _note = TextEditingController(text: o?.note ?? '');
    _date = o?.dateOperation ?? DateTime.now();
    _categorieId = o?.categorieId;
    _magasinId = o?.magasinId;
    _recurrente = o?.recurrente ?? false;
    // Les magasins (module Prix) servent à lier une dépense à un point de vente.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final cat = context.read<CatalogueController>();
      if (cat.magasins.isEmpty) cat.charger();
      final b = context.read<BudgetController>();
      if (b.categories.isEmpty) b.charger();
    });
  }

  Future<void> _choisirDate() async {
    final d = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(2020),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (d != null) setState(() => _date = d);
  }

  Future<void> _enregistrer() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _envoi = true);
    final c = context.read<BudgetController>();
    try {
      final nouvellesAlertes = await c.enregistrerOperation(Operation(
        id: widget.operation?.id ?? 0,
        type: _type,
        montant: lireMontant(_montant.text)!,
        dateOperation: _date,
        categorieId: _categorieId!,
        note: _note.text.trim().isEmpty ? null : _note.text.trim(),
        magasinId: _type == TypeOperation.depense ? _magasinId : null,
        recurrente: _recurrente,
      ));
      if (!mounted) return;
      // BUD-11 : on montre immédiatement l'alerte de budget déclenchée par cette dépense.
      notifier(
        context,
        nouvellesAlertes.isNotEmpty ? '⚠ ${nouvellesAlertes.first.message}' : (_edition ? 'Opération modifiée' : 'Opération ajoutée'),
        erreur: nouvellesAlertes.any((a) => a.type == 'BUDGET_100'),
      );
      context.pop();
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final categories = context.watch<BudgetController>().categoriesDe(_type);
    final magasins = context.watch<CatalogueController>().magasins;
    if (_categorieId != null && !categories.any((c) => c.id == _categorieId)) _categorieId = null;

    return Scaffold(
      appBar: AppBar(title: Text(_edition ? 'Modifier l\'opération' : 'Nouvelle opération')),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(16), children: [
          SegmentedButton<TypeOperation>(
            segments: const [
              ButtonSegment(value: TypeOperation.depense, label: Text('Dépense'), icon: Icon(Icons.arrow_upward)),
              ButtonSegment(value: TypeOperation.revenu, label: Text('Revenu'), icon: Icon(Icons.arrow_downward)),
            ],
            selected: {_type},
            onSelectionChanged: _edition ? null : (s) => setState(() => _type = s.first),
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _montant,
            autofocus: !_edition,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            style: Theme.of(context).textTheme.headlineSmall,
            decoration: const InputDecoration(labelText: 'Montant', suffixText: 'DT'),
            validator: validerMontant,
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<int>(
            // Nouvelle clé quand on passe de Dépense à Revenu : la liste repart de zéro,
            // aucune catégorie de dépense ne peut rester sélectionnée pour un revenu.
            key: ValueKey(_type),
            isExpanded: true,
            initialValue: _categorieId,
            decoration: const InputDecoration(labelText: 'Catégorie'),
            items: [
              for (final c in categories)
                DropdownMenuItem(
                  value: c.id,
                  child: Row(children: [
                    Icon(iconeCategorie(c.icone), size: 20),
                    const SizedBox(width: 8),
                    Flexible(child: Text(c.nom, overflow: TextOverflow.ellipsis)),
                  ]),
                ),
            ],
            onChanged: (v) => setState(() => _categorieId = v),
            validator: (v) => v == null ? 'Choisissez une catégorie' : null,
          ),
          const SizedBox(height: 12),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.event_outlined),
            title: const Text('Date'),
            trailing: Text(Fmt.jour(_date)),
            onTap: _choisirDate,
          ),
          if (_type == TypeOperation.depense) ...[
            DropdownButtonFormField<int?>(
              isExpanded: true,
              initialValue: magasins.any((m) => m.id == _magasinId) ? _magasinId : null,
              decoration: const InputDecoration(labelText: 'Magasin (facultatif)'),
              items: [
                const DropdownMenuItem<int?>(value: null, child: Text('Aucun')),
                for (final Magasin m in magasins) DropdownMenuItem(value: m.id, child: Text(m.nom)),
              ],
              onChanged: (v) => setState(() => _magasinId = v),
            ),
            const SizedBox(height: 12),
          ],
          TextFormField(
            controller: _note,
            decoration: InputDecoration(labelText: _type == TypeOperation.revenu ? 'Source / note' : 'Note'),
            maxLength: 200,
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Récurrente chaque mois'),
            subtitle: const Text('Loyer, abonnement, salaire… recopiée automatiquement le 1er du mois'),
            value: _recurrente,
            onChanged: (v) => setState(() => _recurrente = v),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _envoi ? null : _enregistrer,
            child: _envoi
                ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Enregistrer'),
          ),
        ]),
      ),
    );
  }

  @override
  void dispose() {
    _montant.dispose();
    _note.dispose();
    super.dispose();
  }
}
