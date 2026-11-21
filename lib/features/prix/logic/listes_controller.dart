import 'package:flutter/foundation.dart';

import '../../budget/data/budget_models.dart';
import '../data/prix_models.dart';
import '../data/prix_repository.dart';

/// État des listes de courses (PRX-09..13) et du parcours « liste → comparaison → achat » (LNK-01..03).
class ListesController extends ChangeNotifier {
  ListesController(this._repo);
  final PrixRepository _repo;

  List<ListeResume> listes = [];
  bool chargement = false;
  String? erreur;

  Future<void> charger() async {
    chargement = true;
    erreur = null;
    notifyListeners();
    try {
      listes = await _repo.listes();
    } catch (e) {
      erreur = e.toString();
    } finally {
      chargement = false;
      notifyListeners();
    }
  }

  /// L'univers est fixé à la création : il décide des produits acceptés et du budget débité à l'achat.
  Future<ListeResume> creer(String nom, {Univers univers = Univers.alimentation}) async {
    final l = await _repo.enregistrerListe(nom: nom, univers: univers);
    await charger();
    return l;
  }

  Future<void> renommer(int id, String nom) async {
    await _repo.enregistrerListe(id: id, nom: nom);
    await charger();
  }

  Future<void> supprimer(int id) async {
    await _repo.supprimerListe(id);
    await charger();
  }

  Future<ListeDetail> detail(int id) => _repo.liste(id);

  Future<ListeDetail> ajouter(int listeId, Produit p, double quantite) async {
    final d = await _repo.ajouterLigne(listeId, produitId: p.id, quantite: quantite);
    charger(); // met à jour le nombre d'articles en arrière-plan
    return d;
  }

  Future<ListeDetail> modifierQuantite(int listeId, LigneListe l, double quantite) =>
      quantite <= 0 ? retirer(listeId, l) : _repo.modifierLigne(listeId, l.id, quantite);

  Future<ListeDetail> retirer(int listeId, LigneListe l) async {
    final d = await _repo.supprimerLigne(listeId, l.id);
    charger();
    return d;
  }

  Future<Comparaison> comparer(int listeId) => _repo.comparer(listeId);

  Future<Operation> acheter(int listeId, int magasinId) async {
    final op = await _repo.acheter(listeId, magasinId);
    await charger();
    return op;
  }
}
