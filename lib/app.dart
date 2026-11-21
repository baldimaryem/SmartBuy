import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import 'core/di/dependances.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/logic/auth_controller.dart';
import 'features/budget/logic/budget_controller.dart';
import 'features/prix/data/prix_repository.dart';
import 'features/ia/data/ia_repository.dart';
import 'features/prix/logic/catalogue_controller.dart';
import 'features/prix/logic/listes_controller.dart';
import 'shared/widgets/cadre_mobile.dart';

/// Racine de l'application.
///
/// `MultiProvider` rend disponibles, partout sous lui, les repositories et les
/// contrôleurs d'état. Un écran y accède avec `context.read<T>()` (action ponctuelle)
/// ou `context.watch<T>()` (se redessine quand l'état change).
class SmartBuyApp extends StatefulWidget {
  const SmartBuyApp({super.key, required this.dependances});
  final Dependances dependances;

  @override
  State<SmartBuyApp> createState() => _SmartBuyAppState();
}

class _SmartBuyAppState extends State<SmartBuyApp> {
  late final AuthController _auth;
  late final BudgetController _budget;
  late final CatalogueController _catalogue;
  late final ListesController _listes;
  late final GoRouter _routeur;

  @override
  void initState() {
    super.initState();
    final d = widget.dependances;
    _auth = AuthController(d.auth);
    _budget = BudgetController(d.budget);
    _catalogue = CatalogueController(d.prix);
    _listes = ListesController(d.prix);
    _routeur = creerRouteur(_auth);
    // Jeton expiré côté serveur (401) → retour automatique à l'écran de connexion.
    d.api?.onNonAutorise = _auth.deconnecter;
    _auth.addListener(() {
      if (!_auth.connecte) _budget.reinitialiser();
    });
  }

  @override
  Widget build(BuildContext context) => MultiProvider(
        providers: [
          Provider<PrixRepository>.value(value: widget.dependances.prix),
          Provider<IaRepository>.value(value: widget.dependances.ia),
          ChangeNotifierProvider.value(value: _auth),
          ChangeNotifierProvider.value(value: _budget),
          ChangeNotifierProvider.value(value: _catalogue),
          ChangeNotifierProvider.value(value: _listes),
        ],
        child: MaterialApp.router(
          title: 'SmartBuy',
          debugShowCheckedModeBanner: false,
          theme: AppTheme.clair(),
          darkTheme: AppTheme.sombre(),
          routerConfig: _routeur,
          locale: const Locale('fr'),
          supportedLocales: const [Locale('fr')],
          localizationsDelegates: GlobalMaterialLocalizations.delegates,
          // Sur PC (Chrome / Windows), l'application s'affiche dans un cadre de téléphone.
          builder: (context, child) => CadreMobile(child: child ?? const SizedBox()),
        ),
      );
}
