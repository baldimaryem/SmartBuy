import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../../../shared/utils/formatters.dart';
import '../../../shared/widgets/communs.dart';
import '../data/ia_repository.dart';

/// Validation 3 : conseils intelligents (BUD-16), assistant en langage naturel et bilan par e-mail.
class AssistantScreen extends StatefulWidget {
  const AssistantScreen({super.key});
  @override
  State<AssistantScreen> createState() => _AssistantScreenState();
}

class _AssistantScreenState extends State<AssistantScreen> {
  final _question = TextEditingController();
  final _messages = <(bool, String)>[]; // (vient de l'utilisateur ?, texte)
  late Future<ReponseConseils> _conseils;
  bool _reflexion = false;
  final _mois = Fmt.isoMois(DateTime.now());

  IaRepository get _repo => context.read<IaRepository>();

  @override
  void initState() {
    super.initState();
    _conseils = context.read<IaRepository>().conseils(_mois);
  }

  Future<void> _envoyer([String? texte]) async {
    final q = (texte ?? _question.text).trim();
    if (q.isEmpty) return;
    _question.clear();
    setState(() {
      _messages.add((true, q));
      _reflexion = true;
    });
    try {
      final r = await _repo.poserQuestion(q);
      setState(() => _messages.add((false, r.reponse)));
    } catch (e) {
      setState(() => _messages.add((false, 'Erreur : $e')));
    } finally {
      if (mounted) setState(() => _reflexion = false);
    }
  }

  Future<void> _rapport() async {
    try {
      final dest = await _repo.envoyerRapportMensuel(_mois);
      if (mounted) notifier(context, 'Bilan de ${Fmt.moisLong(_mois)} envoyé à $dest');
    } catch (e) {
      if (mounted) notifier(context, e, erreur: true);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
          title: const Text('Assistant'),
          actions: [IconButton(tooltip: 'Recevoir le bilan par e-mail', onPressed: _rapport, icon: const Icon(Icons.forward_to_inbox))],
        ),
        body: Column(children: [
          Expanded(
            child: ListView(padding: const EdgeInsets.all(16), children: [
              FutureBuilder(
                future: _conseils,
                builder: (context, snap) {
                  if (snap.hasError) return MessageErreur('${snap.error}');
                  if (!snap.hasData) return const Chargement();
                  final r = snap.data!;
                  return Section(
                    titre: 'Conseils du mois',
                    action: Chip(
                      avatar: Icon(r.parIa ? Icons.auto_awesome : Icons.rule, size: 16),
                      label: Text(r.parIa ? 'IA' : 'Règles'),
                    ),
                    child: Column(children: [
                      if (r.conseils.isEmpty) const Text('Rien à signaler, bravo !'),
                      for (final c in r.conseils)
                        ListTile(
                          contentPadding: EdgeInsets.zero,
                          leading: Icon(
                            switch (c.niveau) {
                              NiveauConseil.alerte => Icons.error_outline,
                              NiveauConseil.attention => Icons.warning_amber_outlined,
                              NiveauConseil.info => Icons.lightbulb_outline,
                            },
                            color: switch (c.niveau) {
                              NiveauConseil.alerte => AppTheme.danger,
                              NiveauConseil.attention => AppTheme.attention,
                              NiveauConseil.info => AppTheme.succes,
                            },
                          ),
                          title: Text(c.titre),
                          subtitle: Text(c.message),
                        ),
                    ]),
                  );
                },
              ),
              const SizedBox(height: 16),
              Wrap(spacing: 8, runSpacing: 8, children: [
                for (final s in const ['Combien me reste-t-il ?', 'Prévision de fin de mois', 'Où est-ce que je dépense le plus ?', 'Un conseil pour économiser'])
                  ActionChip(label: Text(s), onPressed: () => _envoyer(s)),
              ]),
              const SizedBox(height: 16),
              for (final (deMoi, texte) in _messages)
                Align(
                  alignment: deMoi ? Alignment.centerRight : Alignment.centerLeft,
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.all(12),
                    constraints: const BoxConstraints(maxWidth: 320),
                    decoration: BoxDecoration(
                      color: deMoi ? Theme.of(context).colorScheme.primaryContainer : Theme.of(context).colorScheme.surfaceContainerHigh,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Text(texte),
                  ),
                ),
              if (_reflexion) const Align(alignment: Alignment.centerLeft, child: Padding(padding: EdgeInsets.all(8), child: Text('…'))),
            ]),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 8, 8),
              child: Row(children: [
                Expanded(
                  child: TextField(
                    controller: _question,
                    decoration: const InputDecoration(hintText: 'Posez une question sur votre budget'),
                    onSubmitted: (_) => _envoyer(),
                  ),
                ),
                IconButton(tooltip: 'Envoyer', onPressed: _reflexion ? null : _envoyer, icon: const Icon(Icons.send)),
              ]),
            ),
          ),
        ]),
      );

  @override
  void dispose() {
    _question.dispose();
    super.dispose();
  }
}
