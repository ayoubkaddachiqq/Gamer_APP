package tn.esprit.dao.impl;

import tn.esprit.dao.ProduitPanierDao;
import tn.esprit.model.Produit;
import tn.esprit.model.ProduitPanier;
import tn.esprit.util.config.DatabaseConnection;
import tn.esprit.util.exception.DataAccessException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProduitPanierDaoImpl implements ProduitPanierDao {

    private static final String INSERT_SQL = """
            INSERT INTO produit_panier (panier_id, produit_id, quantite, prix_unitaire, sous_total)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_SQL = """
            UPDATE produit_panier
            SET quantite = ?, prix_unitaire = ?, sous_total = ?
            WHERE panier_id = ? AND produit_id = ?
            """;
    private static final String DELETE_SQL = """
            DELETE FROM produit_panier
            WHERE panier_id = ? AND produit_id = ?
            """;
    private static final String CLEAR_SQL = "DELETE FROM produit_panier WHERE panier_id = ?";
    private static final String EXISTS_SQL = """
            SELECT 1
            FROM produit_panier
            WHERE panier_id = ? AND produit_id = ?
            """;
    private static final String SELECT_BY_IDS_SQL = """
            SELECT pp.panier_id, pp.produit_id, pp.quantite, pp.prix_unitaire, pp.sous_total, pp.added_at,
                   p.id AS p_id, p.nom, p.description, p.prix, p.stock, p.categorie, p.actif,
                   p.created_at AS p_created_at, p.updated_at AS p_updated_at
            FROM produit_panier pp
            INNER JOIN produit p ON p.id = pp.produit_id
            WHERE pp.panier_id = ? AND pp.produit_id = ?
            """;
    private static final String SELECT_BY_PANIER_SQL = """
            SELECT pp.panier_id, pp.produit_id, pp.quantite, pp.prix_unitaire, pp.sous_total, pp.added_at,
                   p.id AS p_id, p.nom, p.description, p.prix, p.stock, p.categorie, p.actif,
                   p.created_at AS p_created_at, p.updated_at AS p_updated_at
            FROM produit_panier pp
            INNER JOIN produit p ON p.id = pp.produit_id
            WHERE pp.panier_id = ?
            ORDER BY pp.added_at
            """;

    /**
     * Inserts one relation-table line for a panier/product pair.
     */
    @Override
    public ProduitPanier addProduitToPanier(ProduitPanier produitPanier) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {

            statement.setInt(1, produitPanier.getPanierId());
            statement.setInt(2, produitPanier.getProduitId());
            statement.setInt(3, produitPanier.getQuantite());
            statement.setBigDecimal(4, produitPanier.getPrixUnitaire());
            statement.setBigDecimal(5, produitPanier.getSousTotal());
            statement.executeUpdate();
            return produitPanier;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to add produit to panier.", exception);
        }
    }

    /**
     * Updates quantity and subtotals for one existing panier line.
     */
    @Override
    public boolean updateQuantiteProduitDansPanier(int panierId, int produitId, int quantite,
                                                   BigDecimal prixUnitaire, BigDecimal sousTotal) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            statement.setInt(1, quantite);
            statement.setBigDecimal(2, prixUnitaire);
            statement.setBigDecimal(3, sousTotal);
            statement.setInt(4, panierId);
            statement.setInt(5, produitId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update produit quantity in panier.", exception);
        }
    }

    /**
     * Deletes one product line from a panier.
     */
    @Override
    public boolean removeProduitFromPanier(int panierId, int produitId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {

            statement.setInt(1, panierId);
            statement.setInt(2, produitId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to remove produit from panier.", exception);
        }
    }

    /**
     * Loads every product line for one panier, joined with produit details.
     */
    @Override
    public List<ProduitPanier> getProduitsByPanierId(int panierId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_PANIER_SQL)) {

            statement.setInt(1, panierId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ProduitPanier> lignes = new ArrayList<>();
                while (resultSet.next()) {
                    lignes.add(mapProduitPanier(resultSet));
                }
                return lignes;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load panier items for panier id " + panierId + ".", exception);
        }
    }

    /**
     * Deletes all lines from one panier.
     */
    @Override
    public boolean clearPanier(int panierId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(CLEAR_SQL)) {

            statement.setInt(1, panierId);
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to clear panier id " + panierId + ".", exception);
        }
    }

    /**
     * Checks whether one produit already exists in a panier.
     */
    @Override
    public boolean existsProduitInPanier(int panierId, int produitId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(EXISTS_SQL)) {

            statement.setInt(1, panierId);
            statement.setInt(2, produitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to check produit in panier.", exception);
        }
    }

    /**
     * Loads one specific panier line if it exists.
     */
    @Override
    public Optional<ProduitPanier> getProduitDansPanier(int panierId, int produitId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_IDS_SQL)) {

            statement.setInt(1, panierId);
            statement.setInt(2, produitId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProduitPanier(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load produit from panier.", exception);
        }
    }

    /**
     * Maps a joined relation-table row into a ProduitPanier object with embedded Produit.
     */
    private ProduitPanier mapProduitPanier(ResultSet resultSet) throws SQLException {
        ProduitPanier produitPanier = new ProduitPanier();
        produitPanier.setPanierId(resultSet.getInt("panier_id"));
        produitPanier.setProduitId(resultSet.getInt("produit_id"));
        produitPanier.setQuantite(resultSet.getInt("quantite"));
        produitPanier.setPrixUnitaire(resultSet.getBigDecimal("prix_unitaire"));
        produitPanier.setSousTotal(resultSet.getBigDecimal("sous_total"));

        Timestamp addedAt = resultSet.getTimestamp("added_at");
        if (addedAt != null) {
            produitPanier.setAddedAt(addedAt.toLocalDateTime());
        }

        Produit produit = new Produit();
        produit.setId(resultSet.getInt("p_id"));
        produit.setNom(resultSet.getString("nom"));
        produit.setDescription(resultSet.getString("description"));
        produit.setPrix(resultSet.getBigDecimal("prix"));
        produit.setStock(resultSet.getInt("stock"));
        produit.setCategorie(resultSet.getString("categorie"));
        produit.setActif(resultSet.getBoolean("actif"));

        Timestamp produitCreatedAt = resultSet.getTimestamp("p_created_at");
        if (produitCreatedAt != null) {
            produit.setCreatedAt(produitCreatedAt.toLocalDateTime());
        }

        Timestamp produitUpdatedAt = resultSet.getTimestamp("p_updated_at");
        if (produitUpdatedAt != null) {
            produit.setUpdatedAt(produitUpdatedAt.toLocalDateTime());
        }

        produitPanier.setProduit(produit);
        return produitPanier;
    }
}
