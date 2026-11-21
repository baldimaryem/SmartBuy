import 'package:flutter_app/features/budget/logic/budget_calculs.dart';
import 'package:flutter_test/flutter_test.dart';

/// Tests unitaires des règles de budget (BUD-11, BUD-15, BUD-16).
void main() {
  group('Prévision de fin de mois (BUD-15)', () {
    test('extrapole le rythme actuel', () {
      // 300 DT en 10 jours sur un mois de 30 jours → 900 DT
      expect(BudgetCalculs.prevoirFinDeMois(depense: 300, jourCourant: 10, joursDansMois: 30), 900);
    });
    test('le dernier jour, la prévision est le réel', () {
      expect(BudgetCalculs.prevoirFinDeMois(depense: 850, jourCourant: 30, joursDansMois: 30), 850);
    });
    test('jour 0 : pas de division par zéro', () {
      expect(BudgetCalculs.prevoirFinDeMois(depense: 0, jourCourant: 0, joursDansMois: 31), 0);
    });
    test('nombre de jours du mois (février bissextile)', () {
      expect(BudgetCalculs.joursDansMois(2028, 2), 29);
      expect(BudgetCalculs.joursDansMois(2026, 11), 30);
    });
  });

  group('Seuils d\'alerte (BUD-11)', () {
    test('seuil atteint', () {
      expect(BudgetCalculs.seuilAtteint(79, 100), 0);
      expect(BudgetCalculs.seuilAtteint(80, 100), 80);
      expect(BudgetCalculs.seuilAtteint(120, 100), 100);
      expect(BudgetCalculs.seuilAtteint(50, 0), 0);
    });
    test('une seule alerte par seuil franchi', () {
      expect(BudgetCalculs.nouveauxSeuils(avant: 70, apres: 85, plafond: 100), [80]);
      expect(BudgetCalculs.nouveauxSeuils(avant: 85, apres: 90, plafond: 100), isEmpty);
      expect(BudgetCalculs.nouveauxSeuils(avant: 85, apres: 101, plafond: 100), [100]);
      expect(BudgetCalculs.nouveauxSeuils(avant: 10, apres: 150, plafond: 100), [80, 100]);
    });
  });

  test('variation par rapport à la moyenne (BUD-16)', () {
    expect(BudgetCalculs.variation(130, 100), closeTo(30, 0.001));
    expect(BudgetCalculs.variation(50, 0), isNull);
  });
}
