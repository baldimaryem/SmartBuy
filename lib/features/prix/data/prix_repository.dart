import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../../../shared/mock/mock_store.dart';
import '../../../shared/utils/formatters.dart';
import '../../budget/data/budget_models.dart';
import '../logic/comparateur.dart';
import 'prix_models.dart';

/// Accès aux données du module Comparaison des prix (PRX, ADM, LNK + évolution v3).
abstract interface class PrixRepository {
  // Catalogue
  Future<List<Magasin>> magasins();
  Future<List<CategorieProduit>> categoriesProduits({Univers? univers});
  Future<List<Produit>> produits({String? recherche, int? categorieId, String? codeBarres, Univers? univers});
  Future<Produit> produit(int id);
  Future<List<PrixMagasin>> prixParMagasin(int produitId);
  Future<List<PointHistorique>> historique(int produitId);
  Future<Prix> proposerPrix({required int produitId, required int magasinId, required double montant, required DateTime date});

  /// Code-barres : catalogue, puis Open Food Facts. Lève une [ApiException] 404 si le code est inconnu partout.
  Future<ResultatCodeBarres> produitParCodeBarres(String code);

  /// Ajout d'un produit introuvable par un utilisateur connecté (source UTILISATEUR).
  Future<Produit> contribuerProduit({
    required String nom,
    String marque = '',
    String? codeBarres,
    String unite = 'pièce',
    required int categorieProduitId,
  });

  /// Relance la collecte en ligne pour un produit et renvoie ses prix à jour.
  Future<List<PrixMagasin>> actualiserPrix(int produitId);

  // Administration (ADM-01..04, PRX-08)
  Future<Magasin> enregistrerMagasin(Magasin m);
  Future<void> supprimerMagasin(int id);
  Future<Produit> enregistrerProduit(Produit p);
  Future<void> supprimerProduit(int id);
  Future<CategorieProduit> enregistrerCategorieProduit({int? id, required String nom, Univers? univers});
  Future<void> supprimerCategorieProduit(int id);
  Future<List<Prix>> prixEnAttente();
  Future<Prix> deciderPrix(int id, {required bool valider});

  /// Collecte des prix en ligne : [source] = `CARREFOUR` ou `SPACENET`.
  Future<ResultatCollecte> lancerCollecte(String source);

  // Listes de courses
  Future<List<ListeResume>> listes();

  /// Création (avec son univers) ou renommage (l'univers d'une liste existante ne change pas).
  Future<ListeResume> enregistrerListe({int? id, required String nom, Univers univers = Univers.alimentation});
  Future<void> supprimerListe(int id);
  Future<ListeDetail> liste(int id);

  /// 400 si le produit n'est pas du même univers que la liste.
  Future<ListeDetail> ajouterLigne(int listeId, {required int produitId, required double quantite});
  Future<ListeDetail> modifierLigne(int listeId, int ligneId, double quantite);
  Future<ListeDetail> supprimerLigne(int listeId, int ligneId);
  Future<Comparaison> comparer(int listeId);
  Future<Operation> acheter(int listeId, int magasinId);
}

// =========================================================================== API
class ApiPrixRepository implements PrixRepository {
  ApiPrixRepository(this._api);
  final ApiClient _api;

  /// La collecte complète interroge des sites à 1 requête/s : elle peut durer plus d'une minute.
  static const _delaiCollecte = Duration(minutes: 3);

  /// Actualiser un seul produit = quelques requêtes vers les sites marchands.
  static const _delaiActualisation = Duration(minutes: 1);

  List<T> _liste<T>(dynamic json, T Function(Map<String, dynamic>) f) =>
      [for (final e in json as List) f(e as Map<String, dynamic>)];
  Map<String, dynamic> _objet(dynamic json) => json as Map<String, dynamic>;

  @override
  Future<List<Magasin>> magasins() async => _liste(await _api.get('/magasins'), Magasin.fromJson);

  @override
  Future<List<CategorieProduit>> categoriesProduits({Univers? univers}) async =>
      _liste(await _api.get('/categories-produits', query: {'univers': univers?.code}), CategorieProduit.fromJson);

  @override
  Future<List<Produit>> produits({String? recherche, int? categorieId, String? codeBarres, Univers? univers}) async => _liste(
      await _api.get('/produits', query: {
        'univers': univers?.code,
        'q': recherche,
        'categorieId': categorieId,
        'codeBarres': codeBarres,
      }),
      Produit.fromJson);

  @override
  Future<Produit> produit(int id) async => Produit.fromJson(_objet(await _api.get('/produits/$id')));

  @override
  Future<List<PrixMagasin>> prixParMagasin(int produitId) async =>
      _liste(await _api.get('/produits/$produitId/prix'), PrixMagasin.fromJson);

  @override
  Future<List<PointHistorique>> historique(int produitId) async =>
      _liste(await _api.get('/produits/$produitId/historique'), PointHistorique.fromJson);

  @override
  Future<Prix> proposerPrix({required int produitId, required int magasinId, required double montant, required DateTime date}) async =>
      Prix.fromJson(_objet(await _api.post('/prix', body: {
        'produitId': produitId,
        'magasinId': magasinId,
        'montant': montant,
        'dateReleve': Fmt.isoJour(date),
      })));

  // Le serveur peut interroger Open Food Facts puis Carrefour : on lui laisse le délai d'une actualisation.
  @override
  Future<ResultatCodeBarres> produitParCodeBarres(String code) async => ResultatCodeBarres.fromJson(
      _objet(await _api.get('/produits/code-barres/${Uri.encodeComponent(code.trim())}', timeout: _delaiActualisation)));

  @override
  Future<Produit> contribuerProduit({
    required String nom,
    String marque = '',
    String? codeBarres,
    String unite = 'pièce',
    required int categorieProduitId,
  }) async =>
      Produit.fromJson(_objet(await _api.post('/produits/contribution', body: {
        'nom': nom,
        'marque': marque,
        'codeBarres': codeBarres,
        'unite': unite,
        'categorieProduitId': categorieProduitId,
      })));

  @override
  Future<List<PrixMagasin>> actualiserPrix(int produitId) async => _liste(
      await _api.post('/produits/$produitId/actualiser-prix', timeout: _delaiActualisation), PrixMagasin.fromJson);

  @override
  Future<Magasin> enregistrerMagasin(Magasin m) async => Magasin.fromJson(_objet(
      m.id == 0 ? await _api.post('/magasins', body: m.toJson()) : await _api.put('/magasins/${m.id}', body: m.toJson())));

  @override
  Future<void> supprimerMagasin(int id) => _api.delete('/magasins/$id');

  @override
  Future<Produit> enregistrerProduit(Produit p) async => Produit.fromJson(_objet(
      p.id == 0 ? await _api.post('/produits', body: p.toJson()) : await _api.put('/produits/${p.id}', body: p.toJson())));

  @override
  Future<void> supprimerProduit(int id) => _api.delete('/produits/$id');

  @override
  Future<CategorieProduit> enregistrerCategorieProduit({int? id, required String nom, Univers? univers}) async {
    final corps = {'nom': nom, 'univers': ?univers?.code};
    return CategorieProduit.fromJson(_objet(id == null
        ? await _api.post('/categories-produits', body: corps)
        : await _api.put('/categories-produits/$id', body: corps)));
  }

  @override
  Future<void> supprimerCategorieProduit(int id) => _api.delete('/categories-produits/$id');

  @override
  Future<List<Prix>> prixEnAttente() async => _liste(await _api.get('/prix/en-attente'), Prix.fromJson);

  @override
  Future<Prix> deciderPrix(int id, {required bool valider}) async =>
      Prix.fromJson(_objet(await _api.put('/prix/$id/${valider ? 'valider' : 'rejeter'}')));

  @override
  Future<ResultatCollecte> lancerCollecte(String source) async => ResultatCollecte.fromJson(
      _objet(await _api.post('/admin/collecte', query: {'source': source}, timeout: _delaiCollecte)));

  @override
  Future<List<ListeResume>> listes() async => _liste(await _api.get('/listes'), ListeResume.fromJson);

  @override
  Future<ListeResume> enregistrerListe({int? id, required String nom, Univers univers = Univers.alimentation}) async =>
      ListeResume.fromJson(_objet(id == null
          ? await _api.post('/listes', body: {'nom': nom, 'univers': univers.code})
          : await _api.put('/listes/$id', body: {'nom': nom})));

  @override
  Future<void> supprimerListe(int id) => _api.delete('/listes/$id');

  @override
  Future<ListeDetail> liste(int id) async => ListeDetail.fromJson(_objet(await _api.get('/listes/$id')));

  @override
  Future<ListeDetail> ajouterLigne(int listeId, {required int produitId, required double quantite}) async =>
      ListeDetail.fromJson(_objet(
          await _api.post('/listes/$listeId/lignes', body: {'produitId': produitId, 'quantite': quantite})));

  @override
  Future<ListeDetail> modifierLigne(int listeId, int ligneId, double quantite) async =>
      ListeDetail.fromJson(_objet(await _api.put('/listes/$listeId/lignes/$ligneId', body: {'quantite': quantite})));

  @override
  Future<ListeDetail> supprimerLigne(int listeId, int ligneId) async =>
      ListeDetail.fromJson(_objet(await _api.delete('/listes/$listeId/lignes/$ligneId')));

  @override
  Future<Comparaison> comparer(int listeId) async =>
      Comparaison.fromJson(_objet(await _api.get('/listes/$listeId/comparaison')));

  @override
  Future<Operation> acheter(int listeId, int magasinId) async =>
      Operation.fromJson(_objet(await _api.post('/listes/$listeId/acheter', body: {'magasinId': magasinId})));
}

// =========================================================================== MAQUETTE
class MockPrixRepository implements PrixRepository {
  MockPrixRepository(this._db);
  final MockStore _db;

  bool get _admin => _db.utilisateurCourant?.estAdmin ?? false;
  void _exigerAdmin() {
    if (!_admin) throw ApiException('Action réservée à l\'administrateur', status: 403);
  }

  Produit _avecMeilleurPrix(Produit p) {
    double? meilleur;
    String? magasinNom;
    for (final m in _db.magasins) {
      final montant = _db.prixActuel(p.id, m.id);
      if (montant != null && (meilleur == null || montant < meilleur)) {
        meilleur = montant;
        magasinNom = m.nom;
      }
    }
    return p.avecMeilleurPrix(meilleur, magasinNom);
  }

  @override
  Future<List<Magasin>> magasins() async {
    await _db.latence();
    return List.of(_db.magasins);
  }

  @override
  Future<List<CategorieProduit>> categoriesProduits({Univers? univers}) async {
    await _db.latence();
    return _db.categoriesProduits.where((c) => univers == null || c.univers == univers).toList();
  }

  @override
  Future<List<Produit>> produits({String? recherche, int? categorieId, String? codeBarres, Univers? univers}) async {
    await _db.latence();
    final q = recherche?.trim().toLowerCase() ?? '';
    return _db.produits
        .where((p) =>
            (univers == null || p.univers == univers) &&
            (q.isEmpty || p.nom.toLowerCase().contains(q) || p.marque.toLowerCase().contains(q)) &&
            (categorieId == null || p.categorieProduitId == categorieId) &&
            (codeBarres == null || codeBarres.isEmpty || p.codeBarres == codeBarres))
        .map(_avecMeilleurPrix)
        .toList();
  }

  @override
  Future<Produit> produit(int id) async {
    await _db.latence();
    return _avecMeilleurPrix(_db.produit(id));
  }

  @override
  Future<List<PrixMagasin>> prixParMagasin(int produitId) async {
    await _db.latence();
    final liste = <PrixMagasin>[];
    for (final m in _db.magasins) {
      final releves = _db.prix.where((p) => p.produitId == produitId && p.magasinId == m.id && p.statut == 'VALIDE').toList()
        ..sort((a, b) => b.dateReleve.compareTo(a.dateReleve));
      if (releves.isEmpty) continue;
      final r = releves.first;
      liste.add(PrixMagasin(
        prixId: r.id, magasinId: m.id, magasinNom: m.nom, enseigne: m.enseigne, montant: r.montant,
        dateReleve: r.dateReleve, source: r.source,
      ));
    }
    liste.sort((a, b) => a.montant.compareTo(b.montant));
    return [
      for (final p in liste)
        PrixMagasin(
          prixId: p.prixId, magasinId: p.magasinId, magasinNom: p.magasinNom, enseigne: p.enseigne,
          montant: p.montant, dateReleve: p.dateReleve, source: p.source, meilleur: p.montant == liste.first.montant,
        ),
    ];
  }

  @override
  Future<List<PointHistorique>> historique(int produitId) async {
    await _db.latence();
    return [
      for (final p in _db.prix.where((p) => p.produitId == produitId && p.statut == 'VALIDE'))
        PointHistorique(p.magasinNom, p.montant, p.dateReleve),
    ]..sort((a, b) => a.dateReleve.compareTo(b.dateReleve));
  }

  @override
  Future<Prix> proposerPrix({required int produitId, required int magasinId, required double montant, required DateTime date}) async {
    await _db.latence();
    if (montant <= 0) throw ApiException('Le prix doit être positif', status: 400);
    final p = Prix(
      id: _db.nextId(),
      produitId: produitId,
      produitNom: _db.produit(produitId).nom,
      magasinId: magasinId,
      magasinNom: _db.magasin(magasinId)!.nom,
      montant: montant,
      dateReleve: date,
      source: _admin ? 'ADMIN' : 'UTILISATEUR',
      statut: _admin ? 'VALIDE' : 'EN_ATTENTE',
      proposePar: _db.utilisateurCourant?.nom,
    );
    _db.prix.add(p);
    return p;
  }

  /// Hors ligne, pas d'Open Food Facts : seul le catalogue local est consulté.
  @override
  Future<ResultatCodeBarres> produitParCodeBarres(String code) async {
    await _db.latence();
    final p = _db.produits.where((p) => p.codeBarres == code.trim()).firstOrNull;
    if (p == null) throw ApiException('Code-barres inconnu : aucun produit ne correspond à $code', status: 404);
    return ResultatCodeBarres(produit: _avecMeilleurPrix(p), origine: 'CATALOGUE', message: 'Produit trouvé dans le catalogue');
  }

  @override
  Future<Produit> contribuerProduit({
    required String nom,
    String marque = '',
    String? codeBarres,
    String unite = 'pièce',
    required int categorieProduitId,
  }) async {
    await _db.latence();
    if (nom.trim().isEmpty) throw ApiException('Le nom du produit est obligatoire', status: 400);
    final code = (codeBarres?.trim().isEmpty ?? true) ? null : codeBarres!.trim();
    if (code != null && _db.produits.any((p) => p.codeBarres == code)) {
      throw ApiException('Ce code-barres existe déjà', status: 409);
    }
    final rayon = _db.categoriesProduits.where((c) => c.id == categorieProduitId).firstOrNull;
    if (rayon == null) throw ApiException('Rayon introuvable', status: 400);
    final p = Produit(
      id: _db.nextId(), nom: nom.trim(), marque: marque.trim(), codeBarres: code, unite: unite.isEmpty ? 'pièce' : unite,
      categorieProduitId: rayon.id, categorieProduitNom: rayon.nom, univers: rayon.univers, source: 'UTILISATEUR',
    );
    _db.produits.add(p);
    return _avecMeilleurPrix(p);
  }

  /// La maquette ne contacte aucun site marchand : les prix actuels sont renvoyés tels quels.
  @override
  Future<List<PrixMagasin>> actualiserPrix(int produitId) => prixParMagasin(produitId);

  @override
  Future<Magasin> enregistrerMagasin(Magasin m) async {
    _exigerAdmin();
    await _db.latence();
    final nouveau = Magasin(
      id: m.id == 0 ? _db.nextId() : m.id, nom: m.nom, enseigne: m.enseigne, adresse: m.adresse, ville: m.ville,
      enLigne: m.enLigne, siteWeb: m.siteWeb,
    );
    final i = _db.magasins.indexWhere((x) => x.id == nouveau.id);
    i >= 0 ? _db.magasins[i] = nouveau : _db.magasins.add(nouveau);
    return nouveau;
  }

  @override
  Future<void> supprimerMagasin(int id) async {
    _exigerAdmin();
    await _db.latence();
    _db.magasins.removeWhere((m) => m.id == id);
    _db.prix.removeWhere((p) => p.magasinId == id);
  }

  @override
  Future<Produit> enregistrerProduit(Produit p) async {
    _exigerAdmin();
    await _db.latence();
    if (_db.produits.any((x) => x.id != p.id && p.codeBarres != null && x.codeBarres == p.codeBarres)) {
      throw ApiException('Ce code-barres existe déjà', status: 409);
    }
    final cat = _db.categoriesProduits.where((c) => c.id == p.categorieProduitId).firstOrNull;
    final ancien = _db.produits.where((x) => x.id == p.id).firstOrNull;
    final nouveau = Produit(
      id: p.id == 0 ? _db.nextId() : p.id, nom: p.nom, marque: p.marque, codeBarres: p.codeBarres, unite: p.unite,
      categorieProduitId: p.categorieProduitId, categorieProduitNom: cat?.nom ?? '', imageUrl: p.imageUrl,
      // L'univers suit le rayon (comme côté serveur) ; la source d'une fiche existante est conservée.
      univers: cat?.univers ?? Univers.alimentation, source: ancien?.source ?? 'CATALOGUE', urlSource: ancien?.urlSource,
    );
    final i = _db.produits.indexWhere((x) => x.id == nouveau.id);
    i >= 0 ? _db.produits[i] = nouveau : _db.produits.add(nouveau);
    return _avecMeilleurPrix(nouveau);
  }

  @override
  Future<void> supprimerProduit(int id) async {
    _exigerAdmin();
    await _db.latence();
    _db.produits.removeWhere((p) => p.id == id);
    _db.prix.removeWhere((p) => p.produitId == id);
  }

  @override
  Future<CategorieProduit> enregistrerCategorieProduit({int? id, required String nom, Univers? univers}) async {
    _exigerAdmin();
    await _db.latence();
    final i = _db.categoriesProduits.indexWhere((x) => x.id == id);
    final c = CategorieProduit(id ?? _db.nextId(), nom,
        univers: univers ?? (i >= 0 ? _db.categoriesProduits[i].univers : Univers.alimentation));
    i >= 0 ? _db.categoriesProduits[i] = c : _db.categoriesProduits.add(c);
    return c;
  }

  @override
  Future<void> supprimerCategorieProduit(int id) async {
    _exigerAdmin();
    await _db.latence();
    if (_db.produits.any((p) => p.categorieProduitId == id)) {
      throw ApiException('Catégorie utilisée par des produits', status: 409);
    }
    _db.categoriesProduits.removeWhere((c) => c.id == id);
  }

  @override
  Future<List<Prix>> prixEnAttente() async {
    _exigerAdmin();
    await _db.latence();
    return _db.prix.where((p) => p.statut == 'EN_ATTENTE').toList();
  }

  @override
  Future<Prix> deciderPrix(int id, {required bool valider}) async {
    _exigerAdmin();
    await _db.latence();
    final i = _db.prix.indexWhere((p) => p.id == id);
    _db.prix[i] = _db.prix[i].avecStatut(valider ? 'VALIDE' : 'REJETE');
    return _db.prix[i];
  }

  /// Pas de réseau en maquette : la collecte ne trouve rien, mais l'écran d'administration se teste.
  @override
  Future<ResultatCollecte> lancerCollecte(String source) async {
    _exigerAdmin();
    await _db.latence();
    return ResultatCollecte(source: source, duree: '0 s');
  }

  // ------------------------------------------------------------------ listes
  ListeDetail _detail(int id) {
    final l = _db.listes.firstWhere((l) => l.id == id, orElse: () => throw ApiException('Liste introuvable', status: 404));
    return ListeDetail(id: l.id, nom: l.nom, statut: l.statut, dateCreation: l.date, lignes: List.of(l.lignes), univers: l.univers);
  }

  ListeResume _resume(int id) {
    final l = _db.listes.firstWhere((l) => l.id == id);
    return ListeResume(id: l.id, nom: l.nom, dateCreation: l.date, statut: l.statut, nbArticles: l.lignes.length, univers: l.univers);
  }

  @override
  Future<List<ListeResume>> listes() async {
    await _db.latence();
    return [for (final l in _db.listes.reversed) _resume(l.id)];
  }

  @override
  Future<ListeResume> enregistrerListe({int? id, required String nom, Univers univers = Univers.alimentation}) async {
    await _db.latence();
    final i = _db.listes.indexWhere((l) => l.id == id);
    if (i >= 0) {
      final l = _db.listes[i];
      _db.listes[i] = (id: l.id, nom: nom, date: l.date, statut: l.statut, univers: l.univers, lignes: l.lignes);
      return _resume(l.id);
    }
    final nouvelId = _db.nextId();
    _db.listes.add((id: nouvelId, nom: nom, date: DateTime.now(), statut: 'EN_COURS', univers: univers, lignes: <LigneListe>[]));
    return _resume(nouvelId);
  }

  @override
  Future<void> supprimerListe(int id) async {
    await _db.latence();
    _db.listes.removeWhere((l) => l.id == id);
  }

  @override
  Future<ListeDetail> liste(int id) async {
    await _db.latence();
    return _detail(id);
  }

  List<LigneListe> _lignes(int listeId) => _db.listes.firstWhere((l) => l.id == listeId).lignes;

  @override
  Future<ListeDetail> ajouterLigne(int listeId, {required int produitId, required double quantite}) async {
    await _db.latence();
    final liste = _detail(listeId);
    final p = _db.produit(produitId);
    // Même règle que le serveur : une liste = un univers = une catégorie de budget à l'achat.
    if (p.univers != liste.univers) {
      throw ApiException(
          '« ${p.nom} » (${p.univers.libelle}) ne peut pas aller dans une liste ${liste.univers.libelle}', status: 400);
    }
    final lignes = _lignes(listeId);
    final i = lignes.indexWhere((l) => l.produitId == produitId);
    if (i >= 0) {
      final l = lignes[i];
      lignes[i] = LigneListe(id: l.id, produitId: l.produitId, produitNom: l.produitNom, unite: l.unite, quantite: l.quantite + quantite);
    } else {
      lignes.add(LigneListe(id: _db.nextId(), produitId: p.id, produitNom: p.nom, unite: p.unite, quantite: quantite));
    }
    return _detail(listeId);
  }

  @override
  Future<ListeDetail> modifierLigne(int listeId, int ligneId, double quantite) async {
    await _db.latence();
    final lignes = _lignes(listeId);
    final i = lignes.indexWhere((l) => l.id == ligneId);
    final l = lignes[i];
    lignes[i] = LigneListe(id: l.id, produitId: l.produitId, produitNom: l.produitNom, unite: l.unite, quantite: quantite);
    return _detail(listeId);
  }

  @override
  Future<ListeDetail> supprimerLigne(int listeId, int ligneId) async {
    await _db.latence();
    _lignes(listeId).removeWhere((l) => l.id == ligneId);
    return _detail(listeId);
  }

  /// Budget restant ce mois-ci dans la catégorie de l'univers de la liste (LNK-01).
  double? _budgetRestant(Univers univers) {
    final mois = Fmt.isoMois(DateTime.now());
    final categorieId = _db.categorieDeUnivers(univers).id;
    final b = _db.budgets.where((b) => b.mois == mois && b.categorieId == categorieId).firstOrNull;
    if (b == null) return null;
    final depense = _db.operations
        .where((o) => o.estDepense && o.categorieId == categorieId && Fmt.isoMois(o.dateOperation) == mois)
        .fold(0.0, (s, o) => s + o.montant);
    return b.plafond - depense;
  }

  @override
  Future<Comparaison> comparer(int listeId) async {
    await _db.latence();
    final detail = _detail(listeId);
    return comparerListe(
      listeId: listeId,
      lignes: detail.lignes,
      magasins: _db.magasins,
      prixActuel: _db.prixActuel,
      budgetRestant: _budgetRestant(detail.univers),
    );
  }

  /// LNK-02 / LNK-03 : la liste achetée devient une dépense dans la catégorie de son univers.
  @override
  Future<Operation> acheter(int listeId, int magasinId) async {
    final comparaison = await comparer(listeId);
    final choisi = comparaison.magasins.firstWhere((m) => m.magasinId == magasinId,
        orElse: () => throw ApiException('Aucun prix connu dans ce magasin', status: 400));
    final complets = comparaison.magasins.where((m) => m.complet).toList();
    final plusCher = complets.isEmpty ? choisi.total : complets.last.total;
    final detail = _detail(listeId);
    final categorie = _db.categorieDeUnivers(detail.univers);

    final operation = Operation(
      id: _db.nextId(),
      type: TypeOperation.depense,
      montant: choisi.total,
      dateOperation: DateTime.now(),
      categorieId: categorie.id,
      categorieNom: categorie.nom,
      note: 'Liste : ${detail.nom}',
      magasinId: magasinId,
      magasinNom: choisi.magasinNom,
      economie: (plusCher - choisi.total).clamp(0, double.infinity).toDouble(),
    );
    _db.operations.add(operation);
    final i = _db.listes.indexWhere((l) => l.id == listeId);
    final l = _db.listes[i];
    _db.listes[i] = (id: l.id, nom: l.nom, date: l.date, statut: 'ACHETEE', univers: l.univers, lignes: l.lignes);
    return operation;
  }
}
