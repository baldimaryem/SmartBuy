-- =====================================================================
-- SmartBuy : création de l'utilisateur Oracle SMARTBUY.
--
-- Base : Oracle Database 19c installée sur le poste (localhost:1521, service ORCL).
-- À exécuter UNE SEULE FOIS dans SQL Developer avec une connexion SYS
-- (rôle SYSDBA) sur localhost:1521/orcl. Tout sélectionner puis F5 (Run Script).
--
-- Ensuite, connexion SQL Developer « SmartBuy » : smartbuy / smartbuy,
-- localhost, 1521, Service name = orcl. Les tables sont créées par l'API
-- au premier démarrage, ou à la main avec 01_schema.sql puis 02_data.sql.
-- =====================================================================

-- Vérification : on doit être sur le poste local, pas sur un serveur client.
SELECT sys_context('USERENV', 'SERVER_HOST') AS serveur,
       sys_context('USERENV', 'CON_NAME')    AS conteneur
FROM dual;

CREATE USER smartbuy IDENTIFIED BY smartbuy
    DEFAULT TABLESPACE users
    TEMPORARY TABLESPACE temp
    QUOTA UNLIMITED ON users;

GRANT CREATE SESSION  TO smartbuy;
GRANT CREATE TABLE    TO smartbuy;
GRANT CREATE SEQUENCE TO smartbuy;
GRANT CREATE VIEW     TO smartbuy;
