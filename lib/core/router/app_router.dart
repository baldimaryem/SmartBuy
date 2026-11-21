import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/admin/presentation/admin_screen.dart';
import '../../features/auth/logic/auth_controller.dart';
import '../../features/auth/presentation/connexion_screen.dart';
import '../../features/auth/presentation/inscription_screen.dart';
import '../../features/auth/presentation/mot_de_passe_oublie_screen.dart';
import '../../features/auth/presentation/profil_screen.dart';
import '../../features/auth/presentation/verification_email_screen.dart';
import '../../features/budget/data/budget_models.dart';
import '../../features/budget/presentation/accueil_screen.dart';
import '../../features/budget/presentation/alertes_screen.dart';
import '../../features/budget/presentation/budgets_screen.dart';
import '../../features/budget/presentation/categories_screen.dart';
import '../../features/budget/presentation/operation_form_screen.dart';
import '../../features/budget/presentation/operations_screen.dart';
import '../../features/budget/presentation/statistiques_screen.dart';
import '../../features/ia/presentation/assistant_screen.dart';
import '../../features/prix/presentation/catalogue_screen.dart';
import '../../features/prix/presentation/comparaison_screen.dart';
import '../../features/prix/presentation/liste_detail_screen.dart';
import '../../features/prix/presentation/listes_screen.dart';
import '../../features/prix/presentation/magasins_screen.dart';
import '../../features/prix/presentation/produit_screen.dart';
import '../../shared/widgets/coquille.dart';

/// Toutes les routes de l'application, en un seul endroit.
///
/// - `redirect` : garde d'authentification. Non connecté → `/connexion` ;
///   connecté sur une page de connexion → `/accueil` ; non-admin sur `/admin` → `/accueil`.
/// - `refreshListenable` : le routeur réévalue la redirection à chaque
///   `notifyListeners()` de l'[AuthController] (connexion, déconnexion).
/// - `StatefulShellRoute` : la barre de navigation du bas ; chaque onglet garde
///   sa propre pile d'écrans (on retrouve la page quittée en revenant sur l'onglet).
GoRouter creerRouteur(AuthController auth) => GoRouter(
      initialLocation: '/accueil',
      refreshListenable: auth,
      redirect: (context, state) {
        const pagesPubliques = {'/connexion', '/inscription', '/mot-de-passe-oublie', '/verification-email'};
        final versAuth = pagesPubliques.contains(state.matchedLocation);
        if (!auth.connecte) return versAuth ? null : '/connexion';
        if (versAuth) return '/accueil';
        if (state.matchedLocation.startsWith('/admin') && !auth.estAdmin) return '/accueil';
        return null;
      },
      routes: [
        GoRoute(path: '/connexion', builder: (_, _) => const ConnexionScreen()),
        GoRoute(path: '/inscription', builder: (_, _) => const InscriptionScreen()),
        GoRoute(
          path: '/verification-email',
          builder: (_, state) => VerificationEmailScreen(email: state.extra as String? ?? ''),
        ),
        GoRoute(
          path: '/mot-de-passe-oublie',
          builder: (_, state) => MotDePasseOublieScreen(emailInitial: state.extra as String? ?? ''),
        ),
        GoRoute(path: '/profil', builder: (_, _) => const ProfilScreen()),
        GoRoute(path: '/categories', builder: (_, _) => const CategoriesScreen()),
        GoRoute(path: '/admin', builder: (_, _) => const AdminScreen()),
        StatefulShellRoute.indexedStack(
          builder: (context, state, shell) => Coquille(shell: shell),
          branches: [
            StatefulShellBranch(routes: [
              GoRoute(
                path: '/accueil',
                builder: (_, _) => const AccueilScreen(),
                routes: [
                  GoRoute(path: 'budgets', builder: (_, _) => const BudgetsScreen()),
                  GoRoute(path: 'statistiques', builder: (_, _) => const StatistiquesScreen()),
                  GoRoute(path: 'alertes', builder: (_, _) => const AlertesScreen()),
                ],
              ),
            ]),
            StatefulShellBranch(routes: [
              GoRoute(
                path: '/operations',
                builder: (_, _) => const OperationsScreen(),
                routes: [
                  GoRoute(
                    path: 'edition',
                    builder: (_, state) {
                      final extra = state.extra;
                      return OperationFormScreen(
                        operation: extra is Operation ? extra : null,
                        typeInitial: extra is TypeOperation ? extra : TypeOperation.depense,
                      );
                    },
                  ),
                ],
              ),
            ]),
            StatefulShellBranch(routes: [
              GoRoute(
                path: '/prix',
                builder: (_, _) => const CatalogueScreen(),
                routes: [
                  GoRoute(path: 'magasins', builder: (_, _) => const MagasinsScreen()),
                  GoRoute(
                    path: 'produit/:id',
                    builder: (_, state) => ProduitScreen(produitId: int.parse(state.pathParameters['id']!)),
                  ),
                ],
              ),
            ]),
            StatefulShellBranch(routes: [
              GoRoute(
                path: '/listes',
                builder: (_, _) => const ListesScreen(),
                routes: [
                  GoRoute(
                    path: ':id',
                    builder: (_, state) => ListeDetailScreen(listeId: int.parse(state.pathParameters['id']!)),
                    routes: [
                      GoRoute(
                        path: 'comparaison',
                        builder: (_, state) => ComparaisonScreen(listeId: int.parse(state.pathParameters['id']!)),
                      ),
                    ],
                  ),
                ],
              ),
            ]),
            StatefulShellBranch(routes: [
              GoRoute(path: '/assistant', builder: (_, _) => const AssistantScreen()),
            ]),
          ],
        ),
      ],
      errorBuilder: (context, state) => Scaffold(
        appBar: AppBar(),
        body: Center(child: Text('Page introuvable : ${state.uri}')),
      ),
    );
