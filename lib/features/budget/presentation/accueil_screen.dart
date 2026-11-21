import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../../../shared/widgets/graphiques.dart';
import '../../auth/logic/auth_controller.dart';
import '../data/budget_models.dart';
import '../logic/budget_controller.dart';

/// Tableau de bord (BUD-10, BUD-13, BUD-15, LNK-03).
class AccueilScreen extends StatefulWidget {
  const AccueilScreen({super.key});
  @override
  State<AccueilScreen> createState() => _AccueilScreenState();
}

class _AccueilScreenState extends State<AccueilScreen> {
  @override
  void initState() {
    super.initState();
    // Chargement après la première image (on ne notifie pas pendant un build).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final c = context.read<BudgetController>();
      if (c.dashboard == null) c.charger();
    });
  }

  @override
  Widget build(BuildContext context) {
    final c = context.watch<BudgetController>();
    final nom = context.watch<AuthController>().utilisateur?.nom.split(' ').first ?? '';
    final d = c.dashboard;

    return Scaffold(
      appBar: AppBar(
        title: Text('Bonjour $nom'),
        actions: [
          IconButton(
            tooltip: 'Alertes',
            onPressed: () => context.go('/accueil/alertes'),
            icon: Badge(
              isLabelVisible: c.alertesNonLues > 0,
              label: Text('${c.alertesNonLues}'),
              child: const Icon(Icons.notifications_outlined),
            ),
          ),
          IconButton(tooltip: 'Profil', onPressed: () => context.push('/profil'), icon: const Icon(Icons.account_circle_outlined)),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.go('/operations/edition', extra: TypeOperation.depense),
        icon: const Icon(Icons.add),
        label: const Text('Dépense'),
      ),
      body: RefreshIndicator(
        onRefresh: c.charger,
        child: ListView(padding: const EdgeInsets.fromLTRB(16, 0, 16, 96), children: [
          SelecteurMois(mois: c.mois, onChange: c.changerMois),
          if (d == null && c.chargement) const Chargement(),
          if (d == null && c.erreur != null) MessageErreur(c.erreur!, reessayer: c.charger),
          if (d != null) ..._contenu(context, c, d),
        ]),
      ),
    );
  }

  List<Widget> _contenu(BuildContext context, BudgetController c, Dashboard d) {
    final theme = Theme.of(context);
    final budgetsCategories = c.budgets.where((b) => !b.estGlobal).toList()
      ..sort((a, b) => b.pourcentage.compareTo(a.pourcentage));
    return [
      if (c.chargement) const LinearProgressIndicator(),
      // ---- Solde du mois
      Card(
        color: theme.colorScheme.primaryContainer,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text('Solde du mois', style: theme.textTheme.labelLarge),
            Text(Fmt.montant(d.solde), style: theme.textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold)),
            const SizedBox(height: 12),
            Row(children: [
              Expanded(child: _Chiffre(Icons.arrow_downward, 'Revenus', d.totalRevenus, AppTheme.revenu)),
              Expanded(child: _Chiffre(Icons.arrow_upward, 'Dépenses', d.totalDepenses, AppTheme.depense)),
            ]),
          ]),
        ),
      ),
      const SizedBox(height: 12),
      // ---- Reste à dépenser + prévision
      Section(
        titre: 'Reste à dépenser',
        action: TextButton(onPressed: () => context.go('/accueil/budgets'), child: const Text('Budgets')),
        child: d.budgetGlobal == null
            ? const Text('Définissez un budget global pour suivre votre reste à dépenser.')
            : Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(Fmt.montant(d.resteADepenser),
                    style: theme.textTheme.headlineSmall?.copyWith(
                        color: (d.resteADepenser ?? 0) < 0 ? AppTheme.danger : null, fontWeight: FontWeight.w600)),
                Text('sur ${Fmt.montant(d.budgetGlobal)}'),
                const SizedBox(height: 8),
                JaugeBudget(depense: d.totalDepenses, plafond: d.budgetGlobal!),
                const SizedBox(height: 12),
                Row(children: [
                  Icon(d.prevision.depassementProbable ? Icons.trending_up : Icons.trending_flat,
                      color: d.prevision.depassementProbable ? AppTheme.danger : AppTheme.succes),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(d.prevision.depassementProbable
                        ? 'Prévision fin de mois : ${Fmt.montant(d.prevision.depenseEstimeeFinMois)}. Dépassement probable !'
                        : 'Prévision fin de mois : ${Fmt.montant(d.prevision.depenseEstimeeFinMois)}. Vous êtes dans les clous.'),
                  ),
                ]),
              ]),
      ),
      const SizedBox(height: 12),
      // ---- Budgets par catégorie (les plus consommés d'abord)
      if (budgetsCategories.isNotEmpty) ...[
        Section(
          titre: 'Budgets par catégorie',
          child: Column(children: [
            for (final b in budgetsCategories.take(4))
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Row(children: [
                    Expanded(child: Text(b.categorieNom ?? '')),
                    Text('${Fmt.montant(b.depense)} / ${Fmt.montant(b.montantPlafond)}', style: theme.textTheme.bodySmall),
                  ]),
                  const SizedBox(height: 4),
                  JaugeBudget(depense: b.depense, plafond: b.montantPlafond, hauteur: 8),
                ]),
              ),
          ]),
        ),
        const SizedBox(height: 12),
      ],
      // ---- Répartition
      Section(
        titre: 'Répartition des dépenses',
        action: TextButton(onPressed: () => context.go('/accueil/statistiques'), child: const Text('Détails')),
        child: GraphiqueCirculaire(parts: [for (final r in d.repartition) Part(r.categorieNom, r.montant)]),
      ),
      if (d.economiesRealisees > 0) ...[
        const SizedBox(height: 12),
        Card(
          child: ListTile(
            leading: const Icon(Icons.savings_outlined, color: AppTheme.succes),
            title: Text('${Fmt.montant(d.economiesRealisees)} économisés'),
            subtitle: const Text('grâce à la comparaison des prix ce mois-ci'),
          ),
        ),
      ],
    ];
  }
}

class _Chiffre extends StatelessWidget {
  const _Chiffre(this.icone, this.libelle, this.valeur, this.couleur);
  final IconData icone;
  final String libelle;
  final double valeur;
  final Color couleur;

  @override
  Widget build(BuildContext context) => Row(children: [
        CircleAvatar(radius: 16, backgroundColor: couleur.withValues(alpha: 0.15), child: Icon(icone, size: 18, color: couleur)),
        const SizedBox(width: 8),
        Flexible(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(libelle, style: Theme.of(context).textTheme.labelSmall),
            FittedBox(child: Text(Fmt.montant(valeur), style: Theme.of(context).textTheme.titleSmall)),
          ]),
        ),
      ]);
}
