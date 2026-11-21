import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../data/budget_models.dart';
import '../../auth/logic/auth_controller.dart';
import '../logic/budget_controller.dart';

/// BUD-08 / BUD-09 : budget global et budgets par catégorie du mois sélectionné.
class BudgetsScreen extends StatelessWidget {
  const BudgetsScreen({super.key});

  Future<void> _editer(BuildContext context, {Budget? budget}) async {
    final c = context.read<BudgetController>();
    final resultat = await showModalBottomSheet<(int?, double)>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _FormulaireBudget(
        budget: budget,
        categories: c.categoriesDe(TypeOperation.depense),
        dejaDefinis: c.budgets.map((b) => b.categorieId).toSet(),
      ),
    );
    if (resultat == null) return;
    try {
      await c.definirBudget(categorieId: resultat.$1, plafond: resultat.$2);
      if (context.mounted) notifier(context, 'Budget enregistré');
    } catch (e) {
      if (context.mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<BudgetController>();
    final revenu = context.watch<AuthController>().utilisateur?.revenuMensuel;
    final aUnBudgetGlobal = c.budgets.any((b) => b.estGlobal);
    return Scaffold(
      appBar: AppBar(title: const Text('Budgets')),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _editer(context),
        icon: const Icon(Icons.add),
        label: const Text('Budget'),
      ),
      body: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 96), children: [
        SelecteurMois(mois: c.mois, onChange: c.changerMois),
        if (c.chargement) const LinearProgressIndicator(),
        // Le revenu habituel du profil sert ici : il propose un budget global qui garde 20 % d'épargne.
        if (revenu != null && revenu > 0 && !aUnBudgetGlobal && !c.chargement) ...[
          _SuggestionBudget(revenu: revenu),
          const SizedBox(height: 12),
        ],
        if (c.budgets.isEmpty && !c.chargement)
          const EtatVide(icone: Icons.account_balance_wallet_outlined, message: 'Aucun budget pour ce mois.\nCommencez par un budget global.'),
        for (final b in c.budgets) ...[
          Card(
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () => _editer(context, budget: b),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [
                    Expanded(
                      child: Text(b.estGlobal ? 'Budget global' : b.categorieNom ?? '',
                          style: Theme.of(context).textTheme.titleMedium),
                    ),
                    Text('${b.pourcentage.toStringAsFixed(0)} %',
                        style: TextStyle(color: JaugeBudget.couleur(b.pourcentage), fontWeight: FontWeight.bold)),
                    IconButton(
                      tooltip: 'Supprimer',
                      icon: const Icon(Icons.delete_outline),
                      onPressed: () async {
                        if (await confirmer(context, 'Supprimer ce budget ?')) await c.supprimerBudget(b.id);
                      },
                    ),
                  ]),
                  JaugeBudget(depense: b.depense, plafond: b.montantPlafond),
                  const SizedBox(height: 8),
                  Text(b.reste >= 0
                      ? '${Fmt.montant(b.depense)} dépensés · reste ${Fmt.montant(b.reste)} sur ${Fmt.montant(b.montantPlafond)}'
                      : 'Dépassé de ${Fmt.montant(-b.reste)} (plafond ${Fmt.montant(b.montantPlafond)})'),
                ]),
              ),
            ),
          ),
          const SizedBox(height: 12),
        ],
      ]),
    );
  }
}

class _FormulaireBudget extends StatefulWidget {
  const _FormulaireBudget({this.budget, required this.categories, required this.dejaDefinis});
  final Budget? budget;
  final List<Categorie> categories;
  final Set<int?> dejaDefinis;

  @override
  State<_FormulaireBudget> createState() => _FormulaireBudgetState();
}

class _FormulaireBudgetState extends State<_FormulaireBudget> {
  final _form = GlobalKey<FormState>();
  late final TextEditingController _plafond =
      TextEditingController(text: widget.budget?.montantPlafond.toStringAsFixed(3) ?? '');
  late int? _categorieId = widget.budget?.categorieId;

  @override
  Widget build(BuildContext context) => Padding(
        padding: EdgeInsets.fromLTRB(16, 16, 16, MediaQuery.viewInsetsOf(context).bottom + 16),
        child: Form(
          key: _form,
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Text(widget.budget == null ? 'Nouveau budget' : 'Modifier le budget', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            DropdownButtonFormField<int?>(
              isExpanded: true,
              initialValue: _categorieId,
              decoration: const InputDecoration(labelText: 'Portée'),
              items: [
                const DropdownMenuItem<int?>(value: null, child: Text('Budget global (toutes dépenses)')),
                for (final c in widget.categories) DropdownMenuItem(value: c.id, child: Text(c.nom)),
              ],
              onChanged: widget.budget != null ? null : (v) => setState(() => _categorieId = v),
            ),
            if (widget.budget == null && widget.dejaDefinis.contains(_categorieId))
              const Padding(
                padding: EdgeInsets.only(top: 4),
                child: Text('Un budget existe déjà : il sera remplacé.'),
              ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _plafond,
              autofocus: true,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'Plafond mensuel', suffixText: 'DT'),
              validator: validerMontant,
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: () {
                if (_form.currentState!.validate()) Navigator.pop(context, (_categorieId, lireMontant(_plafond.text)!));
              },
              child: const Text('Enregistrer'),
            ),
          ]),
        ),
      );
}

/// Suggestion de budget global à partir du revenu habituel (règle simple : épargner 20 %).
class _SuggestionBudget extends StatelessWidget {
  const _SuggestionBudget({required this.revenu});
  final double revenu;

  static const tauxEpargne = 0.20;

  @override
  Widget build(BuildContext context) {
    final epargne = revenu * tauxEpargne;
    final budget = revenu - epargne;
    return Card(
      color: Theme.of(context).colorScheme.secondaryContainer,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          const Row(children: [Icon(Icons.lightbulb_outline), SizedBox(width: 8), Text('Suggestion')]),
          const SizedBox(height: 8),
          Text("Avec un revenu de ${Fmt.montant(revenu)}, gardez ${Fmt.montant(epargne)} d'épargne (20 %) : "
              'votre budget global du mois serait de ${Fmt.montant(budget)}.'),
          const SizedBox(height: 12),
          FilledButton.tonal(
            onPressed: () async {
              try {
                await context.read<BudgetController>().definirBudget(plafond: budget);
                if (context.mounted) notifier(context, '✓ Budget global de ${Fmt.montant(budget)} enregistré');
              } catch (e) {
                if (context.mounted) notifier(context, e, erreur: true);
              }
            },
            child: Text('Appliquer ${Fmt.montant(budget)}'),
          ),
        ]),
      ),
    );
  }
}
