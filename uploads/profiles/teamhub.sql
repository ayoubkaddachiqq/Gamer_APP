-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1:3306
-- Généré le : lun. 11 mai 2026 à 20:23
-- Version du serveur : 8.4.7
-- Version de PHP : 8.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `teamhub`
--

-- --------------------------------------------------------

--
-- Structure de la table `annonce`
--

DROP TABLE IF EXISTS `annonce`;
CREATE TABLE IF NOT EXISTS `annonce` (
  `id` int NOT NULL AUTO_INCREMENT,
  `titre` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `jeu` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `salaire` double DEFAULT NULL,
  `date_publication` date DEFAULT NULL,
  `statut` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_categorie` int DEFAULT NULL,
  `image_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `id_categorie` (`id_categorie`)
) ENGINE=MyISAM AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `annonce`
--

INSERT INTO `annonce` (`id`, `titre`, `description`, `jeu`, `salaire`, `date_publication`, `statut`, `id_categorie`, `image_path`) VALUES
(1, 'Recherche Tank CS2', 'ON CHERCHE UN JOUEUR ACTIF', 'CS2', 2000, '2026-05-07', 'OUVERTE', 1, NULL),
(2, 'harry up', 'on cherche', 'valorant', 3000, '2026-05-07', 'OUVERTE', 1, 'C:\\Users\\cherif\\Desktop\\images (3).jpg'),
(3, 'Recherche Duelist Valorant', 'Equipe semi-pro cherche un joueur actif pour scrims et tournois chaque weekend.', 'Valorant', 3500, '2026-05-07', 'OUVERTE', 3, '/images/annonces/valorant.png'),
(4, 'Support League of Legends', 'Besoin d\'un support serieux, disponible le soir, bon niveau macro et communication vocale.', 'League of Legends', 2800, '2026-05-07', 'OUVERTE', 2, '/images/annonces/lol.png'),
(5, 'Coach Fortnite', 'Structure e-sport recherche coach pour encadrer les joueurs et preparer les strategies.', 'Fortnite', 4500, '2026-05-07', 'EN_ATTENTE', 6, '/images/annonces/fortnite.png'),
(6, 'Defenseur Rocket League', 'Equipe recrute un defenseur solide pour competition locale et entrainements reguliers.', 'Rocket League', 2200, '2026-05-07', 'OUVERTE', 4, '/images/annonces/rocket-league.png'),
(7, 'Strategiste CS2', 'Nous cherchons un profil tactique pour analyser les matchs et preparer les calls.', 'CS2', 3200, '2026-05-07', 'OUVERTE', 5, '/images/annonces/cs2.png'),
(8, 'Tank Overwatch 2', 'Roster cherche un tank principal avec experience en ranked et bonne communication.', 'Overwatch 2', 3000, '2026-05-07', 'FERMEE', 1, '/images/annonces/overwatch.png');

-- --------------------------------------------------------

--
-- Structure de la table `categorie`
--

DROP TABLE IF EXISTS `categorie`;
CREATE TABLE IF NOT EXISTS `categorie` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nom` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Déchargement des données de la table `categorie`
--

INSERT INTO `categorie` (`id`, `nom`, `description`) VALUES
(1, 'Tank', 'Joueur défensif'),
(2, 'Support', 'Joueur support'),
(3, 'Attaquant', 'Joueur offensif'),
(4, 'Defenseur', 'Joueur defensif'),
(5, 'Strategiste', 'Joueur tactique'),
(6, 'Coach', 'Encadrement equipe');
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
