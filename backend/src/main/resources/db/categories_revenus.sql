-- Catégories de revenus par défaut (exécuté à chaque démarrage : n'ajoute que celles qui manquent).
-- Un revenu, c'est tout l'argent qui ENTRE : salaire, freelance, carte resto, prime, aides...
INSERT INTO categorie (nom, type, icone)
SELECT 'Freelance', 'REVENU', 'laptop' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM categorie WHERE nom = 'Freelance' AND type = 'REVENU' AND utilisateur_id IS NULL);
INSERT INTO categorie (nom, type, icone)
SELECT 'Carte resto / tickets', 'REVENU', 'restaurant' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM categorie WHERE nom = 'Carte resto / tickets' AND type = 'REVENU' AND utilisateur_id IS NULL);
INSERT INTO categorie (nom, type, icone)
SELECT 'Prime / bonus', 'REVENU', 'card_giftcard' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM categorie WHERE nom = 'Prime / bonus' AND type = 'REVENU' AND utilisateur_id IS NULL);
INSERT INTO categorie (nom, type, icone)
SELECT 'Aides et allocations', 'REVENU', 'volunteer_activism' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM categorie WHERE nom = 'Aides et allocations' AND type = 'REVENU' AND utilisateur_id IS NULL);
INSERT INTO categorie (nom, type, icone)
SELECT 'Remboursement', 'REVENU', 'replay' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM categorie WHERE nom = 'Remboursement' AND type = 'REVENU' AND utilisateur_id IS NULL);
