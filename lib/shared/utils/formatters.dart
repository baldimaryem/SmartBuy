import 'package:intl/intl.dart';

/// Fonctions de formatage partagées par tous les écrans.
class Fmt {
  Fmt._();

  static final _montant = NumberFormat.currency(locale: 'fr_FR', symbol: 'DT', decimalDigits: 3);
  static final _jour = DateFormat('dd/MM/yyyy', 'fr_FR');
  static final _moisLong = DateFormat('MMMM yyyy', 'fr_FR');
  static final _moisCourt = DateFormat('MMM', 'fr_FR');

  /// 1234.5 → « 1 234,500 DT » (le dinar a 3 décimales : les millimes).
  static String montant(num? valeur) => valeur == null ? '—' : _montant.format(valeur);

  static String jour(DateTime d) => _jour.format(d);

  /// « 2026-11 » → « novembre 2026 »
  static String moisLong(String mois) => _majuscule(_moisLong.format(parseMois(mois)));

  /// « 2026-11 » → « nov. »
  static String moisCourt(String mois) => _moisCourt.format(parseMois(mois));

  /// Date → « 2026-11-05 » (format attendu par l'API)
  static String isoJour(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  /// Date → « 2026-11 »
  static String isoMois(DateTime d) => isoJour(d).substring(0, 7);

  static DateTime parseMois(String mois) => DateTime.parse('$mois-01');

  /// Mois précédent ou suivant : décaler(« 2026-01 », -1) → « 2025-12 »
  static String decalerMois(String mois, int delta) {
    final d = parseMois(mois);
    return isoMois(DateTime(d.year, d.month + delta));
  }

  static String _majuscule(String s) => s.isEmpty ? s : s[0].toUpperCase() + s.substring(1);
}

/// Conversion JSON → double, que le serveur envoie `12`, `12.5` ou `null`.
double toDouble(Object? v) => v == null ? 0 : (v as num).toDouble();
double? toDoubleOrNull(Object? v) => v == null ? null : (v as num).toDouble();
