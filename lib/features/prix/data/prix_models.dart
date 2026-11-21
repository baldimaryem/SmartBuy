import 'package:flutter/material.dart';

import '../../../shared/utils/formatters.dart';

/// Modèles du module Comparaison des prix (voir `docs/07-contrat-api.md`, dont la section « Évolution v3 »).
///
/// Règle commune : les champs ajoutés en v3 sont **facultatifs** à la lecture (valeur par défaut),
/// pour que l'application fonctionne encore avec un serveur qui ne les envoie pas.

/// Univers de courses : chaque rayon, produit et liste appartient à un univers.
/// Une liste ne mélange pas les univers : le budget débité à l'achat en dépend.
enum Univers {
  alimentation('ALIMENTATION', 'Alimentation', Icons.shopping_basket_outlined),
  vetements('VETEMENTS', 'Vêtements', Icons.checkroom_outlined),
  highTech('HIGH_TECH', 'High-tech', Icons.devices_outlined);

  const Univers(this.code, this.libelle, this.icone);

  /// Valeur échangée avec l'API (`ALIMENTATION`, `VETEMENTS`, `HIGH_TECH`).
  final String code;
  final String libelle;
  final IconData icone;

  /// Catégorie de budget débitée quand on achète une liste de cet univers (LNK-01 / LNK-02).
  /// Elle porte le même nom que l'univers : c'est le serveur qui fait le lien, on ne fait que l'afficher.
  String get categorieBudget => libelle;

  /// Code inconnu ou absent (ancien serveur) → alimentation, l'univers historique de l'application.
  static Univers parse(Object? code) =>
      Univers.values.where((u) => u.code == code).firstOrNull ?? Univers.alimentation;
}

class Magasin {
  const Magasin({
    required this.id,
    required this.nom,
    required this.enseigne,
    this.adresse = '',
    this.ville = '',
    this.enLigne = false,
    this.siteWeb,
  });

  final int id;
  final String nom;
  final String enseigne;
  final String adresse;
  final String ville;

  /// Magasin « en ligne » d'une enseigne : c'est là que sont rangés les prix collectés sur son site.
  final bool enLigne;
  final String? siteWeb;

  factory Magasin.fromJson(Map<String, dynamic> j) => Magasin(
        id: j['id'] as int,
        nom: j['nom'] as String,
        enseigne: j['enseigne'] as String? ?? '',
        adresse: j['adresse'] as String? ?? '',
        ville: j['ville'] as String? ?? '',
        enLigne: j['enLigne'] as bool? ?? false,
        siteWeb: j['siteWeb'] as String?,
      );

  // enLigne / siteWeb sont renvoyés tels quels : renommer un magasin en ligne ne doit pas les effacer.
  Map<String, dynamic> toJson() =>
      {'nom': nom, 'enseigne': enseigne, 'adresse': adresse, 'ville': ville, 'enLigne': enLigne, 'siteWeb': siteWeb};
}

/// Rayon du catalogue (« Produits laitiers », « Smartphones »…).
class CategorieProduit {
  const CategorieProduit(this.id, this.nom, {this.univers = Univers.alimentation});
  final int id;
  final String nom;
  final Univers univers;

  factory CategorieProduit.fromJson(Map<String, dynamic> j) =>
      CategorieProduit(j['id'] as int, j['nom'] as String, univers: Univers.parse(j['univers']));
}

class Produit {
  const Produit({
    required this.id,
    required this.nom,
    this.marque = '',
    this.codeBarres,
    this.unite = 'pièce',
    this.categorieProduitId,
    this.categorieProduitNom = '',
    this.imageUrl,
    this.meilleurPrix,
    this.meilleurMagasinNom,
    this.univers = Univers.alimentation,
    this.source = 'CATALOGUE',
    this.urlSource,
  });

  final int id;
  final String nom;
  final String marque;
  final String? codeBarres;
  final String unite;
  final int? categorieProduitId;
  final String categorieProduitNom;
  final String? imageUrl;
  final double? meilleurPrix;
  final String? meilleurMagasinNom;
  final Univers univers;

  /// D'où vient la fiche : CATALOGUE (saisie admin), OPEN_FOOD_FACTS (trouvée par code-barres),
  /// COLLECTE (site marchand) ou UTILISATEUR (contribution).
  final String source;

  /// Page du produit sur le site marchand (produits collectés), sinon null.
  final String? urlSource;

  bool get deOpenFoodFacts => source == 'OPEN_FOOD_FACTS';

  /// Libellé affiché sur la fiche produit pour expliquer l'origine des données.
  String get libelleSource => switch (source) {
        'OPEN_FOOD_FACTS' => 'Open Food Facts',
        'COLLECTE' => 'Collecté sur ${_siteSource ?? 'un site marchand'}',
        'UTILISATEUR' => 'Ajouté par un utilisateur',
        _ => 'Catalogue SmartBuy',
      };

  /// « https://www.carrefour.tn/xxx » → « carrefour.tn ».
  String? get _siteSource {
    final hote = Uri.tryParse(urlSource ?? '')?.host.replaceFirst('www.', '') ?? '';
    return hote.isEmpty ? null : hote;
  }

  /// Copie avec le meilleur prix calculé (utilisé par la maquette).
  Produit avecMeilleurPrix(double? prix, String? magasinNom) => Produit(
        id: id, nom: nom, marque: marque, codeBarres: codeBarres, unite: unite,
        categorieProduitId: categorieProduitId, categorieProduitNom: categorieProduitNom, imageUrl: imageUrl,
        meilleurPrix: prix, meilleurMagasinNom: magasinNom, univers: univers, source: source, urlSource: urlSource,
      );

  factory Produit.fromJson(Map<String, dynamic> j) => Produit(
        id: j['id'] as int,
        nom: j['nom'] as String,
        marque: j['marque'] as String? ?? '',
        codeBarres: j['codeBarres'] as String?,
        unite: j['unite'] as String? ?? 'pièce',
        categorieProduitId: j['categorieProduitId'] as int?,
        categorieProduitNom: j['categorieProduitNom'] as String? ?? '',
        imageUrl: j['imageUrl'] as String?,
        meilleurPrix: toDoubleOrNull(j['meilleurPrix']),
        meilleurMagasinNom: j['meilleurMagasinNom'] as String?,
        univers: Univers.parse(j['univers']),
        source: j['source'] as String? ?? 'CATALOGUE',
        urlSource: j['urlSource'] as String?,
      );

  // Pas d'univers ici : le serveur le déduit du rayon (categorieProduitId).
  Map<String, dynamic> toJson() => {
        'nom': nom,
        'marque': marque,
        'codeBarres': codeBarres,
        'unite': unite,
        'categorieProduitId': categorieProduitId,
        'imageUrl': imageUrl,
      };
}

/// Prix actuel d'un produit dans un magasin (PRX-04).
class PrixMagasin {
  const PrixMagasin({
    required this.prixId,
    required this.magasinId,
    required this.magasinNom,
    required this.montant,
    required this.dateReleve,
    this.enseigne = '',
    this.meilleur = false,
    this.source = 'ADMIN',
  });

  final int prixId;
  final int magasinId;
  final String magasinNom;
  final String enseigne;
  final double montant;
  final DateTime dateReleve;
  final bool meilleur;

  /// ADMIN, UTILISATEUR ou COLLECTE (prix réel lu sur le site de l'enseigne).
  final String source;
  bool get collecte => source == 'COLLECTE';

  factory PrixMagasin.fromJson(Map<String, dynamic> j) => PrixMagasin(
        prixId: j['prixId'] as int,
        magasinId: j['magasinId'] as int,
        magasinNom: j['magasinNom'] as String,
        enseigne: j['enseigne'] as String? ?? '',
        montant: toDouble(j['montant']),
        dateReleve: DateTime.parse(j['dateReleve'] as String),
        meilleur: j['meilleur'] as bool? ?? false,
        source: j['source'] as String? ?? 'ADMIN',
      );
}

class PointHistorique {
  const PointHistorique(this.magasinNom, this.montant, this.dateReleve);
  final String magasinNom;
  final double montant;
  final DateTime dateReleve;

  factory PointHistorique.fromJson(Map<String, dynamic> j) =>
      PointHistorique(j['magasinNom'] as String, toDouble(j['montant']), DateTime.parse(j['dateReleve'] as String));
}

/// Un relevé de prix (proposé par un utilisateur, saisi par l'admin ou collecté en ligne).
class Prix {
  const Prix({
    required this.id,
    required this.produitId,
    required this.produitNom,
    required this.magasinId,
    required this.magasinNom,
    required this.montant,
    required this.dateReleve,
    this.source = 'UTILISATEUR',
    this.statut = 'EN_ATTENTE',
    this.proposePar,
  });

  final int id;
  final int produitId;
  final String produitNom;
  final int magasinId;
  final String magasinNom;
  final double montant;
  final DateTime dateReleve;
  final String source;
  final String statut;
  final String? proposePar;

  factory Prix.fromJson(Map<String, dynamic> j) => Prix(
        id: j['id'] as int,
        produitId: j['produitId'] as int,
        produitNom: j['produitNom'] as String? ?? '',
        magasinId: j['magasinId'] as int,
        magasinNom: j['magasinNom'] as String? ?? '',
        montant: toDouble(j['montant']),
        dateReleve: DateTime.parse(j['dateReleve'] as String),
        source: j['source'] as String? ?? 'UTILISATEUR',
        statut: j['statut'] as String? ?? 'EN_ATTENTE',
        proposePar: j['proposePar'] as String?,
      );

  Prix avecStatut(String nouveau) => Prix(
        id: id,
        produitId: produitId,
        produitNom: produitNom,
        magasinId: magasinId,
        magasinNom: magasinNom,
        montant: montant,
        dateReleve: dateReleve,
        source: source,
        statut: nouveau,
        proposePar: proposePar,
      );
}

class ListeResume {
  const ListeResume({
    required this.id,
    required this.nom,
    required this.dateCreation,
    this.statut = 'EN_COURS',
    this.nbArticles = 0,
    this.univers = Univers.alimentation,
  });

  final int id;
  final String nom;
  final DateTime dateCreation;
  final String statut;
  final int nbArticles;
  final Univers univers;

  bool get achetee => statut == 'ACHETEE';

  factory ListeResume.fromJson(Map<String, dynamic> j) => ListeResume(
        id: j['id'] as int,
        nom: j['nom'] as String,
        dateCreation: DateTime.parse(j['dateCreation'] as String),
        statut: j['statut'] as String? ?? 'EN_COURS',
        nbArticles: j['nbArticles'] as int? ?? 0,
        univers: Univers.parse(j['univers']),
      );
}

class LigneListe {
  const LigneListe({required this.id, required this.produitId, required this.produitNom, required this.quantite, this.unite = ''});

  final int id;
  final int produitId;
  final String produitNom;
  final String unite;
  final double quantite;

  factory LigneListe.fromJson(Map<String, dynamic> j) => LigneListe(
        id: j['id'] as int,
        produitId: j['produitId'] as int,
        produitNom: j['produitNom'] as String,
        unite: j['unite'] as String? ?? '',
        quantite: toDouble(j['quantite']),
      );
}

class ListeDetail {
  const ListeDetail({
    required this.id,
    required this.nom,
    required this.statut,
    required this.dateCreation,
    required this.lignes,
    this.univers = Univers.alimentation,
  });

  final int id;
  final String nom;
  final String statut;
  final DateTime dateCreation;
  final List<LigneListe> lignes;
  final Univers univers;

  bool get achetee => statut == 'ACHETEE';

  factory ListeDetail.fromJson(Map<String, dynamic> j) => ListeDetail(
        id: j['id'] as int,
        nom: j['nom'] as String,
        statut: j['statut'] as String? ?? 'EN_COURS',
        dateCreation: DateTime.parse(j['dateCreation'] as String),
        lignes: [for (final l in (j['lignes'] as List? ?? const [])) LigneListe.fromJson(l as Map<String, dynamic>)],
        univers: Univers.parse(j['univers']),
      );
}

class DetailComparaison {
  const DetailComparaison(this.produitId, this.produitNom, this.quantite, this.prixUnitaire);
  final int produitId;
  final String produitNom;
  final double quantite;
  final double prixUnitaire;
  double get sousTotal => quantite * prixUnitaire;
}

class TotalMagasin {
  const TotalMagasin({
    required this.magasinId,
    required this.magasinNom,
    required this.total,
    required this.complet,
    this.enseigne = '',
    this.produitsManquants = const [],
    this.details = const [],
  });

  final int magasinId;
  final String magasinNom;
  final String enseigne;
  final double total;
  final bool complet;
  final List<String> produitsManquants;
  final List<DetailComparaison> details;

  factory TotalMagasin.fromJson(Map<String, dynamic> j) => TotalMagasin(
        magasinId: j['magasinId'] as int,
        magasinNom: j['magasinNom'] as String,
        enseigne: j['enseigne'] as String? ?? '',
        total: toDouble(j['total']),
        complet: j['complet'] as bool? ?? false,
        produitsManquants: [for (final p in (j['produitsManquants'] as List? ?? const [])) p as String],
        details: [
          for (final d in (j['details'] as List? ?? const []))
            DetailComparaison(d['produitId'] as int, d['produitNom'] as String, toDouble(d['quantite']), toDouble(d['prixUnitaire'])),
        ],
      );
}

class Comparaison {
  const Comparaison({
    required this.listeId,
    required this.magasins,
    this.magasinRecommandeId,
    this.economie = 0,
    this.budgetRestant,
    this.depasseBudget = false,
  });

  final int listeId;
  final List<TotalMagasin> magasins;
  final int? magasinRecommandeId;
  final double economie;

  /// Reste du budget de la catégorie de l'univers de la liste (Alimentation, Vêtements ou High-tech).
  final double? budgetRestant;
  final bool depasseBudget;

  TotalMagasin? get recommande =>
      magasins.where((m) => m.magasinId == magasinRecommandeId).firstOrNull;

  factory Comparaison.fromJson(Map<String, dynamic> j) => Comparaison(
        listeId: j['listeId'] as int,
        magasins: [for (final m in (j['magasins'] as List? ?? const [])) TotalMagasin.fromJson(m as Map<String, dynamic>)],
        magasinRecommandeId: j['magasinRecommandeId'] as int?,
        economie: toDouble(j['economie']),
        budgetRestant: toDoubleOrNull(j['budgetRestant']),
        depasseBudget: j['depasseBudget'] as bool? ?? false,
      );
}

/// Réponse de `GET /produits/code-barres/{code}` (404 si le code est inconnu partout).
class ResultatCodeBarres {
  const ResultatCodeBarres({required this.produit, this.origine = 'CATALOGUE', this.message = ''});

  final Produit produit;

  /// CATALOGUE (déjà connu) ou OPEN_FOOD_FACTS (trouvé en ligne et ajouté au catalogue à l'instant).
  final String origine;
  final String message;

  bool get ajouteAuCatalogue => origine == 'OPEN_FOOD_FACTS';

  factory ResultatCodeBarres.fromJson(Map<String, dynamic> j) => ResultatCodeBarres(
        produit: Produit.fromJson(j['produit'] as Map<String, dynamic>),
        origine: j['origine'] as String? ?? 'CATALOGUE',
        message: j['message'] as String? ?? '',
      );
}

/// Bilan d'une collecte de prix en ligne lancée par l'administrateur (`POST /admin/collecte`).
class ResultatCollecte {
  const ResultatCollecte({
    required this.source,
    this.produitsAnalyses = 0,
    this.produitsCrees = 0,
    this.prixEnregistres = 0,
    this.erreurs = const [],
    this.duree = '',
  });

  final String source;
  final int produitsAnalyses;
  final int produitsCrees;
  final int prixEnregistres;
  final List<String> erreurs;

  /// Texte prêt à afficher : le contrat ne fixe pas le format, on accepte des secondes ou une chaîne.
  final String duree;

  factory ResultatCollecte.fromJson(Map<String, dynamic> j) => ResultatCollecte(
        source: j['source'] as String? ?? '',
        produitsAnalyses: (j['produitsAnalyses'] as num?)?.toInt() ?? 0,
        produitsCrees: (j['produitsCrees'] as num?)?.toInt() ?? 0,
        prixEnregistres: (j['prixEnregistres'] as num?)?.toInt() ?? 0,
        erreurs: [for (final e in (j['erreurs'] as List? ?? const [])) '$e'],
        duree: _duree(j['duree']),
      );

  static String _duree(Object? v) => switch (v) {
        null => '',
        num n => '${n.round()} s',
        // Durée ISO 8601 (« PT1M5.2S ») si le serveur sérialise un java.time.Duration.
        String s when s.startsWith('PT') => s.substring(2).toLowerCase(),
        _ => '$v',
      };
}
