import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../shared/widgets/communs.dart';
import '../logic/catalogue_controller.dart';
import 'univers_widgets.dart';

/// PRX-01 : liste des magasins.
class MagasinsScreen extends StatelessWidget {
  const MagasinsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = context.watch<CatalogueController>();
    return Scaffold(
      appBar: AppBar(title: const Text('Magasins')),
      body: c.magasins.isEmpty
          ? (c.chargement ? const Chargement() : const EtatVide(icone: Icons.store_outlined, message: 'Aucun magasin.'))
          : ListView(children: [
              for (final m in c.magasins)
                ListTile(
                  leading: CircleAvatar(child: Text(m.enseigne.isEmpty ? '?' : m.enseigne[0])),
                  title: Text(m.nom),
                  // Un magasin en ligne n'a pas d'adresse : on affiche son site.
                  subtitle: Text(m.enLigne ? (m.siteWeb ?? 'Site marchand') : '${m.adresse}, ${m.ville}'),
                  trailing: m.enLigne ? const Etiquette('en ligne', icone: Icons.language) : null,
                ),
            ]),
    );
  }
}
