-- =====================================================================
-- Évolution v3 : univers de courses, vrais codes-barres tunisiens, prix collectés en ligne.
-- Exécutée une seule fois par DatabaseInitializer (si CATEGORIE_PRODUIT.UNIVERS n'existe pas).
-- =====================================================================

-- 1. Univers : chaque rayon et chaque liste appartient à un univers de courses.
ALTER TABLE categorie_produit ADD (univers VARCHAR2(12) DEFAULT 'ALIMENTATION' NOT NULL);
ALTER TABLE categorie_produit ADD CONSTRAINT ck_categorie_produit_univers
    CHECK (univers IN ('ALIMENTATION', 'VETEMENTS', 'HIGH_TECH'));
ALTER TABLE liste_courses ADD (univers VARCHAR2(12) DEFAULT 'ALIMENTATION' NOT NULL);
ALTER TABLE liste_courses ADD CONSTRAINT ck_liste_univers
    CHECK (univers IN ('ALIMENTATION', 'VETEMENTS', 'HIGH_TECH'));

-- 2. Origine d'un produit (saisi, trouvé sur Open Food Facts, collecté sur un site, ajouté par un utilisateur).
ALTER TABLE produit ADD (source VARCHAR2(20) DEFAULT 'CATALOGUE' NOT NULL);
ALTER TABLE produit ADD CONSTRAINT ck_produit_source
    CHECK (source IN ('CATALOGUE', 'OPEN_FOOD_FACTS', 'COLLECTE', 'UTILISATEUR'));
ALTER TABLE produit ADD (url_source VARCHAR2(500));
CREATE INDEX ix_produit_url_source ON produit (url_source);

-- 3. Magasins en ligne (sites marchands dont les prix sont collectés).
ALTER TABLE magasin ADD (en_ligne CHAR(1) DEFAULT 'N' NOT NULL);
ALTER TABLE magasin ADD CONSTRAINT ck_magasin_en_ligne CHECK (en_ligne IN ('O', 'N'));
ALTER TABLE magasin ADD (site_web VARCHAR2(200));

-- 4. Un prix peut maintenant venir de la collecte automatique.
ALTER TABLE prix DROP CONSTRAINT ck_prix_source;
ALTER TABLE prix ADD CONSTRAINT ck_prix_source CHECK (source IN ('ADMIN', 'UTILISATEUR', 'COLLECTE'));

-- 5. Magasins en ligne réels.
INSERT INTO magasin (nom, enseigne, adresse, ville, en_ligne, site_web)
VALUES ('Carrefour Tunisie (en ligne)', 'Carrefour', 'Livraison à domicile', 'Tunisie', 'O', 'https://www.carrefour.tn');
INSERT INTO magasin (nom, enseigne, adresse, ville, en_ligne, site_web)
VALUES ('Spacenet (en ligne)', 'Spacenet', 'Livraison à domicile', 'Tunisie', 'O', 'https://spacenet.tn');

-- 6. Rayons des nouveaux univers.
INSERT INTO categorie_produit (nom, univers) VALUES ('Vêtements homme', 'VETEMENTS');
INSERT INTO categorie_produit (nom, univers) VALUES ('Vêtements femme', 'VETEMENTS');
INSERT INTO categorie_produit (nom, univers) VALUES ('Vêtements enfant', 'VETEMENTS');
INSERT INTO categorie_produit (nom, univers) VALUES ('Chaussures', 'VETEMENTS');
INSERT INTO categorie_produit (nom, univers) VALUES ('Smartphones', 'HIGH_TECH');
INSERT INTO categorie_produit (nom, univers) VALUES ('Ordinateurs portables', 'HIGH_TECH');
INSERT INTO categorie_produit (nom, univers) VALUES ('Tablettes', 'HIGH_TECH');
INSERT INTO categorie_produit (nom, univers) VALUES ('Accessoires high-tech', 'HIGH_TECH');

-- 7. Catégories de budget pour les nouveaux univers (dépenses).
INSERT INTO categorie (nom, type, icone)
SELECT 'Vêtements', 'DEPENSE', 'checkroom' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM categorie WHERE nom = 'Vêtements' AND type = 'DEPENSE' AND utilisateur_id IS NULL);
INSERT INTO categorie (nom, type, icone)
SELECT 'High-tech', 'DEPENSE', 'laptop' FROM dual
WHERE NOT EXISTS (SELECT 1 FROM categorie WHERE nom = 'High-tech' AND type = 'DEPENSE' AND utilisateur_id IS NULL);

-- 8. Vrais codes-barres tunisiens (source : Open Food Facts) à la place des codes de démonstration.
UPDATE produit SET nom = 'Lait demi-écrémé UHT 1L', marque = 'Délice', code_barres = '6194043001019' WHERE code_barres = '6191234500009';
UPDATE produit SET nom = 'Lait demi-écrémé UHT 1L', marque = 'Vitalait', code_barres = '6191507220214' WHERE code_barres = '6191234507015';
UPDATE produit SET nom = 'Yaourt nature 100g', marque = 'Vitalait', code_barres = '6191507249383' WHERE code_barres = '6191234514020';
UPDATE produit SET nom = 'Beurre extra fin 200g', marque = 'Délice', code_barres = '6194043001026' WHERE code_barres = '6191234521035';
UPDATE produit SET nom = 'Eau minérale naturelle 1,5L', marque = 'Safia', code_barres = '6194001800111' WHERE code_barres = '6191234535056';
UPDATE produit SET nom = 'Eau minérale naturelle 1,5L', marque = 'Sabrine', code_barres = '6194007510014' WHERE code_barres = '6191234542061';
UPDATE produit SET nom = 'Boga Lim 1,5L', marque = 'Boga', code_barres = '6194019605241' WHERE code_barres = '6191234549077';
UPDATE produit SET nom = 'Boga cidre 1,5L', marque = 'Boga', code_barres = '6194019605258' WHERE code_barres = '6191234556082';
UPDATE produit SET nom = 'Boisson au jus d''orange 1L', marque = 'Délice', code_barres = '6191534802094' WHERE code_barres = '6191234563097';
UPDATE produit SET nom = 'Double concentré de tomates 400g', marque = 'Sicam', code_barres = '6194055500128' WHERE code_barres = '6191234584122';
UPDATE produit SET nom = 'Huile d''olive vierge extra 1L', marque = 'Terra Delyssa', code_barres = '6191509900664' WHERE code_barres = '6191234591137';
UPDATE produit SET nom = 'Harissa du Cap Bon 380g', marque = 'Neblia', code_barres = '6194001450026' WHERE code_barres = '6191234612160';
UPDATE produit SET nom = 'Gaufrettes goût chocolat 100g', marque = 'Saïda', code_barres = '6194008543028' WHERE code_barres = '6191234626181';
-- Produits sans code-barres réel connu (frais, vrac...) : on retire le code inventé.
UPDATE produit SET code_barres = NULL WHERE code_barres LIKE '6191234%';

-- 9. Autres vrais produits tunisiens (leurs prix arriveront par la collecte Carrefour).
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Double concentré de tomates 800g', 'Sicam', '6194055500135', 'pièce', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Épicerie';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Biscuits Major chocolat 180g', 'Saïda', '6194008534019', 'pièce', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Épicerie';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Crackers TUC 65g', 'Lu', '6194008516046', 'pièce', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Épicerie';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Chocotom 190g', 'Tom', '6194000521017', 'pièce', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Épicerie';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Riz étuvé thaïlandais 1kg', 'Addiwen', '6194061500365', 'kg', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Épicerie';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Farine pâtissière 1kg', 'Warda', '6191564600011', 'kg', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Épicerie';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Mayonnaise 240ml', 'Jadida', '6194000803298', 'pièce', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Épicerie';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Danup 260g', 'Délice Danone', '6194003804070', 'pièce', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Produits laitiers';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Raib 420g', 'Délice', '6194043001996', 'pièce', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Produits laitiers';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Nectar multivitaminé 1L', 'Délice', '6191534802117', 'L', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Boissons';
INSERT INTO produit (nom, marque, code_barres, unite, categorie_produit_id, source)
SELECT 'Eau minérale naturelle Marwa 1,5L', 'Marwa', '6194001800722', 'L', id, 'CATALOGUE' FROM categorie_produit WHERE nom = 'Boissons';

-- 10. Univers Vêtements : quelques articles (prix relevés en magasin, saisis par l'administrateur).
INSERT INTO produit (nom, marque, unite, categorie_produit_id) SELECT 'Jean slim homme', 'Tex', 'pièce', id FROM categorie_produit WHERE nom = 'Vêtements homme';
INSERT INTO produit (nom, marque, unite, categorie_produit_id) SELECT 'T-shirt coton col rond homme', 'Tex', 'pièce', id FROM categorie_produit WHERE nom = 'Vêtements homme';
INSERT INTO produit (nom, marque, unite, categorie_produit_id) SELECT 'Robe d''été femme', 'Tex', 'pièce', id FROM categorie_produit WHERE nom = 'Vêtements femme';
INSERT INTO produit (nom, marque, unite, categorie_produit_id) SELECT 'Pyjama enfant', 'Tex', 'pièce', id FROM categorie_produit WHERE nom = 'Vêtements enfant';
INSERT INTO produit (nom, marque, unite, categorie_produit_id) SELECT 'Baskets running adulte', 'Sport', 'pièce', id FROM categorie_produit WHERE nom = 'Chaussures';

INSERT INTO prix (produit_id, magasin_id, montant, date_releve, source, statut)
SELECT p.id, m.id, v.montant, TRUNC(SYSDATE) - 2, 'ADMIN', 'VALIDE'
FROM produit p
JOIN (SELECT 'Jean slim homme' nom, 'Carrefour La Marsa' mag, 59.9 montant FROM dual
      UNION ALL SELECT 'Jean slim homme', 'Géant Tunis City', 54.9 FROM dual
      UNION ALL SELECT 'Jean slim homme', 'Monoprix Lafayette', 64.9 FROM dual
      UNION ALL SELECT 'T-shirt coton col rond homme', 'Carrefour La Marsa', 19.9 FROM dual
      UNION ALL SELECT 'T-shirt coton col rond homme', 'Géant Tunis City', 17.5 FROM dual
      UNION ALL SELECT 'T-shirt coton col rond homme', 'Monoprix Lafayette', 22.9 FROM dual
      UNION ALL SELECT 'Robe d''été femme', 'Carrefour La Marsa', 69.9 FROM dual
      UNION ALL SELECT 'Robe d''été femme', 'Monoprix Lafayette', 74.9 FROM dual
      UNION ALL SELECT 'Pyjama enfant', 'Géant Tunis City', 29.9 FROM dual
      UNION ALL SELECT 'Pyjama enfant', 'Carrefour La Marsa', 32.5 FROM dual
      UNION ALL SELECT 'Baskets running adulte', 'Géant Tunis City', 89.0 FROM dual
      UNION ALL SELECT 'Baskets running adulte', 'Carrefour La Marsa', 95.0 FROM dual) v ON v.nom = p.nom
JOIN magasin m ON m.nom = v.mag;
