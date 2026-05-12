-- Migration: Full Gamer_Module schema integration into gamer_app
-- All user_id FK columns use INT (matching users.id) instead of BIGINT
-- Safe to run multiple times (idempotent)

SET @db = (SELECT DATABASE());
SET @fk_was_on = (SELECT @@FOREIGN_KEY_CHECKS);
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. users table
-- ============================================================
DROP TABLE IF EXISTS users_old_backup;
SET @users_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users');
SET @sql = IF(@users_exists = 1, 'RENAME TABLE users TO users_old_backup', 'SELECT \'skip backup\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS users (
  id INT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role ENUM('PLAYER', 'TEAM', 'ADMIN') NOT NULL DEFAULT 'PLAYER',
  status ENUM('ACTIVE', 'PENDING', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  face_enrolled BOOLEAN NOT NULL DEFAULT FALSE,
  profile_photo VARCHAR(300) DEFAULT 'uploads/profiles/default.png',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Restore old user data (detect password column name dynamically)
SET @backup_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users_old_backup');
SET @pw_col = NULL;
SET @has_pw_hash = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users_old_backup' AND COLUMN_NAME = 'password_hash');
SET @has_pw_plain = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users_old_backup' AND COLUMN_NAME = 'password');
SET @pw_col = IF(@has_pw_hash = 1, 'password_hash', NULL);
SET @pw_col = IF(@pw_col IS NULL AND @has_pw_plain = 1, 'password', @pw_col);

SET @restore_sql = 'SELECT \'no users data to restore\'';
SET @restore_sql = IF(@backup_exists = 1 AND @pw_col IS NOT NULL,
  CONCAT('INSERT INTO users (id, email, username, password_hash, profile_photo) ',
         'SELECT id, email, username, `', @pw_col, '`, ',
         'COALESCE(profile_photo, ''uploads/profiles/default.png'') ',
         'FROM users_old_backup'),
  @restore_sql);
PREPARE stmt FROM @restore_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Insert test users only if table is empty
SET @user_count = (SELECT COUNT(*) FROM users);
SET @sql = IF(@user_count = 0,
  'INSERT INTO users (id, email, username, password_hash, role, status, email_verified, profile_photo) VALUES
   (1, ''ghost@example.com'', ''GhostProtocol'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png''),
   (2, ''neon@example.com'', ''NeonSniper'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png''),
   (3, ''pixel@example.com'', ''PixelQueen'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png''),
   (4, ''shadow@example.com'', ''ShadowBlade'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png''),
   (5, ''cyber@example.com'', ''CyberWolf'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png'')',
  'SELECT ''users table already has data, skipping test inserts''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. user_profiles
-- ============================================================
CREATE TABLE IF NOT EXISTS user_profiles (
  user_id INT NOT NULL,
  display_name VARCHAR(80) NULL,
  bio VARCHAR(500) NULL,
  avatar_url VARCHAR(300) NULL,
  last_login_at TIMESTAMP NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default profiles for test users
INSERT IGNORE INTO user_profiles (user_id, display_name) VALUES
(1, 'GhostProtocol'), (2, 'NeonSniper'), (3, 'PixelQueen'), (4, 'ShadowBlade'), (5, 'CyberWolf');

-- ============================================================
-- 3. email_verification_tokens
-- ============================================================
CREATE TABLE IF NOT EXISTS email_verification_tokens (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  code_hash VARCHAR(100) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  request_ip VARCHAR(45) NULL,
  PRIMARY KEY (id),
  KEY idx_email_tokens_user (user_id),
  KEY idx_email_tokens_expires (expires_at),
  CONSTRAINT fk_email_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. password_reset_tokens
-- ============================================================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  code_hash VARCHAR(100) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  request_ip VARCHAR(45) NULL,
  PRIMARY KEY (id),
  KEY idx_reset_tokens_user (user_id),
  KEY idx_reset_tokens_expires (expires_at),
  CONSTRAINT fk_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. user_games
-- ============================================================
CREATE TABLE IF NOT EXISTS user_games (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  rawg_id INT NOT NULL,
  game_name VARCHAR(255) NOT NULL,
  cover_url VARCHAR(500) NULL,
  genre VARCHAR(100) NULL,
  metacritic_score INT NULL DEFAULT 0,
  is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
  added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY unique_user_game (user_id, rawg_id),
  CONSTRAINT fk_user_games_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. audit_log
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NULL,
  action VARCHAR(60) NOT NULL,
  metadata VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_user (user_id),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. riot_accounts
-- ============================================================
CREATE TABLE IF NOT EXISTS riot_accounts (
  id INT NOT NULL AUTO_INCREMENT,
  user_id INT NOT NULL,
  game_name VARCHAR(100) NOT NULL,
  tag_line VARCHAR(10) NOT NULL,
  puuid VARCHAR(100) NULL,
  summoner_id VARCHAR(100) NULL,
  summoner_level INT NULL,
  profile_icon_id INT NULL,
  tier VARCHAR(20) NULL,
  rank_division VARCHAR(5) NULL,
  league_points INT NULL,
  wins INT NULL,
  losses INT NULL,
  last_updated_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_riot_account (game_name, tag_line),
  CONSTRAINT fk_riot_account_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. Re-add FK constraints on Feed tables (lost during rename)
-- ============================================================
SET @fk_posts = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'posts' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_posts IS NOT NULL, CONCAT('ALTER TABLE posts DROP FOREIGN KEY ', @fk_posts), 'SELECT \'no fk on posts\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE posts ADD CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

SET @fk_comments = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'comments' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_comments IS NOT NULL, CONCAT('ALTER TABLE comments DROP FOREIGN KEY ', @fk_comments), 'SELECT \'no fk on comments\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE comments ADD CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

SET @fk_likes = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'likes' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_likes IS NOT NULL, CONCAT('ALTER TABLE likes DROP FOREIGN KEY ', @fk_likes), 'SELECT \'no fk on likes\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE likes ADD CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

SET @fk_shares = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'shares' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_shares IS NOT NULL, CONCAT('ALTER TABLE shares DROP FOREIGN KEY ', @fk_shares), 'SELECT \'no fk on shares\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE shares ADD CONSTRAINT fk_share_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

SET @fk_imagepost = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'imagepost' AND COLUMN_NAME = 'post_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_imagepost IS NOT NULL, CONCAT('ALTER TABLE imagepost DROP FOREIGN KEY ', @fk_imagepost), 'SELECT \'no fk on imagepost\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 9. Cleanup
-- ============================================================
DROP TABLE IF EXISTS users_old_backup;
SET @sql = CONCAT('SET FOREIGN_KEY_CHECKS = ', @fk_was_on);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
