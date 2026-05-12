-- Migration: Create proper users table matching Gamer_Module schema (with INT id)
-- Run this against the `gamer_app` database
-- WARNING: This backs up the existing users table to users_old_backup and creates a fresh one

SET @db = (SELECT DATABASE());

-- 1. Backup old users table (renames it to users_old_backup)
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users');
SET @sql = IF(@table_exists > 0, 'RENAME TABLE users TO users_old_backup', 'SELECT \'no table to backup\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Create new users table with all Gamer_Module columns but INT id
CREATE TABLE IF NOT EXISTS users (
  id INT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role ENUM('PLAYER', 'TEAM', 'ADMIN') NOT NULL DEFAULT 'PLAYER',
  status ENUM('ACTIVE', 'PENDING', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  profile_photo VARCHAR(300) DEFAULT 'uploads/profiles/default.png',
  face_enrolled BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Restore data from backup if it exists
--    Old table likely has: id, username, email, password, profile_photo
--    Map old `password` -> new `password_hash` (plain text, will be rehashed on first login)
SET @backup_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users_old_backup');
SET @sql = IF(@backup_exists > 0,
  'INSERT INTO users (id, email, username, password_hash, profile_photo)
   SELECT id, email, username, `password`, profile_photo
   FROM users_old_backup',
  'SELECT \'no backup to restore from\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Insert test users only if table is empty
SET @user_count = (SELECT COUNT(*) FROM users);
SET @sql = IF(@user_count = 0,
  'INSERT INTO users (id, email, username, password_hash, role, status, email_verified, profile_photo) VALUES
   (1, ''ghost@example.com'', ''GhostProtocol'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png''),
   (2, ''neon@example.com'', ''NeonSniper'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png''),
   (3, ''pixel@example.com'', ''PixelQueen'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png''),
   (4, ''shadow@example.com'', ''ShadowBlade'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png''),
   (5, ''cyber@example.com'', ''CyberWolf'', ''$2a$12$4y/i8djOcnj6rRKZVtWY4eWCpbl0HIHc5csxxIJu/wYAGKVf5Fyhq'', ''PLAYER'', ''ACTIVE'', TRUE, ''uploads/profiles/default.png'')',
  'SELECT \'users table already has data, skipping test inserts\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
