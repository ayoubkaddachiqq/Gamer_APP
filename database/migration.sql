-- Migration: Integrate Gamer_Module authentication into Feed's gamer_app database
-- Run this against the `gamer_app` database
-- This script is idempotent — safe to run multiple times

-- 1. Add authentication-related columns to the existing users table
SET @db = (SELECT DATABASE());

-- password_hash
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'password_hash');
SET @sql = IF(@exists = 0, 'ALTER TABLE users ADD COLUMN password_hash VARCHAR(100) NOT NULL DEFAULT \'\' AFTER email', 'SELECT \'password_hash already exists\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- role
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'role');
SET @sql = IF(@exists = 0, 'ALTER TABLE users ADD COLUMN role ENUM(\'PLAYER\', \'TEAM\', \'ADMIN\') NOT NULL DEFAULT \'PLAYER\' AFTER password_hash', 'SELECT \'role already exists\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- status
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'status');
SET @sql = IF(@exists = 0, 'ALTER TABLE users ADD COLUMN status ENUM(\'ACTIVE\', \'PENDING\', \'LOCKED\') NOT NULL DEFAULT \'ACTIVE\' AFTER role', 'SELECT \'status already exists\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- email_verified
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email_verified');
SET @sql = IF(@exists = 0, 'ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE AFTER status', 'SELECT \'email_verified already exists\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- created_at
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'created_at');
SET @sql = IF(@exists = 0, 'ALTER TABLE users ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER profile_photo', 'SELECT \'created_at already exists\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- updated_at
SET @exists = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'users' AND COLUMN_NAME = 'updated_at');
SET @sql = IF(@exists = 0, 'ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at', 'SELECT \'updated_at already exists\'');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Copy existing plain-text passwords to password_hash (for backward compatibility)
UPDATE users SET password_hash = `password` WHERE `password` IS NOT NULL AND `password` != '' AND (password_hash IS NULL OR password_hash = '');
