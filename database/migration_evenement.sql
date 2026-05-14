-- Migration: Evenement module tables into gamer_app
-- Safe to run multiple times (idempotent)

SET @db = (SELECT DATABASE());

-- ============================================================
-- 1. type_evenement
-- ============================================================
CREATE TABLE IF NOT EXISTS type_evenement (
  id INT NOT NULL AUTO_INCREMENT,
  libelle VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_evenement_libelle (libelle)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed default types
INSERT IGNORE INTO type_evenement (libelle) VALUES
  ('Entrainement'),
  ('Tournoi'),
  ('Rencontre'),
  ('LAN Party'),
  ('Workshop'),
  ('Qualification'),
  ('Finale'),
  ('Streaming'),
  ('Networking');

-- ============================================================
-- 2. evenement
-- ============================================================
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
  user_id INT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  KEY idx_evenement_type_id (type_id),
  KEY idx_evenement_date_debut (date_debut),
  KEY idx_evenement_lieu (lieu),
  KEY idx_evenement_user (user_id),
  CONSTRAINT fk_evenement_type FOREIGN KEY (type_id) REFERENCES type_evenement (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_evenement_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT chk_evenement_dates CHECK (date_fin > date_debut),
  CONSTRAINT chk_evenement_nb_participants CHECK (nb_participants_max > 0),
  CONSTRAINT chk_evenement_statut CHECK (statut IN ('Planifié', 'En cours', 'Terminé', 'Annulé'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add user_id column if missing (for existing tables)
SET @has_user_id = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'evenement' AND COLUMN_NAME = 'user_id');
SET @sql = IF(@has_user_id = 0, 'ALTER TABLE evenement ADD COLUMN user_id INT NOT NULL DEFAULT 1 AFTER image', 'SELECT \'user_id already exists\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Ensure FK on user_id points to users, not users_old_backup
SET @fk_user_refs = (SELECT REFERENCED_TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'evenement' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @fk_user_name = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'evenement' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_user_refs IS NULL OR @fk_user_refs != 'users', CONCAT('ALTER TABLE evenement DROP FOREIGN KEY ', @fk_user_name), 'SELECT \'fk_evenement_user already correct\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = 'ALTER TABLE evenement ADD CONSTRAINT fk_evenement_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE';
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. inscription
-- ============================================================
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
  CONSTRAINT fk_inscription_evenement FOREIGN KEY (evenement_id) REFERENCES evenement (id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT fk_inscription_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES users (id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT chk_inscription_statut CHECK (statut IN ('En attente', 'Confirmé', 'Annulé'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
