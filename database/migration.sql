-- Migration: Create proper users table matching Gamer_Module schema (with INT id)
-- Run this against the `gamer_app` database
-- Handles: fresh install, partial re-runs, and existing data migration

SET @db = (SELECT DATABASE());

-- Temporarily disable FK checks so we can rename/drop tables safely
SET @fk_was_on = (SELECT @@FOREIGN_KEY_CHECKS);
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- STEP 1: Backup old users table if it exists
--         First drop any leftover backup from a previous failed run
-- ============================================================
DROP TABLE IF EXISTS users_old_backup;

SET @users_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users');
SET @sql = IF(@users_exists = 1, 'RENAME TABLE users TO users_old_backup', 'SELECT \'no table to backup\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- STEP 2: Create new users table
-- ============================================================
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

-- ============================================================
-- STEP 3: Restore data from backup
--         Dynamically detect what password column the old table used
-- ============================================================
SET @backup_exists = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users_old_backup');

-- Detect password column name: prefer password_hash, fall back to password
SET @pw_col = NULL;
SET @has_pw_hash = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users_old_backup' AND COLUMN_NAME = 'password_hash');
SET @has_pw_plain = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users_old_backup' AND COLUMN_NAME = 'password');
SET @pw_col = IF(@has_pw_hash = 1, 'password_hash', NULL);
SET @pw_col = IF(@pw_col IS NULL AND @has_pw_plain = 1, 'password', @pw_col);

SET @restore_sql = 'SELECT \'no data to restore\'';
SET @restore_sql = IF(@backup_exists = 1 AND @pw_col IS NOT NULL,
  CONCAT('INSERT INTO users (id, email, username, password_hash, profile_photo) ',
         'SELECT id, email, username, `', @pw_col, '`, ',
         'COALESCE(profile_photo, ''uploads/profiles/default.png'') ',
         'FROM users_old_backup'),
  @restore_sql);

PREPARE stmt FROM @restore_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- STEP 4: Insert test users only if table is empty
--         ALL test users use password: password123
--         BCrypt hash generated with cost 12
-- ============================================================
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
-- STEP 5: Re-add foreign key constraints to the new users table
--         (Drop old ones if they exist, create new ones)
-- ============================================================

-- Drop old FK from posts -> users_old_backup, add new FK -> users
SET @fk_posts = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'posts' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_posts IS NOT NULL, CONCAT('ALTER TABLE posts DROP FOREIGN KEY ', @fk_posts), 'SELECT \'no fk on posts\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE posts ADD CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Drop old FK from comments -> users_old_backup, add new FK -> users
SET @fk_comments = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'comments' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_comments IS NOT NULL, CONCAT('ALTER TABLE comments DROP FOREIGN KEY ', @fk_comments), 'SELECT \'no fk on comments\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE comments ADD CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Drop old FK from likes -> users_old_backup, add new FK -> users
SET @fk_likes = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'likes' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_likes IS NOT NULL, CONCAT('ALTER TABLE likes DROP FOREIGN KEY ', @fk_likes), 'SELECT \'no fk on likes\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE likes ADD CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Drop old FK from shares -> users_old_backup, add new FK -> users
SET @fk_shares = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'shares' AND COLUMN_NAME = 'user_id' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_shares IS NOT NULL, CONCAT('ALTER TABLE shares DROP FOREIGN KEY ', @fk_shares), 'SELECT \'no fk on shares\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
ALTER TABLE shares ADD CONSTRAINT fk_share_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Drop old FK from imagepost -> users_old_backup if any (imagepost may reference posts, not users directly)
SET @fk_imagepost = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'imagepost' AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql = IF(@fk_imagepost IS NOT NULL, CONCAT('ALTER TABLE imagepost DROP FOREIGN KEY ', @fk_imagepost), 'SELECT \'no fk on imagepost\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- STEP 6: Clean up backup table
-- ============================================================
DROP TABLE IF EXISTS users_old_backup;

-- Re-enable FK checks to their original state
SET @sql = CONCAT('SET FOREIGN_KEY_CHECKS = ', @fk_was_on);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
