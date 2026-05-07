package tn.esprit.demo.dao;

import tn.esprit.demo.model.TokenRecord;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

public class TokenDao {
    private final DataSource dataSource;
    private final String tableName;

    public TokenDao(DataSource dataSource, String tableName) {
        this.dataSource = dataSource;
        this.tableName = tableName;
    }

    public long create(TokenRecord record) {
        String sql = "INSERT INTO " + tableName + " (user_id, code_hash, expires_at, request_ip) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, record.getUserId());
            statement.setString(2, record.getCodeHash());
            statement.setTimestamp(3, Timestamp.from(record.getExpiresAt()));
            statement.setString(4, record.getRequestIp());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create token", e);
        }
        throw new IllegalStateException("Failed to create token, no id returned");
    }

    public Optional<TokenRecord> findActiveByUserId(long userId) {
        String sql = "SELECT * FROM " + tableName + " WHERE user_id = ? AND used_at IS NULL ORDER BY created_at DESC LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load token", e);
        }
        return Optional.empty();
    }

    public void markUsed(long id) {
        String sql = "UPDATE " + tableName + " SET used_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to mark token used", e);
        }
    }

    private TokenRecord mapRow(ResultSet rs) throws SQLException {
        TokenRecord record = new TokenRecord();
        record.setId(rs.getLong("id"));
        record.setUserId(rs.getLong("user_id"));
        record.setCodeHash(rs.getString("code_hash"));
        record.setRequestIp(rs.getString("request_ip"));
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        Timestamp usedAt = rs.getTimestamp("used_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (expiresAt != null) {
            record.setExpiresAt(expiresAt.toInstant());
        }
        if (usedAt != null) {
            record.setUsedAt(usedAt.toInstant());
        }
        if (createdAt != null) {
            record.setCreatedAt(createdAt.toInstant());
        }
        return record;
    }
}
