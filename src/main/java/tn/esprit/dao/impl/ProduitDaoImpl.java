package tn.esprit.dao.impl;

import tn.esprit.dao.ProduitDao;
import tn.esprit.model.Produit;
import tn.esprit.util.config.DatabaseConnection;
import tn.esprit.util.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProduitDaoImpl implements ProduitDao {

    private static final String INSERT_SQL = """
            INSERT INTO produit (nom, description, prix, stock, categorie, actif)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String SELECT_BY_ID_SQL = """
            SELECT id, nom, description, prix, stock, categorie, actif, created_at, updated_at
            FROM produit
            WHERE id = ?
            """;
    private static final String SELECT_ALL_SQL = """
            SELECT id, nom, description, prix, stock, categorie, actif, created_at, updated_at
            FROM produit
            ORDER BY id
            """;
    private static final String SELECT_ACTIVE_SQL = """
            SELECT id, nom, description, prix, stock, categorie, actif, created_at, updated_at
            FROM produit
            WHERE actif = TRUE
            ORDER BY nom
            """;
    private static final String UPDATE_SQL = """
            UPDATE produit
            SET nom = ?, description = ?, prix = ?, stock = ?, categorie = ?, actif = ?
            WHERE id = ?
            """;
    private static final String DELETE_SQL = "DELETE FROM produit WHERE id = ?";
    private static final String UPDATE_STOCK_SQL = "UPDATE produit SET stock = ? WHERE id = ?";

    /**
     * Persists a produit and returns it with its generated id.
     */
    @Override
    public Produit createProduit(Produit produit) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            fillStatement(statement, produit);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    produit.setId(keys.getInt(1));
                }
            }
            return produit;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to create produit.", exception);
        }
    }

    /**
     * Fetches one produit from the database by id.
     */
    @Override
    public Optional<Produit> getProduitById(int id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapProduit(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load produit with id " + id + ".", exception);
        }
    }

    /**
     * Fetches every produit row.
     */
    @Override
    public List<Produit> getAllProduits() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            List<Produit> produits = new ArrayList<>();
            while (resultSet.next()) {
                produits.add(mapProduit(resultSet));
            }
            return produits;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load produits.", exception);
        }
    }

    /**
     * Updates all editable produit fields.
     */
    @Override
    public Produit updateProduit(Produit produit) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            fillStatement(statement, produit);
            statement.setInt(7, produit.getId());
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new DataAccessException("No produit updated for id " + produit.getId() + ".");
            }
            return produit;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update produit with id " + produit.getId() + ".", exception);
        }
    }

    /**
     * Deletes one produit row.
     */
    @Override
    public boolean deleteProduit(int id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to delete produit with id " + id + ".", exception);
        }
    }

    /**
     * Updates only the stock column of one produit.
     */
    @Override
    public boolean updateStock(int produitId, int nouveauStock) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STOCK_SQL)) {

            statement.setInt(1, nouveauStock);
            statement.setInt(2, produitId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update stock for produit id " + produitId + ".", exception);
        }
    }

    /**
     * Returns only active produit rows.
     */
    @Override
    public List<Produit> getProduitsActifs() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ACTIVE_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            List<Produit> produits = new ArrayList<>();
            while (resultSet.next()) {
                produits.add(mapProduit(resultSet));
            }
            return produits;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load active produits.", exception);
        }
    }

    /**
     * Fills an INSERT or UPDATE prepared statement from a Produit object.
     */
    private void fillStatement(PreparedStatement statement, Produit produit) throws SQLException {
        statement.setString(1, produit.getNom());
        statement.setString(2, produit.getDescription());
        statement.setBigDecimal(3, produit.getPrix());
        statement.setInt(4, produit.getStock());
        statement.setString(5, produit.getCategorie());
        statement.setBoolean(6, produit.isActif());
    }

    /**
     * Maps the current ResultSet row into a Produit object.
     */
    private Produit mapProduit(ResultSet resultSet) throws SQLException {
        Produit produit = new Produit();
        produit.setId(resultSet.getInt("id"));
        produit.setNom(resultSet.getString("nom"));
        produit.setDescription(resultSet.getString("description"));
        produit.setPrix(resultSet.getBigDecimal("prix"));
        produit.setStock(resultSet.getInt("stock"));
        produit.setCategorie(resultSet.getString("categorie"));
        produit.setActif(resultSet.getBoolean("actif"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            produit.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            produit.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return produit;
    }
}
