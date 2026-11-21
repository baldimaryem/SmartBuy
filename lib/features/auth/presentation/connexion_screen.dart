import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/config/app_config.dart';
import '../../../core/network/api_exception.dart';
import '../../../shared/widgets/communs.dart';
import '../../../shared/mock/mock_store.dart';
import '../logic/auth_controller.dart';

/// AUTH-02 / AUTH-06 : connexion (utilisateur ou administrateur).
class ConnexionScreen extends StatefulWidget {
  const ConnexionScreen({super.key});
  @override
  State<ConnexionScreen> createState() => _ConnexionScreenState();
}

class _ConnexionScreenState extends State<ConnexionScreen> {
  final _form = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _mdp = TextEditingController();
  bool _masque = true;

  Future<void> _valider() async {
    if (!_form.currentState!.validate()) return;
    try {
      await context.read<AuthController>().login(_email.text, _mdp.text);
      // Pas de navigation ici : le routeur redirige seul vers /accueil.
    } on ApiException catch (e) {
      if (!mounted) return;
      notifier(context, e, erreur: true);
      // 403 = compte créé mais e-mail jamais confirmé : on renvoie un code et on ouvre l'écran de saisie.
      if (e.status == 403) {
        final email = _email.text.trim();
        final auth = context.read<AuthController>();
        context.go('/verification-email', extra: email);
        await auth.renvoyerCode(email);
      }
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthController>();
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _form,
                child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                  Icon(Icons.savings_outlined, size: 64, color: theme.colorScheme.primary),
                  const SizedBox(height: 12),
                  Text('SmartBuy', textAlign: TextAlign.center, style: theme.textTheme.headlineMedium),
                  Text('Maîtrisez votre budget, achetez au meilleur prix',
                      textAlign: TextAlign.center, style: theme.textTheme.bodyMedium),
                  const SizedBox(height: 32),
                  TextFormField(
                    controller: _email,
                    keyboardType: TextInputType.emailAddress,
                    autofillHints: const [AutofillHints.email],
                    decoration: const InputDecoration(labelText: 'E-mail', prefixIcon: Icon(Icons.mail_outline)),
                    validator: (v) => (v == null || !v.contains('@')) ? 'E-mail invalide' : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _mdp,
                    obscureText: _masque,
                    decoration: InputDecoration(
                      labelText: 'Mot de passe',
                      prefixIcon: const Icon(Icons.lock_outline),
                      suffixIcon: IconButton(
                        tooltip: _masque ? 'Afficher' : 'Masquer',
                        icon: Icon(_masque ? Icons.visibility_outlined : Icons.visibility_off_outlined),
                        onPressed: () => setState(() => _masque = !_masque),
                      ),
                    ),
                    validator: (v) => (v == null || v.isEmpty) ? 'Mot de passe requis' : null,
                    onFieldSubmitted: (_) => _valider(),
                  ),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: () => context.go('/mot-de-passe-oublie', extra: _email.text.trim()),
                      child: const Text('Mot de passe oublié ?'),
                    ),
                  ),
                  const SizedBox(height: 8),
                  FilledButton(
                    onPressed: auth.chargement ? null : _valider,
                    child: auth.chargement
                        ? const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Text('Se connecter'),
                  ),
                  const SizedBox(height: 8),
                  TextButton(onPressed: () => context.go('/inscription'), child: const Text('Créer un compte')),
                  if (AppConfig.useMock) ...[
                    const SizedBox(height: 24),
                    Card(
                      child: ListTile(
                        leading: const Icon(Icons.science_outlined),
                        title: const Text('Mode maquette (données simulées)'),
                        subtitle: Text('Créez un compte : le code de confirmation est ${MockStore.codeMaquette}.'),
                      ),
                    ),
                  ],
                ]),
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _email.dispose();
    _mdp.dispose();
    super.dispose();
  }
}
