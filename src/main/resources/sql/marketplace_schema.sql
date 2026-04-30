CREATE DATABASE IF NOT EXISTS marketplace_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE marketplace_db;

CREATE TABLE IF NOT EXISTS produit (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Identifiant technique du produit',
    nom VARCHAR(150) NOT NULL COMMENT 'Nom visible par le client',
    description TEXT NULL COMMENT 'Description detaillee du produit',
    prix DECIMAL(10,2) NOT NULL COMMENT 'Prix courant du produit',
    stock INT NOT NULL DEFAULT 0 COMMENT 'Quantite disponible en stock',
    categorie VARCHAR(100) NULL COMMENT 'Categorie fonctionnelle du produit',
    actif BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Permet de masquer un produit sans le supprimer',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de creation du produit',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Date de derniere modification',
    CONSTRAINT chk_produit_prix CHECK (prix >= 0),
    CONSTRAINT chk_produit_stock CHECK (stock >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS panier (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'Identifiant technique du panier',
    reference VARCHAR(100) UNIQUE NULL COMMENT 'Reference metier utile pour retrouver un panier',
    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIF' COMMENT 'Etat du panier: ACTIF, VALIDE, ABANDONNE',
    total DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Montant total du panier synchronise avec ses lignes',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de creation du panier',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Date de derniere modification',
    CONSTRAINT chk_panier_total CHECK (total >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS produit_panier (
    panier_id INT NOT NULL COMMENT 'Reference vers le panier parent',
    produit_id INT NOT NULL COMMENT 'Reference vers le produit ajoute au panier',
    quantite INT NOT NULL COMMENT 'Nombre d unites du produit dans le panier',
    prix_unitaire DECIMAL(10,2) NOT NULL COMMENT 'Prix fige du produit au moment de l ajout',
    sous_total DECIMAL(10,2) NOT NULL COMMENT 'Quantite * prix_unitaire pour simplifier les lectures',
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date d ajout de la ligne dans le panier',
    PRIMARY KEY (panier_id, produit_id),
    CONSTRAINT fk_produit_panier_panier
        FOREIGN KEY (panier_id) REFERENCES panier(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_produit_panier_produit
        FOREIGN KEY (produit_id) REFERENCES produit(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_produit_panier_quantite CHECK (quantite > 0),
    CONSTRAINT chk_produit_panier_prix CHECK (prix_unitaire >= 0),
    CONSTRAINT chk_produit_panier_total CHECK (sous_total >= 0)
) ENGINE=InnoDB;

CREATE INDEX idx_produit_nom ON produit (nom);
CREATE INDEX idx_produit_actif ON produit (actif);
CREATE INDEX idx_panier_statut ON panier (statut);

INSERT INTO produit (nom, description, prix, stock, categorie, actif)
VALUES
    ('Clavier Mecanique', 'Clavier RGB pour gaming', 249.90, 25, 'Accessoires', TRUE),
    ('Souris Sans Fil', 'Souris ergonomique rechargeable', 89.90, 40, 'Accessoires', TRUE),
    ('Casque Audio', 'Casque stereo avec micro integre', 159.50, 15, 'Audio', TRUE);

INSERT INTO panier (id, reference, statut, total)
VALUES (1, 'PAN-DEMO-001', 'ACTIF', 0.00)
ON DUPLICATE KEY UPDATE
    reference = VALUES(reference),
    statut = VALUES(statut);

INSERT INTO produit_panier (panier_id, produit_id, quantite, prix_unitaire, sous_total)
VALUES
    (1, 1, 2, 249.90, 499.80),
    (1, 2, 1, 89.90, 89.90)
ON DUPLICATE KEY UPDATE
    quantite = VALUES(quantite),
    prix_unitaire = VALUES(prix_unitaire),
    sous_total = VALUES(sous_total);

UPDATE panier
SET total = (
    SELECT COALESCE(SUM(pp.sous_total), 0.00)
    FROM produit_panier pp
    WHERE pp.panier_id = panier.id
)
WHERE id = 1;
