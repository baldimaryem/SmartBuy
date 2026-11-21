import '../data/prix_models.dart';

/// Algorithme de comparaison d'une liste de courses (PRX-11, PRX-12, PRX-13, LNK-01).
///
/// Entrées :
/// - les lignes de la liste (produit + quantité) ;
/// - le prix **actuel** de chaque produit dans chaque magasin (dernier prix validé) ;
/// - le budget « Alimentation » restant (facultatif).
///
/// Étapes :
/// 1. pour chaque magasin, additionner quantité × prix ; noter les produits sans prix ;
/// 2. trier : magasins **complets** d'abord, puis par total croissant ;
/// 3. recommander le moins cher **parmi les complets** (un total incomplet paraît
///    artificiellement bas : il ne serait pas honnête de le recommander) ;
/// 4. économie = plus cher complet − moins cher complet ;
/// 5. comparer au budget restant.
///
/// Fonction pure : utilisée par le mode maquette et vérifiée par `test/comparateur_test.dart`.
/// Le back-end applique exactement la même règle.
Comparaison comparerListe({
  required int listeId,
  required List<LigneListe> lignes,
  required List<Magasin> magasins,
  required double? Function(int produitId, int magasinId) prixActuel,
  double? budgetRestant,
}) {
  final totaux = <TotalMagasin>[];
  for (final m in magasins) {
    var total = 0.0;
    final manquants = <String>[];
    final details = <DetailComparaison>[];
    for (final l in lignes) {
      final p = prixActuel(l.produitId, m.id);
      if (p == null) {
        manquants.add(l.produitNom);
      } else {
        total += p * l.quantite;
        details.add(DetailComparaison(l.produitId, l.produitNom, l.quantite, p));
      }
    }
    // Un magasin qui n'a aucun produit de la liste n'apporte rien à la comparaison.
    if (details.isEmpty) continue;
    totaux.add(TotalMagasin(
      magasinId: m.id,
      magasinNom: m.nom,
      enseigne: m.enseigne,
      total: _arrondi(total),
      complet: manquants.isEmpty,
      produitsManquants: manquants,
      details: details,
    ));
  }

  totaux.sort((a, b) {
    if (a.complet != b.complet) return a.complet ? -1 : 1;
    return a.total.compareTo(b.total);
  });

  final complets = totaux.where((t) => t.complet).toList();
  final recommande = complets.isEmpty ? null : complets.first;
  final economie = complets.length < 2 ? 0.0 : _arrondi(complets.last.total - complets.first.total);

  return Comparaison(
    listeId: listeId,
    magasins: totaux,
    magasinRecommandeId: recommande?.magasinId,
    economie: economie,
    budgetRestant: budgetRestant,
    depasseBudget: recommande != null && budgetRestant != null && recommande.total > budgetRestant,
  );
}

/// Arrondi au millime (3 décimales) pour éviter 41.199999999.
double _arrondi(double v) => (v * 1000).roundToDouble() / 1000;
