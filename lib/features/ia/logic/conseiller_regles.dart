import '../../../shared/utils/formatters.dart';
import '../../budget/data/budget_models.dart';
import '../../budget/logic/budget_calculs.dart';
import '../data/ia_repository.dart';

/// Moteur de conseils à base de **règles** (BUD-16).
///
/// Pourquoi des règles avant l'IA ? Elles sont explicables (« la règle dit : +30 %
/// par rapport à la moyenne »), fonctionnent hors ligne et servent de secours si
/// le modèle d'IA du serveur n'est pas disponible. Le back-end possède le même
/// moteur et, si une clé d'API est configurée, demande en plus l'avis d'un modèle Claude.
class ConseillerRegles {
  ConseillerRegles({required this.courant, required this.budgets, required this.moisPrecedents});

  final Dashboard courant;
  final List<Budget> budgets;
  final List<Dashboard> moisPrecedents;

  /// Seuil à partir duquel une hausse de dépense dans une catégorie est signalée.
  static const double seuilHausse = 30;

  List<Conseil> conseils() {
    final liste = <Conseil>[];

    // Règle 1 : prévision de dépassement du budget global (BUD-15).
    if (courant.prevision.depassementProbable && courant.budgetGlobal != null) {
      liste.add(Conseil(
        titre: 'Dépassement probable',
        message: 'À ce rythme, vous dépenserez environ ${Fmt.montant(courant.prevision.depenseEstimeeFinMois)} '
            'ce mois-ci pour un budget de ${Fmt.montant(courant.budgetGlobal)}.',
        niveau: NiveauConseil.alerte,
      ));
    }

    // Règle 2 : budgets de catégorie à plus de 80 %.
    for (final b in budgets.where((b) => !b.estGlobal && b.pourcentage >= 80)) {
      liste.add(Conseil(
        titre: '${b.categorieNom} : ${b.pourcentage.toStringAsFixed(0)} % du budget',
        message: b.reste >= 0
            ? 'Il vous reste ${Fmt.montant(b.reste)} pour ${b.categorieNom} ce mois-ci.'
            : 'Budget dépassé de ${Fmt.montant(-b.reste)}.',
        niveau: b.pourcentage >= 100 ? NiveauConseil.alerte : NiveauConseil.attention,
      ));
    }

    // Règle 3 : catégorie en forte hausse par rapport à la moyenne des 3 mois précédents.
    for (final c in courant.repartition) {
      final passes = moisPrecedents
          .map((d) => d.repartition.where((r) => r.categorieId == c.categorieId).fold(0.0, (s, r) => s + r.montant))
          .toList();
      if (passes.isEmpty) continue;
      final moyenne = passes.reduce((a, b) => a + b) / passes.length;
      final variation = BudgetCalculs.variation(c.montant, moyenne);
      if (variation != null && variation >= seuilHausse) {
        liste.add(Conseil(
          titre: '${c.categorieNom} en hausse',
          message: 'Vos dépenses ${c.categorieNom} ont augmenté de ${variation.toStringAsFixed(0)} % '
              'par rapport à votre moyenne (${Fmt.montant(moyenne)}).',
          niveau: NiveauConseil.attention,
        ));
      }
    }

    // Règle 4 : taux d'épargne.
    if (courant.totalRevenus > 0) {
      final taux = courant.solde / courant.totalRevenus * 100;
      liste.add(taux >= 20
          ? Conseil(titre: 'Bonne épargne', message: 'Vous épargnez ${taux.toStringAsFixed(0)} % de vos revenus ce mois-ci. Continuez !')
          : Conseil(
              titre: 'Épargne faible',
              message: 'Vous épargnez ${taux.toStringAsFixed(0)} % de vos revenus. Objectif conseillé : 20 %.',
              niveau: NiveauConseil.attention,
            ));
    }

    // Règle 5 : encourager la comparaison des prix (lien entre les deux modules).
    final alimentation = courant.repartition.where((r) => r.categorieNom == 'Alimentation').firstOrNull;
    if (alimentation != null && alimentation.montant > 0) {
      liste.add(Conseil(
        titre: 'Comparez avant d\'acheter',
        message: courant.economiesRealisees > 0
            ? 'La comparaison vous a déjà fait économiser ${Fmt.montant(courant.economiesRealisees)} ce mois-ci.'
            : 'Préparez vos courses dans une liste : SmartBuy vous indiquera le magasin le moins cher.',
      ));
    }

    return liste;
  }

  /// Assistant simplifié : reconnaît quelques intentions par mots-clés.
  String repondre(String question) {
    final q = question.toLowerCase();
    if (q.contains('reste') || q.contains('combien') && q.contains('dépenser')) {
      return courant.resteADepenser == null
          ? 'Vous n\'avez pas encore défini de budget global pour ce mois.'
          : 'Il vous reste ${Fmt.montant(courant.resteADepenser)} à dépenser ce mois-ci.';
    }
    if (q.contains('prévision') || q.contains('fin du mois') || q.contains('fin de mois')) {
      return 'Au rythme actuel, vos dépenses atteindront environ ${Fmt.montant(courant.prevision.depenseEstimeeFinMois)} en fin de mois.';
    }
    if (q.contains('catégorie') || q.contains('plus dépens') || q.contains('où')) {
      if (courant.repartition.isEmpty) return 'Aucune dépense enregistrée ce mois-ci.';
      final top = courant.repartition.first;
      return 'Votre premier poste de dépense est ${top.categorieNom} avec ${Fmt.montant(top.montant)}.';
    }
    if (q.contains('économ') || q.contains('conseil')) {
      final c = conseils();
      return c.isEmpty ? 'Tout va bien, aucun conseil particulier.' : c.map((e) => '• ${e.message}').join('\n');
    }
    return 'Ce mois-ci : ${Fmt.montant(courant.totalRevenus)} de revenus, ${Fmt.montant(courant.totalDepenses)} de dépenses, '
        'solde ${Fmt.montant(courant.solde)}. Demandez-moi par exemple « combien me reste-t-il ? » ou « un conseil ».';
  }
}
