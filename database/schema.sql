CREATE DATABASE IF NOT EXISTS teamhub
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE teamhub;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  role ENUM('PLAYER', 'TEAM') NOT NULL DEFAULT 'PLAYER',
  status ENUM('ACTIVE', 'PENDING', 'LOCKED') NOT NULL DEFAULT 'PENDING',
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  face_enrolled BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_username (username)
);

CREATE TABLE IF NOT EXISTS user_profiles (
  user_id BIGINT NOT NULL,
  display_name VARCHAR(80) NULL,
  bio VARCHAR(500) NULL,
  avatar_url VARCHAR(300) NULL,
  last_login_at TIMESTAMP NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS email_verification_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  code_hash VARCHAR(100) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  request_ip VARCHAR(45) NULL,
  PRIMARY KEY (id),
  KEY idx_email_tokens_user (user_id),
  KEY idx_email_tokens_expires (expires_at),
  CONSTRAINT fk_email_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  code_hash VARCHAR(100) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  request_ip VARCHAR(45) NULL,
  PRIMARY KEY (id),
  KEY idx_reset_tokens_user (user_id),
  KEY idx_reset_tokens_expires (expires_at),
  CONSTRAINT fk_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_games (
  id            INT            NOT NULL AUTO_INCREMENT,
  user_id       BIGINT         NOT NULL,
  rawg_id       INT            NOT NULL,
  game_name     VARCHAR(255)   NOT NULL,
  cover_url     VARCHAR(500)   NULL,
  genre         VARCHAR(100)   NULL,
  metacritic_score INT         NULL DEFAULT 0,
  is_favorite   BOOLEAN        NOT NULL DEFAULT FALSE,
  added_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY unique_user_game (user_id, rawg_id),
  CONSTRAINT fk_user_games_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NULL,
  action VARCHAR(60) NOT NULL,
  metadata VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_user (user_id),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS riot_accounts (
  id              INT            NOT NULL AUTO_INCREMENT,
  user_id         BIGINT         NOT NULL,
  game_name       VARCHAR(100)   NOT NULL,
  tag_line        VARCHAR(10)    NOT NULL,
  puuid           VARCHAR(100)   NULL,
  summoner_id     VARCHAR(100)   NULL,
  summoner_level  INT            NULL,
  profile_icon_id INT            NULL,
  tier            VARCHAR(20)    NULL,
  rank_division   VARCHAR(5)     NULL,
  league_points   INT            NULL,
  wins            INT            NULL,
  losses          INT            NULL,
  last_updated    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_riot_user (user_id),
  CONSTRAINT fk_riot_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
