package tn.esprit.dao.impl;

import tn.esprit.dao.PanierDao;
import tn.esprit.model.Panier;
import tn.esprit.util.config.DatabaseConnection;
import tn.esprit.util.exception.DataAccessException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PanierDaoImpl implements PanierDao {

    private static final String INSERT_SQL = """
            INSERT INTO panier (reference, statut, total)
            VALUES (?, ?, ?)
            """;
    private static final String SELECT_BY_ID_SQL = """
            SELECT id, reference, statut, total, created_at, updated_at
            FROM panier
            WHERE id = ?
            """;
    private static final String SELECT_ALL_SQL = """
            SELECT id, reference, statut, total, created_at, updated_at
            FROM panier
            ORDER BY id
            """;
    private static final String UPDATE_SQL = """
            UPDATE panier
            SET reference = ?, statut = ?, total = ?
            WHERE id = ?
            """;
    private static final String DELETE_SQL = "DELETE FROM panier WHERE id = ?";
    private static final String UPDATE_TOTAL_SQL = "UPDATE panier SET total = ? WHERE id = ?";

    /**
     * Persists a panier and returns it with its generated id.
     */
    @Override
    public Panier createPanier(Panier panier) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, panier.getReference());
            statement.setString(2, panier.getStatut());
            statement.setBigDecimal(3, panier.getTotal());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    panier.setId(keys.getInt(1));
                }
            }
            return panier;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to create panier.", exception);
        }
    }

    /**
     * Fetches one panier header by id.
     */
    @Override
    public Optional<Panier> getPanierById(int id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapPanier(resultSet));
                }
            }
            return Optional.empty();
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load panier with id " + id + ".", exception);
        }
    }

    /**
     * Fetches all panier headers.
     */
    @Override
    public List<Panier> getAllPaniers() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            List<Panier> paniers = new ArrayList<>();
            while (resultSet.next()) {
                paniers.add(mapPanier(resultSet));
            }
            return paniers;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load paniers.", exception);
        }
    }

    /**
     * Updates panier header fields.
     */
    @Override
    public Panier updatePanier(Panier panier) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            statement.setString(1, panier.getReference());
            statement.setString(2, panier.getStatut());
            statement.setBigDecimal(3, panier.getTotal());
            statement.setInt(4, panier.getId());
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new DataAccessException("No panier updated for id " + panier.getId() + ".");
            }
            return panier;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update panier with id " + panier.getId() + ".", exception);
        }
    }

    /**
     * Deletes one panier row.
     */
    @Override
    public boolean deletePanier(int id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to delete panier with id " + id + ".", exception);
        }
    }

    /**
     * Updates the stored total value of one panier.
     */
    @Override
    public boolean updateTotalPanier(int id, BigDecimal total) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_TOTAL_SQL)) {

            statement.setBigDecimal(1, total);
            statement.setInt(2, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update total for panier id " + id + ".", exception);
        }
    }

    /**
     * Maps the current ResultSet row into a Panier object.
     */
    private Panier mapPanier(ResultSet resultSet) throws SQLException {
        Panier panier = new Panier();
        panier.setId(resultSet.getInt("id"));
        panier.setReference(resultSet.getString("reference"));
        panier.setStatut(resultSet.getString("statut"));
        panier.setTotal(resultSet.getBigDecimal("total"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            panier.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            panier.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return panier;
    }
}
