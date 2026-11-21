import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../../../shared/widgets/graphiques.dart';
import '../logic/budget_controller.dart';

/// BUD-13 / BUD-14 / LNK-03 : répartition, évolution sur 6 mois et économies.
class StatistiquesScreen extends StatelessWidget {
  const StatistiquesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = context.watch<BudgetController>();
    final d = c.dashboard;
    return Scaffold(
      appBar: AppBar(title: const Text('Statistiques')),
      body: d == null
          ? (c.erreur != null ? MessageErreur(c.erreur!, reessayer: c.charger) : const Chargement())
          : ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 24), children: [
              SelecteurMois(mois: c.mois, onChange: c.changerMois),
              Section(
                titre: 'Dépenses par catégorie',
                child: Column(children: [
                  GraphiqueCirculaire(parts: [for (final r in d.repartition) Part(r.categorieNom, r.montant)]),
                  const SizedBox(height: 12),
                  for (final r in d.repartition)
                    ListTile(
                      dense: true,
                      contentPadding: EdgeInsets.zero,
                      title: Text(r.categorieNom),
                      trailing: Text(Fmt.montant(r.montant)),
                    ),
                ]),
              ),
              const SizedBox(height: 12),
              Section(
                titre: 'Évolution sur 6 mois',
                child: GraphiqueBarres(
                  series: const ['Revenus', 'Dépenses'],
                  couleurs: const [AppTheme.revenu, AppTheme.depense],
                  barres: [for (final e in d.evolution) Barre(Fmt.moisCourt(e.mois), [e.revenus, e.depenses])],
                ),
              ),
              const SizedBox(height: 12),
              Section(
                titre: 'Indicateurs',
                child: Column(children: [
                  _ligne('Taux d\'épargne',
                      d.totalRevenus <= 0 ? '—' : '${(d.solde / d.totalRevenus * 100).toStringAsFixed(0)} %'),
                  _ligne('Moyenne des dépenses (6 mois)',
                      Fmt.montant(d.evolution.isEmpty ? 0 : d.evolution.fold(0.0, (s, e) => s + e.depenses) / d.evolution.length)),
                  _ligne('Prévision fin de mois', Fmt.montant(d.prevision.depenseEstimeeFinMois)),
                  _ligne('Économies grâce à la comparaison', Fmt.montant(d.economiesRealisees)),
                ]),
              ),
            ]),
    );
  }

  Widget _ligne(String libelle, String valeur) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 6),
        child: Row(children: [Expanded(child: Text(libelle)), Text(valeur, style: const TextStyle(fontWeight: FontWeight.w600))]),
      );
}
