import 'package:flutter/material.dart';
import 'package:flutter_app/app.dart';
import 'package:flutter_app/core/di/dependances.dart';
import 'package:flutter_app/core/network/api_exception.dart';
import 'package:flutter_app/features/budget/data/budget_models.dart';
import 'package:flutter_app/features/prix/data/prix_models.dart';
import 'package:flutter_app/shared/mock/mock_store.dart';
import 'package:flutter_app/shared/utils/formatters.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';

/// Tests de scénarios sur la couche données (mode maquette) et un test d'interface.
void main() {
  setUpAll(() => initializeDateFormatting('fr_FR'));

  group('Scénario complet (repositories maquette)', () {
    late Dependances d;
    setUp(() async {
      d = Dependances.maquette(store: MockStore(delai: Duration.zero));
      await d.auth.register('Maryem', 'maryem@gmail.com', 'secret123');
      await d.auth.verifierEmail('maryem@gmail.com', MockStore.codeMaquette);
    });

    test('connexion refusée avec un mauvais mot de passe (AUTH-02)', () async {
      expect(() => d.auth.login('maryem@gmail.com', 'faux'), throwsA(isA<Exception>()));
    });

    test("inscription : connexion impossible avant la confirmation de l'e-mail (AUTH-01)", () async {
      await d.auth.register('Ali', 'ali@gmail.com', 'secret123');
      expect(
        () => d.auth.login('ali@gmail.com', 'secret123'),
        throwsA(isA<ApiException>().having((e) => e.status, 'status', 403)),
      );
      expect(() => d.auth.verifierEmail('ali@gmail.com', '000000'), throwsA(isA<ApiException>()));
      await d.auth.verifierEmail('ali@gmail.com', MockStore.codeMaquette);
      expect((await d.auth.login('ali@gmail.com', 'secret123')).utilisateur.nom, 'Ali');
    });

    test('mot de passe oublié : mauvais code refusé, bon code → nouvelle connexion (AUTH-05)', () async {
      await d.auth.demanderCodeReinitialisation('maryem@gmail.com');
      expect(
        () => d.auth.reinitialiserMotDePasse(email: 'maryem@gmail.com', code: '000000', nouveauMotDePasse: 'nouveau123'),
        throwsA(isA<Exception>()),
      );
      await d.auth.reinitialiserMotDePasse(email: 'maryem@gmail.com', code: '123456', nouveauMotDePasse: 'nouveau123');
      final s = await d.auth.login('maryem@gmail.com', 'nouveau123');
      expect(s.utilisateur.email, 'maryem@gmail.com');
    });

    test('une dépense fait baisser le reste à dépenser (BUD-04, BUD-10)', () async {
      final mois = Fmt.isoMois(DateTime.now());
      final avant = await d.budget.dashboard(mois);
      await d.budget.enregistrerOperation(Operation(
        id: 0, type: TypeOperation.depense, montant: 50, dateOperation: DateTime.now(), categorieId: 4,
      ));
      final apres = await d.budget.dashboard(mois);
      expect(apres.totalDepenses, closeTo(avant.totalDepenses + 50, 0.001));
      expect(apres.resteADepenser, closeTo(avant.resteADepenser! - 50, 0.001));
    });

    test('dépasser un budget de catégorie crée une alerte 100 % (BUD-11)', () async {
      final nbAvant = (await d.budget.alertes()).length;
      await d.budget.enregistrerOperation(Operation(
        id: 0, type: TypeOperation.depense, montant: 500, dateOperation: DateTime.now(), categorieId: 2, // Transport, plafond 120
      ));
      final alertes = await d.budget.alertes();
      expect(alertes.length, greaterThan(nbAvant));
      expect(alertes.any((a) => a.type == 'BUDGET_100' && a.message.contains('Transport')), isTrue);
    });

    test('liste → comparaison → achat → dépense avec économie (LNK-02, LNK-03)', () async {
      final listes = await d.prix.listes();
      final c = await d.prix.comparer(listes.first.id);
      expect(c.magasinRecommandeId, isNotNull);
      final op = await d.prix.acheter(listes.first.id, c.magasinRecommandeId!);
      expect(op.categorieNom, 'Alimentation');
      expect(op.montant, closeTo(c.recommande!.total, 0.001));
      expect(op.economie, closeTo(c.economie, 0.001));
      expect((await d.prix.listes()).first.achetee, isTrue);
    });

    test('un prix proposé par un utilisateur attend la validation (PRX-07)', () async {
      final p = await d.prix.proposerPrix(produitId: 1, magasinId: 1, montant: 0.9, date: DateTime.now());
      expect(p.statut, 'EN_ATTENTE');
      // L'utilisateur simple n'a pas accès à la validation (PRX-08)
      expect(() => d.prix.prixEnAttente(), throwsA(isA<Exception>()));
    });

    test('les prix d\'un produit sont triés du moins cher au plus cher (PRX-04)', () async {
      final prix = await d.prix.prixParMagasin(1);
      expect(prix.first.meilleur, isTrue);
      for (var i = 1; i < prix.length; i++) {
        expect(prix[i].montant, greaterThanOrEqualTo(prix[i - 1].montant));
      }
    });

    test('recherche par code-barres (PRX-03)', () async {
      final r = await d.prix.produits(codeBarres: '6194043001019');
      expect(r.single, isA<Produit>());
      expect(r.single.nom, contains('Lait'));
    });

    // ------------------------------------------------------------ évolution v3 : univers, codes-barres
    test('code-barres connu : trouvé dans le catalogue local, pas « ajouté »', () async {
      final r = await d.prix.produitParCodeBarres('6194043001019');
      expect(r.produit.nom, contains('Lait'));
      expect(r.origine, 'CATALOGUE');
      expect(r.ajouteAuCatalogue, isFalse);
    });

    test("code-barres inconnu : ApiException 404 en maquette (pas d'Open Food Facts hors ligne)", () async {
      expect(
        () => d.prix.produitParCodeBarres('0000000000000'),
        throwsA(isA<ApiException>().having((e) => e.status, 'status', 404)),
      );
    });

    test('contribution : le produit ajouté est retrouvé par son code-barres, source UTILISATEUR', () async {
      final rayon = (await d.prix.categoriesProduits(univers: Univers.vetements)).first;
      final p = await d.prix.contribuerProduit(
        nom: 'Pull laine', marque: 'Test', codeBarres: '1234567890128', categorieProduitId: rayon.id,
      );
      expect(p.source, 'UTILISATEUR');
      expect(p.univers, Univers.vetements);
      expect((await d.prix.produitParCodeBarres('1234567890128')).produit.id, p.id);
    });

    test('les rayons et produits sont filtrés par univers', () async {
      final rayons = await d.prix.categoriesProduits(univers: Univers.highTech);
      expect(rayons, isNotEmpty);
      expect(rayons.every((r) => r.univers == Univers.highTech), isTrue);
      final produits = await d.prix.produits(univers: Univers.vetements);
      expect(produits, isNotEmpty);
      expect(produits.every((p) => p.univers == Univers.vetements), isTrue);
    });

    test('règle du même univers : un produit High-tech refusé dans une liste Alimentation (400)', () async {
      final liste = await d.prix.enregistrerListe(nom: 'Courses', univers: Univers.alimentation);
      final smartphone = (await d.prix.produits(univers: Univers.highTech)).first;
      expect(
        () => d.prix.ajouterLigne(liste.id, produitId: smartphone.id, quantite: 1),
        throwsA(isA<ApiException>().having((e) => e.status, 'status', 400)),
      );
      expect((await d.prix.liste(liste.id)).lignes, isEmpty);
    });

    test('acheter une liste Vêtements crée une dépense « Vêtements »', () async {
      final liste = await d.prix.enregistrerListe(nom: 'Rentrée', univers: Univers.vetements);
      expect(liste.univers, Univers.vetements);
      for (final p in (await d.prix.produits(univers: Univers.vetements)).take(2)) {
        await d.prix.ajouterLigne(liste.id, produitId: p.id, quantite: 1);
      }
      final c = await d.prix.comparer(liste.id);
      expect(c.magasinRecommandeId, isNotNull);
      final op = await d.prix.acheter(liste.id, c.magasinRecommandeId!);
      expect(op.categorieNom, 'Vêtements');
      expect(op.type, TypeOperation.depense);
      expect(op.montant, closeTo(c.recommande!.total, 0.001));
    });

    test('les conseils par règles détectent la hausse des loisirs (BUD-16)', () async {
      final r = await d.ia.conseils(Fmt.isoMois(DateTime.now()));
      expect(r.source, 'REGLES');
      expect(r.conseils, isNotEmpty);
    });
  });

  testWidgets('inscription → code reçu par e-mail → accueil (navigation)', (tester) async {
    _ecranTelephone(tester);
    final d = Dependances.maquette(store: MockStore(delai: Duration.zero));
    await tester.pumpWidget(SmartBuyApp(dependances: d));
    await tester.pumpAndSettle();
    expect(find.text('Se connecter'), findsOneWidget);
    expect(find.textContaining('smartbuy.tn'), findsNothing); // plus de comptes fictifs

    await tester.tap(find.text('Créer un compte'));
    await tester.pumpAndSettle();
    final champs = find.byType(TextFormField);
    await tester.enterText(champs.at(0), 'Maryem');
    await tester.enterText(champs.at(1), 'maryem@gmail.com');
    await tester.enterText(champs.at(2), 'secret123');
    await tester.enterText(champs.at(3), 'secret123');
    await tester.tap(find.text('Créer mon compte'));
    await tester.pumpAndSettle();

    expect(find.text('maryem@gmail.com'), findsOneWidget); // écran « Confirmer mon e-mail »
    await tester.enterText(find.byType(TextFormField), MockStore.codeMaquette);
    await tester.tap(find.text('Confirmer'));
    await tester.pumpAndSettle();

    expect(find.textContaining('Bonjour'), findsOneWidget);
    expect(find.text('Solde du mois'), findsOneWidget);
    expect(find.byType(NavigationBar), findsOneWidget);
  });

  testWidgets('onglet Prix : univers puis code-barres inconnu → « Ajouter ce produit »', (tester) async {
    _ecranTelephone(tester);
    final store = MockStore(delai: Duration.zero);
    final d = Dependances.maquette(store: store);
    // Hors de l'horloge simulée du test : sinon les Future.delayed de la maquette n'avancent jamais.
    await tester.runAsync(() async {
      await d.auth.register('Maryem', 'maryem@gmail.com', 'secret123');
      await d.auth.verifierEmail('maryem@gmail.com', MockStore.codeMaquette);
    });
    await tester.pumpWidget(SmartBuyApp(dependances: d));
    await tester.pumpAndSettle();
    final champs = find.byType(TextFormField);
    await tester.enterText(champs.at(0), 'maryem@gmail.com');
    await tester.enterText(champs.at(1), 'secret123');
    await tester.tap(find.widgetWithText(FilledButton, 'Se connecter'));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Prix'));
    await tester.pumpAndSettle();
    expect(find.text('Code-barres (ex. 6194043001019)'), findsOneWidget);

    // Choisir un univers n'affiche que ses produits.
    await tester.tap(find.text('High-tech'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Galaxy A15'), findsOneWidget);
    expect(find.textContaining('Lait'), findsNothing);

    // Code inconnu → proposition d'ajout, formulaire pré-rempli.
    await tester.enterText(find.byType(TextField).first, '1234567890128');
    await tester.tap(find.byTooltip('Chercher ce code'));
    await tester.pumpAndSettle();
    expect(find.text('Code inconnu'), findsOneWidget);
    await tester.tap(find.text('Ajouter ce produit'));
    await tester.pumpAndSettle();
    expect(find.text('Ajouter un produit'), findsOneWidget);
    expect(find.text('1234567890128'), findsWidgets);
  });

  testWidgets('lien « Mot de passe oublié ? » → écran en 2 étapes', (tester) async {
    _ecranTelephone(tester);
    final d = Dependances.maquette(store: MockStore(delai: Duration.zero));
    await tester.pumpWidget(SmartBuyApp(dependances: d));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Mot de passe oublié ?'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField).first, 'maryem@gmail.com');
    await tester.tap(find.text('Recevoir un code par e-mail'));
    await tester.pumpAndSettle();
    expect(find.text('Code à 6 chiffres'), findsOneWidget);
  });
}

/// Simule un écran de smartphone (412 × 915) au lieu de la fenêtre de test par défaut (800 × 600).
void _ecranTelephone(WidgetTester tester) {
  tester.view.physicalSize = const Size(412 * 3, 915 * 3);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);
}
