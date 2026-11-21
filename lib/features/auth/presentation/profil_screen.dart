import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../shared/widgets/communs.dart';
import '../logic/auth_controller.dart';

/// AUTH-03 / AUTH-04 : profil et déconnexion.
class ProfilScreen extends StatefulWidget {
  const ProfilScreen({super.key});
  @override
  State<ProfilScreen> createState() => _ProfilScreenState();
}

class _ProfilScreenState extends State<ProfilScreen> {
  final _form = GlobalKey<FormState>();
  late final TextEditingController _nom;
  late final TextEditingController _revenu;
  late String _devise;

  @override
  void initState() {
    super.initState();
    final u = context.read<AuthController>().utilisateur!;
    _nom = TextEditingController(text: u.nom);
    _revenu = TextEditingController(text: u.revenuMensuel?.toStringAsFixed(3) ?? '');
    _devise = u.devise;
  }

  Future<void> _enregistrer() async {
    if (!_form.currentState!.validate()) return;
    try {
      await context.read<AuthController>().modifierProfil(
            nom: _nom.text.trim(),
            devise: _devise,
            revenuMensuel: lireMontant(_revenu.text),
          );
      if (!mounted) return;
      // Message bien visible, puis retour à l'écran précédent.
      notifier(context, '✓ Profil enregistré');
      context.pop();
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthController>();
    final u = auth.utilisateur;
    if (u == null) return const Scaffold();
    return Scaffold(
      appBar: AppBar(title: const Text('Mon profil')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        ListTile(
          leading: CircleAvatar(child: Text(u.nom.isEmpty ? '?' : u.nom[0].toUpperCase())),
          title: Text(u.email),
          subtitle: Text(u.estAdmin ? 'Administrateur' : 'Utilisateur'),
        ),
        const SizedBox(height: 8),
        Form(
          key: _form,
          child: Column(children: [
            TextFormField(
              controller: _nom,
              decoration: const InputDecoration(labelText: 'Nom'),
              validator: (v) => (v == null || v.trim().length < 2) ? 'Nom trop court' : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _revenu,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(
                labelText: 'Revenu mensuel habituel (DT)',
                helperText: 'Sert à vous proposer un budget (objectif : épargner 20 %)',
              ),
              validator: (v) => (v == null || v.isEmpty) ? null : validerMontant(v),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              isExpanded: true,
              initialValue: _devise,
              decoration: const InputDecoration(labelText: 'Devise'),
              items: const [
                DropdownMenuItem(value: 'TND', child: Text('Dinar tunisien (TND)')),
                DropdownMenuItem(value: 'EUR', child: Text('Euro (EUR)')),
              ],
              onChanged: (v) => setState(() => _devise = v ?? 'TND'),
            ),
            const SizedBox(height: 16),
            FilledButton(onPressed: auth.chargement ? null : _enregistrer, child: const Text('Enregistrer')),
          ]),
        ),
        const Divider(height: 32),
        ListTile(
          leading: const Icon(Icons.category_outlined),
          title: const Text('Mes catégories'),
          trailing: const Icon(Icons.chevron_right),
          onTap: () => context.push('/categories'),
        ),
        if (u.estAdmin)
          ListTile(
            leading: const Icon(Icons.admin_panel_settings_outlined),
            title: const Text('Espace administrateur'),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => context.push('/admin'),
          ),
        const SizedBox(height: 8),
        OutlinedButton.icon(
          onPressed: auth.deconnecter,
          icon: const Icon(Icons.logout),
          label: const Text('Se déconnecter'),
        ),
      ]),
    );
  }

  @override
  void dispose() {
    _nom.dispose();
    _revenu.dispose();
    super.dispose();
  }
}
