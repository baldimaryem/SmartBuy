import 'package:flutter/material.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'app.dart';
import 'core/di/dependances.dart';

/// Point d'entrée.
///
///   flutter run --dart-define=USE_MOCK=true   → données simulées (aucun serveur requis)
///   flutter run                               → API Spring Boot + Oracle (voir backend/README.md)
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('fr_FR'); // noms des mois en français
  runApp(SmartBuyApp(dependances: Dependances.creer()));
}
