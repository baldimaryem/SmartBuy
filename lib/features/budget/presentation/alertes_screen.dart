import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/communs.dart';
import '../logic/budget_controller.dart';

/// BUD-11 : liste des alertes (budget à 80 %, 100 %, baisse de prix).
class AlertesScreen extends StatelessWidget {
  const AlertesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final c = context.watch<BudgetController>();
    final format = DateFormat('dd/MM/yyyy HH:mm', 'fr_FR');
    return Scaffold(
      appBar: AppBar(title: const Text('Alertes')),
      body: c.alertes.isEmpty
          ? const EtatVide(icone: Icons.notifications_none, message: 'Aucune alerte. Vos budgets sont sous contrôle !')
          : ListView.separated(
              itemCount: c.alertes.length,
              separatorBuilder: (_, _) => const Divider(height: 1),
              itemBuilder: (context, i) {
                final a = c.alertes[i];
                final (icone, couleur) = switch (a.type) {
                  'BUDGET_100' => (Icons.error_outline, AppTheme.danger),
                  'BUDGET_80' => (Icons.warning_amber_outlined, AppTheme.attention),
                  _ => (Icons.trending_down, AppTheme.succes),
                };
                return ListTile(
                  leading: Icon(icone, color: couleur),
                  title: Text(a.message, style: TextStyle(fontWeight: a.lue ? FontWeight.normal : FontWeight.bold)),
                  subtitle: Text(format.format(a.dateAlerte)),
                  trailing: a.lue ? null : const Icon(Icons.circle, size: 10),
                  onTap: () => c.marquerLue(a),
                );
              },
            ),
    );
  }
}
