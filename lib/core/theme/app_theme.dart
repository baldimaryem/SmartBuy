import 'package:flutter/material.dart';

/// Thème Material 3 de SmartBuy, construit à partir d'une seule couleur de base.
///
/// Les jetons de design (couleurs, rayons, espacements) sont décrits dans
/// `docs/design/design-tokens.json` : ce fichier en est la traduction en Dart.
class AppTheme {
  AppTheme._();

  static const Color primaire = Color(0xFF0F766E); // vert canard : argent, confiance
  static const Color succes = Color(0xFF16A34A);
  static const Color attention = Color(0xFFF59E0B);
  static const Color danger = Color(0xFFDC2626);
  static const Color revenu = Color(0xFF16A34A);
  static const Color depense = Color(0xFFDC2626);

  /// Palette des graphiques (une couleur par catégorie, dans l'ordre).
  static const List<Color> graphique = [
    Color(0xFF0F766E), Color(0xFFF59E0B), Color(0xFF6366F1), Color(0xFFEC4899),
    Color(0xFF0EA5E9), Color(0xFF84CC16), Color(0xFFF97316), Color(0xFF64748B),
  ];

  static const double rayon = 16;

  static ThemeData clair() => _theme(Brightness.light);
  static ThemeData sombre() => _theme(Brightness.dark);

  static ThemeData _theme(Brightness luminosite) {
    final schema = ColorScheme.fromSeed(seedColor: primaire, brightness: luminosite);
    return ThemeData(
      useMaterial3: true,
      colorScheme: schema,
      scaffoldBackgroundColor: schema.surface,
      appBarTheme: AppBarTheme(
        centerTitle: false,
        backgroundColor: schema.surface,
        foregroundColor: schema.onSurface,
        elevation: 0,
        scrolledUnderElevation: 1,
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        color: schema.surfaceContainerLow,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(rayon)),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: schema.surfaceContainerHighest.withValues(alpha: 0.5),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide.none,
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(52),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
      snackBarTheme: const SnackBarThemeData(behavior: SnackBarBehavior.floating),
    );
  }
}

/// Icônes des catégories : le serveur envoie un nom (« shopping_cart »),
/// l'application le traduit en icône Material.
IconData iconeCategorie(String? nom) => switch (nom) {
      'shopping_cart' => Icons.shopping_cart_outlined,
      'directions_car' => Icons.directions_car_outlined,
      'home' => Icons.home_outlined,
      'sports_esports' => Icons.sports_esports_outlined,
      'local_hospital' => Icons.local_hospital_outlined,
      'receipt' => Icons.receipt_long_outlined,
      'work' => Icons.work_outline,
      'school' => Icons.school_outlined,
      'attach_money' => Icons.attach_money,
      'laptop' => Icons.laptop_outlined,
      'card_giftcard' => Icons.card_giftcard_outlined,
      'volunteer_activism' => Icons.volunteer_activism_outlined,
      'replay' => Icons.replay,
      'restaurant' => Icons.restaurant_outlined,
      'checkroom' => Icons.checkroom_outlined,
      'pets' => Icons.pets_outlined,
      _ => Icons.category_outlined,
    };

const List<String> iconesDisponibles = [
  'shopping_cart', 'directions_car', 'home', 'sports_esports', 'local_hospital',
  'receipt', 'restaurant', 'checkroom', 'pets', 'work', 'school', 'attach_money', 'laptop', 'card_giftcard',
  'volunteer_activism', 'replay', 'category',
];
