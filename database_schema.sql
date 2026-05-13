-- Schema MySQL pour l'application Gamer_APP / TeamHub
-- Base utilisee dans org.esprit.utils.MyDataBase:
-- jdbc:mysql://localhost:3306/teamhub

CREATE DATABASE IF NOT EXISTS teamhub
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE teamhub;

CREATE TABLE IF NOT EXISTS type_evenement (
  id INT NOT NULL AUTO_INCREMENT,
  libelle VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_evenement_libelle (libelle)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evenement (
  id INT NOT NULL AUTO_INCREMENT,
  titre VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  type_id INT NOT NULL,
  date_debut DATETIME NOT NULL,
  date_fin DATETIME NOT NULL,
  lieu VARCHAR(255) NOT NULL,
  nb_participants_max INT NOT NULL,
  statut VARCHAR(50) NOT NULL DEFAULT 'Planifié',
  image VARCHAR(500) NULL,
  PRIMARY KEY (id),
  KEY idx_evenement_type_id (type_id),
  KEY idx_evenement_date_debut (date_debut),
  KEY idx_evenement_lieu (lieu),
  CONSTRAINT fk_evenement_type
    FOREIGN KEY (type_id)
    REFERENCES type_evenement (id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,
  CONSTRAINT chk_evenement_dates
    CHECK (date_fin > date_debut),
  CONSTRAINT chk_evenement_nb_participants
    CHECK (nb_participants_max > 0),
  CONSTRAINT chk_evenement_statut
    CHECK (statut IN ('Planifié', 'En cours', 'Terminé', 'Annulé'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inscription (
  id INT NOT NULL AUTO_INCREMENT,
  evenement_id INT NOT NULL,
  utilisateur_id INT NOT NULL,
  date_inscription DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  statut VARCHAR(50) NOT NULL DEFAULT 'En attente',
  PRIMARY KEY (id),
  UNIQUE KEY uk_inscription_evenement_utilisateur (evenement_id, utilisateur_id),
  KEY idx_inscription_evenement_id (evenement_id),
  KEY idx_inscription_utilisateur_id (utilisateur_id),
  KEY idx_inscription_statut (statut),
  CONSTRAINT fk_inscription_evenement
    FOREIGN KEY (evenement_id)
    REFERENCES evenement (id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  CONSTRAINT chk_inscription_statut
    CHECK (statut IN ('En attente', 'Confirmé', 'Annulé'))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Donnees de reference creees aussi automatiquement par TypeEvenementService.
INSERT INTO type_evenement (libelle) VALUES
  ('Entrainement'),
  ('Tournoi'),
  ('Rencontre'),
  ('LAN Party'),
  ('Workshop'),
  ('Qualification'),
  ('Finale'),
  ('Streaming'),
  ('Networking')
ON DUPLICATE KEY UPDATE libelle = VALUES(libelle);

-- Remarque d'integration:
-- La table inscription contient utilisateur_id, mais ce projet ne definit pas
-- de table utilisateur. Si un autre module gere les utilisateurs, ajouter la
-- cle etrangere suivante apres creation de cette table:
--
-- ALTER TABLE inscription
--   ADD CONSTRAINT fk_inscription_utilisateur
--   FOREIGN KEY (utilisateur_id)
--   REFERENCES utilisateur (id)
--   ON UPDATE CASCADE
--   ON DELETE RESTRICT;
