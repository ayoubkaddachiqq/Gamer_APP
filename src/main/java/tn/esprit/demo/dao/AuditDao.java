package tn.esprit.demo.dao;

import tn.esprit.demo.model.AuditEvent;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class AuditDao {
    private final DataSource dataSource;

    public AuditDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<AuditEvent> findRecent(int limit) {
        String sql = "SELECT a.id, a.user_id, u.username, a.action, a.metadata, a.created_at " +
                     "FROM audit_log a LEFT JOIN users u ON a.user_id = u.id " +
                     "ORDER BY a.created_at DESC LIMIT ?";
        List<AuditEvent> events = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditEvent e = new AuditEvent();
                    e.setId(rs.getLong("id"));
                    e.setUserId(rs.getLong("user_id"));
                    e.setUsername(rs.getString("username"));
                    e.setAction(rs.getString("action"));
                    e.setMetadata(rs.getString("metadata"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) e.setCreatedAt(ts.toInstant());
                    events.add(e);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load audit log", e);
        }
        return events;
    }

    public int countTodayLogins() {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE action='LOGIN_SUCCESS' AND DATE(created_at)=CURDATE()";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count logins", e);
        }
        return 0;
    }

    public void log(Long userId, String action, String metadata) {
        String sql = "INSERT INTO audit_log (user_id, action, metadata) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (userId == null) {
                statement.setNull(1, java.sql.Types.BIGINT);
            } else {
                statement.setLong(1, userId);
            }
            statement.setString(2, action);
            statement.setString(3, metadata);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to write audit log", e);
        }
    }
}
