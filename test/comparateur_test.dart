import 'package:flutter_app/features/prix/data/prix_models.dart';
import 'package:flutter_app/features/prix/logic/comparateur.dart';
import 'package:flutter_test/flutter_test.dart';

/// Tests de l'algorithme de comparaison d'une liste (PRX-11, PRX-12, PRX-13, LNK-01).
void main() {
  const magasins = [
    Magasin(id: 1, nom: 'A', enseigne: 'A'),
    Magasin(id: 2, nom: 'B', enseigne: 'B'),
    Magasin(id: 3, nom: 'C', enseigne: 'C'),
  ];
  const lignes = [
    LigneListe(id: 1, produitId: 10, produitNom: 'Lait', quantite: 6),
    LigneListe(id: 2, produitId: 20, produitNom: 'Pâtes', quantite: 2),
  ];
  // Magasin A : complet 6×1.5 + 2×1 = 11 ; B : complet 6×1.3 + 2×1.2 = 10.2 ; C : sans pâtes, 6×1
  const prix = {(10, 1): 1.5, (20, 1): 1.0, (10, 2): 1.3, (20, 2): 1.2, (10, 3): 1.0};
  double? prixActuel(int p, int m) => prix[(p, m)];

  test('calcule le total par magasin et recommande le moins cher complet', () {
    final c = comparerListe(listeId: 1, lignes: lignes, magasins: magasins, prixActuel: prixActuel);
    expect(c.magasins.map((m) => m.magasinNom), ['B', 'A', 'C']);
    expect(c.magasins.first.total, closeTo(10.2, 0.0001));
    expect(c.magasinRecommandeId, 2);
    expect(c.economie, closeTo(0.8, 0.0001));
  });

  test('un magasin incomplet n\'est jamais recommandé, même moins cher (PRX-13)', () {
    final c = comparerListe(listeId: 1, lignes: lignes, magasins: magasins, prixActuel: prixActuel);
    final magasinC = c.magasins.firstWhere((m) => m.magasinId == 3);
    expect(magasinC.complet, isFalse);
    expect(magasinC.produitsManquants, ['Pâtes']);
    expect(c.magasinRecommandeId, isNot(3));
  });

  test('signale le dépassement du budget restant (LNK-01)', () {
    expect(comparerListe(listeId: 1, lignes: lignes, magasins: magasins, prixActuel: prixActuel, budgetRestant: 10).depasseBudget, isTrue);
    expect(comparerListe(listeId: 1, lignes: lignes, magasins: magasins, prixActuel: prixActuel, budgetRestant: 50).depasseBudget, isFalse);
  });

  test('aucun magasin complet : pas de recommandation ni d\'économie', () {
    final c = comparerListe(listeId: 1, lignes: lignes, magasins: [magasins[2]], prixActuel: prixActuel);
    expect(c.magasinRecommandeId, isNull);
    expect(c.economie, 0);
  });

  test('liste vide : aucun magasin', () {
    expect(comparerListe(listeId: 1, lignes: const [], magasins: magasins, prixActuel: prixActuel).magasins, isEmpty);
  });
}
