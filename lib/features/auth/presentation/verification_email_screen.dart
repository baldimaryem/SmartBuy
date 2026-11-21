import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../shared/widgets/communs.dart';
import '../logic/auth_controller.dart';

/// Inscription, étape 2 : saisir le code à 6 chiffres reçu par e-mail.
///
/// Pourquoi ? C'est la seule façon de prouver qu'une adresse e-mail existe vraiment
/// et appartient à la personne : seul le propriétaire de la boîte peut lire le code.
class VerificationEmailScreen extends StatefulWidget {
  const VerificationEmailScreen({super.key, required this.email});
  final String email;

  @override
  State<VerificationEmailScreen> createState() => _VerificationEmailScreenState();
}

class _VerificationEmailScreenState extends State<VerificationEmailScreen> {
  final _form = GlobalKey<FormState>();
  final _code = TextEditingController();
  bool _renvoi = false;

  Future<void> _confirmer() async {
    if (!_form.currentState!.validate()) return;
    try {
      await context.read<AuthController>().verifierEmail(widget.email, _code.text);
      // Connecté : le routeur ouvre l'accueil automatiquement.
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  Future<void> _renvoyer() async {
    setState(() => _renvoi = true);
    try {
      final message = await context.read<AuthController>().renvoyerCode(widget.email);
      if (mounted) notifier(context, message);
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    } finally {
      if (mounted) setState(() => _renvoi = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthController>();
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Confirmer mon e-mail'),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => context.go('/connexion')),
      ),
      body: Form(
        key: _form,
        child: ListView(padding: const EdgeInsets.all(24), children: [
          Icon(Icons.mark_email_unread_outlined, size: 64, color: theme.colorScheme.primary),
          const SizedBox(height: 16),
          Text('Un code à 6 chiffres a été envoyé à', textAlign: TextAlign.center, style: theme.textTheme.bodyLarge),
          Text(widget.email, textAlign: TextAlign.center, style: theme.textTheme.titleMedium),
          const SizedBox(height: 8),
          Text('Pensez à regarder dans les spams. Le code est valable 15 minutes.',
              textAlign: TextAlign.center, style: theme.textTheme.bodySmall),
          const SizedBox(height: 24),
          TextFormField(
            controller: _code,
            autofocus: true,
            keyboardType: TextInputType.number,
            maxLength: 6,
            textAlign: TextAlign.center,
            style: theme.textTheme.headlineMedium?.copyWith(letterSpacing: 10),
            decoration: const InputDecoration(hintText: '······', counterText: ''),
            validator: (v) => RegExp(r'^\d{6}$').hasMatch(v?.trim() ?? '') ? null : 'Saisissez les 6 chiffres',
            onFieldSubmitted: (_) => _confirmer(),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: auth.chargement ? null : _confirmer,
            child: auth.chargement
                ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text('Confirmer'),
          ),
          TextButton(
            onPressed: _renvoi ? null : _renvoyer,
            child: const Text('Je n\'ai rien reçu : renvoyer un code'),
          ),
        ]),
      ),
    );
  }

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }
}
