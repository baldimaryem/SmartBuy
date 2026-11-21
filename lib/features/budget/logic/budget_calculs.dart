/// Règles de calcul du budget, isolées dans des fonctions **pures**
/// (aucun accès réseau, aucun widget) : faciles à comprendre et à tester
/// (voir `test/budget_calculs_test.dart`).
///
/// Ce sont les mêmes règles que le back-end ; côté Flutter, elles servent au
/// mode maquette (suivi 1) et à l'affichage (couleurs des jauges).
class BudgetCalculs {
  BudgetCalculs._();

  /// BUD-15 : prévision de fin de mois par extrapolation linéaire.
  /// « Si je continue à ce rythme, combien aurai-je dépensé le dernier jour ? »
  ///   dépensé ÷ jours écoulés × jours du mois
  static double prevoirFinDeMois({required double depense, required int jourCourant, required int joursDansMois}) {
    if (jourCourant <= 0) return depense;
    if (jourCourant >= joursDansMois) return depense;
    return depense / jourCourant * joursDansMois;
  }

  static int joursDansMois(int annee, int mois) => DateTime(annee, mois + 1, 0).day;

  /// BUD-11 : seuil d'alerte franchi (0 = aucun, 80, 100).
  static int seuilAtteint(double depense, double plafond) {
    if (plafond <= 0) return 0;
    final p = depense / plafond * 100;
    if (p >= 100) return 100;
    if (p >= 80) return 80;
    return 0;
  }

  /// Seuils franchis par une nouvelle dépense : ceux atteints **après** et pas **avant**.
  /// Permet de n'envoyer qu'une alerte par seuil.
  static List<int> nouveauxSeuils({required double avant, required double apres, required double plafond}) => [
        for (final s in const [80, 100])
          if (plafond > 0 && avant / plafond * 100 < s && apres / plafond * 100 >= s) s,
      ];

  /// Variation en % entre une valeur et une moyenne de référence (BUD-16).
  static double? variation(double valeur, double reference) =>
      reference <= 0 ? null : (valeur - reference) / reference * 100;
}
