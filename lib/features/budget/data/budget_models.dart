import '../../../shared/utils/formatters.dart';

/// Modèles du module Smart Budget. Chacun reflète un objet JSON du contrat d'API
/// (`docs/07-contrat-api.md`) : `fromJson` lit la réponse du serveur, `toJson`
/// prépare le corps des requêtes.

enum TypeOperation {
  depense('DEPENSE', 'Dépense'),
  revenu('REVENU', 'Revenu');

  const TypeOperation(this.code, this.libelle);
  final String code;
  final String libelle;

  static TypeOperation parse(String code) => values.firstWhere((t) => t.code == code);
}

class Categorie {
  const Categorie({
    required this.id,
    required this.nom,
    required this.type,
    this.icone,
    this.personnalisee = false,
  });

  final int id;
  final String nom;
  final TypeOperation type;
  final String? icone;
  final bool personnalisee;

  factory Categorie.fromJson(Map<String, dynamic> j) => Categorie(
        id: j['id'] as int,
        nom: j['nom'] as String,
        type: TypeOperation.parse(j['type'] as String),
        icone: j['icone'] as String?,
        personnalisee: j['personnalisee'] as bool? ?? false,
      );
}

class Operation {
  const Operation({
    required this.id,
    required this.type,
    required this.montant,
    required this.dateOperation,
    required this.categorieId,
    this.categorieNom = '',
    this.note,
    this.magasinId,
    this.magasinNom,
    this.recurrente = false,
    this.economie = 0,
  });

  final int id;
  final TypeOperation type;
  final double montant;
  final DateTime dateOperation;
  final int categorieId;
  final String categorieNom;
  final String? note;
  final int? magasinId;
  final String? magasinNom;
  final bool recurrente;
  final double economie;

  bool get estDepense => type == TypeOperation.depense;

  factory Operation.fromJson(Map<String, dynamic> j) => Operation(
        id: j['id'] as int,
        type: TypeOperation.parse(j['type'] as String),
        montant: toDouble(j['montant']),
        dateOperation: DateTime.parse(j['dateOperation'] as String),
        categorieId: j['categorieId'] as int,
        categorieNom: j['categorieNom'] as String? ?? '',
        note: j['note'] as String?,
        magasinId: j['magasinId'] as int?,
        magasinNom: j['magasinNom'] as String?,
        recurrente: j['recurrente'] as bool? ?? false,
        economie: toDouble(j['economie']),
      );

  /// Corps envoyé à `POST /operations` et `PUT /operations/{id}`.
  Map<String, dynamic> toRequete() => {
        'type': type.code,
        'montant': montant,
        'dateOperation': Fmt.isoJour(dateOperation),
        'note': note,
        'categorieId': categorieId,
        'magasinId': magasinId,
        'recurrente': recurrente,
      };

  Operation copyWith({int? id, String? categorieNom, String? magasinNom, double? economie}) => Operation(
        id: id ?? this.id,
        type: type,
        montant: montant,
        dateOperation: dateOperation,
        categorieId: categorieId,
        categorieNom: categorieNom ?? this.categorieNom,
        note: note,
        magasinId: magasinId,
        magasinNom: magasinNom ?? this.magasinNom,
        recurrente: recurrente,
        economie: economie ?? this.economie,
      );
}

class Budget {
  const Budget({
    required this.id,
    required this.mois,
    required this.montantPlafond,
    this.categorieId,
    this.categorieNom,
    this.depense = 0,
  });

  final int id;
  final String mois;
  final int? categorieId; // null = budget global
  final String? categorieNom;
  final double montantPlafond;
  final double depense;

  bool get estGlobal => categorieId == null;
  double get reste => montantPlafond - depense;
  double get pourcentage => montantPlafond <= 0 ? 0 : depense / montantPlafond * 100;

  factory Budget.fromJson(Map<String, dynamic> j) => Budget(
        id: j['id'] as int,
        mois: j['mois'] as String,
        categorieId: j['categorieId'] as int?,
        categorieNom: j['categorieNom'] as String?,
        montantPlafond: toDouble(j['montantPlafond']),
        depense: toDouble(j['depense']),
      );
}

class Prevision {
  const Prevision(this.depenseEstimeeFinMois, this.depassementProbable);
  final double depenseEstimeeFinMois;
  final bool depassementProbable;
}

class MontantCategorie {
  const MontantCategorie(this.categorieId, this.categorieNom, this.montant);
  final int categorieId;
  final String categorieNom;
  final double montant;
}

class MoisResume {
  const MoisResume(this.mois, this.depenses, this.revenus);
  final String mois;
  final double depenses;
  final double revenus;
}

class Dashboard {
  const Dashboard({
    required this.mois,
    required this.totalRevenus,
    required this.totalDepenses,
    required this.prevision,
    required this.repartition,
    required this.evolution,
    this.budgetGlobal,
    this.resteADepenser,
    this.economiesRealisees = 0,
    this.alertesNonLues = 0,
  });

  final String mois;
  final double totalRevenus;
  final double totalDepenses;
  final double? budgetGlobal;
  final double? resteADepenser;
  final Prevision prevision;
  final List<MontantCategorie> repartition;
  final List<MoisResume> evolution;
  final double economiesRealisees;
  final int alertesNonLues;

  double get solde => totalRevenus - totalDepenses;

  factory Dashboard.fromJson(Map<String, dynamic> j) {
    final p = j['prevision'] as Map<String, dynamic>? ?? const {};
    return Dashboard(
      mois: j['mois'] as String,
      totalRevenus: toDouble(j['totalRevenus']),
      totalDepenses: toDouble(j['totalDepenses']),
      budgetGlobal: toDoubleOrNull(j['budgetGlobal']),
      resteADepenser: toDoubleOrNull(j['resteADepenser']),
      prevision: Prevision(toDouble(p['depenseEstimeeFinMois']), p['depassementProbable'] as bool? ?? false),
      repartition: [
        for (final r in (j['repartition'] as List? ?? const []))
          MontantCategorie(r['categorieId'] as int, r['categorieNom'] as String, toDouble(r['montant'])),
      ],
      evolution: [
        for (final e in (j['evolution'] as List? ?? const []))
          MoisResume(e['mois'] as String, toDouble(e['depenses']), toDouble(e['revenus'])),
      ],
      economiesRealisees: toDouble(j['economiesRealisees']),
      alertesNonLues: j['alertesNonLues'] as int? ?? 0,
    );
  }
}

class Alerte {
  const Alerte({
    required this.id,
    required this.type,
    required this.message,
    required this.dateAlerte,
    this.lue = false,
  });

  final int id;
  final String type; // BUDGET_80, BUDGET_100, BAISSE_PRIX
  final String message;
  final DateTime dateAlerte;
  final bool lue;

  factory Alerte.fromJson(Map<String, dynamic> j) => Alerte(
        id: j['id'] as int,
        type: j['type'] as String,
        message: j['message'] as String,
        dateAlerte: DateTime.parse(j['dateAlerte'] as String),
        lue: j['lue'] as bool? ?? false,
      );

  Alerte marquerLue() => Alerte(id: id, type: type, message: message, dateAlerte: dateAlerte, lue: true);
}
