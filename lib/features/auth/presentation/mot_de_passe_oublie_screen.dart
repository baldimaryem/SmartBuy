import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../shared/widgets/communs.dart';
import '../logic/auth_controller.dart';

/// AUTH-05 : mot de passe oublié, en 2 étapes.
///   1. l'utilisateur saisit son e-mail → le serveur envoie un code à 6 chiffres (valable 15 min) ;
///   2. il saisit le code et un nouveau mot de passe.
class MotDePasseOublieScreen extends StatefulWidget {
  const MotDePasseOublieScreen({super.key, this.emailInitial = ''});
  final String emailInitial;

  @override
  State<MotDePasseOublieScreen> createState() => _MotDePasseOublieScreenState();
}

class _MotDePasseOublieScreenState extends State<MotDePasseOublieScreen> {
  final _formEmail = GlobalKey<FormState>();
  final _formCode = GlobalKey<FormState>();
  late final _email = TextEditingController(text: widget.emailInitial);
  final _code = TextEditingController();
  final _mdp = TextEditingController();
  final _confirmation = TextEditingController();
  bool _codeEnvoye = false;
  bool _envoi = false;
  String? _info;

  Future<void> _demanderCode() async {
    if (!_formEmail.currentState!.validate()) return;
    setState(() => _envoi = true);
    try {
      final message = await context.read<AuthController>().demanderCodeReinitialisation(_email.text);
      setState(() {
        _codeEnvoye = true;
        _info = message;
      });
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  Future<void> _reinitialiser() async {
    if (!_formCode.currentState!.validate()) return;
    setState(() => _envoi = true);
    try {
      final message = await context.read<AuthController>().reinitialiserMotDePasse(
            email: _email.text,
            code: _code.text,
            nouveauMotDePasse: _mdp.text,
          );
      if (!mounted) return;
      notifier(context, message);
      context.go('/connexion');
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    } finally {
      if (mounted) setState(() => _envoi = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mot de passe oublié'),
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => context.go('/connexion')),
      ),
      body: ListView(padding: const EdgeInsets.all(24), children: [
        // Indicateur d'étape
        Row(children: [
          _Etape(numero: 1, libelle: 'E-mail', active: true),
          const SizedBox(width: 32, child: Divider(indent: 6, endIndent: 6)),
          Flexible(child: _Etape(numero: 2, libelle: 'Code et nouveau mot de passe', active: _codeEnvoye)),
        ]),
        const SizedBox(height: 24),
        Form(
          key: _formEmail,
          child: TextFormField(
            controller: _email,
            enabled: !_codeEnvoye,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(labelText: 'E-mail du compte', prefixIcon: Icon(Icons.mail_outline)),
            validator: (v) => (v == null || !v.contains('@')) ? 'E-mail invalide' : null,
          ),
        ),
        const SizedBox(height: 16),
        if (!_codeEnvoye)
          FilledButton(
            onPressed: _envoi ? null : _demanderCode,
            child: _envoi ? const _Roue() : const Text('Recevoir un code par e-mail'),
          )
        else ...[
          Card(
            color: theme.colorScheme.secondaryContainer,
            child: ListTile(leading: const Icon(Icons.mark_email_read_outlined), title: Text(_info ?? '')),
          ),
          const SizedBox(height: 16),
          Form(
            key: _formCode,
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              TextFormField(
                controller: _code,
                keyboardType: TextInputType.number,
                maxLength: 6,
                style: theme.textTheme.headlineSmall?.copyWith(letterSpacing: 8),
                textAlign: TextAlign.center,
                decoration: const InputDecoration(labelText: 'Code à 6 chiffres', counterText: ''),
                validator: (v) => RegExp(r'^\d{6}$').hasMatch(v?.trim() ?? '') ? null : '6 chiffres',
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _mdp,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Nouveau mot de passe',
                  helperText: '8 caractères minimum, dont un chiffre',
                  prefixIcon: Icon(Icons.lock_outline),
                ),
                validator: (v) {
                  if (v == null || v.length < 8) return '8 caractères minimum';
                  if (!RegExp(r'\d').hasMatch(v)) return 'Ajoutez au moins un chiffre';
                  return null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _confirmation,
                obscureText: true,
                decoration: const InputDecoration(labelText: 'Confirmer', prefixIcon: Icon(Icons.lock_outline)),
                validator: (v) => v != _mdp.text ? 'Les mots de passe ne correspondent pas' : null,
              ),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: _envoi ? null : _reinitialiser,
                child: _envoi ? const _Roue() : const Text('Changer mon mot de passe'),
              ),
              TextButton(onPressed: _envoi ? null : _demanderCode, child: const Text('Renvoyer un code')),
            ]),
          ),
        ],
      ]),
    );
  }

  @override
  void dispose() {
    for (final c in [_email, _code, _mdp, _confirmation]) {
      c.dispose();
    }
    super.dispose();
  }
}

class _Etape extends StatelessWidget {
  const _Etape({required this.numero, required this.libelle, required this.active});
  final int numero;
  final String libelle;
  final bool active;

  @override
  Widget build(BuildContext context) {
    final schema = Theme.of(context).colorScheme;
    return Row(mainAxisSize: MainAxisSize.min, children: [
      CircleAvatar(
        radius: 12,
        backgroundColor: active ? schema.primary : schema.surfaceContainerHighest,
        child: Text('$numero', style: TextStyle(fontSize: 12, color: active ? schema.onPrimary : schema.outline)),
      ),
      const SizedBox(width: 6),
      Flexible(
        child: Text(libelle, overflow: TextOverflow.ellipsis, style: TextStyle(color: active ? null : schema.outline)),
      ),
    ]);
  }
}

class _Roue extends StatelessWidget {
  const _Roue();
  @override
  Widget build(BuildContext context) =>
      const SizedBox(width: 22, height: 22, child: CircularProgressIndicator(strokeWidth: 2));
}
