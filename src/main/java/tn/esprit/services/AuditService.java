package tn.esprit.services;

import tn.esprit.entities.AuditEvent;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditService {

    public void log(int userId, String action, String metadata) {
        String sql = "INSERT INTO audit_log (user_id, action, metadata) VALUES (?, ?, ?)";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, metadata);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging audit: " + e.getMessage());
        }
    }

    public List<AuditEvent> getRecentLogs(int limit) {
        List<AuditEvent> events = new ArrayList<>();
        String sql = "SELECT a.*, COALESCE(u.username, 'Deleted User') AS username "
                   + "FROM audit_log a LEFT JOIN users u ON a.user_id = u.id "
                   + "ORDER BY a.created_at DESC LIMIT ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditEvent e = new AuditEvent();
                    e.setId(rs.getInt("id"));
                    e.setUserId(rs.getInt("user_id"));
                    e.setUsername(rs.getString("username"));
                    e.setAction(rs.getString("action"));
                    e.setMetadata(rs.getString("metadata"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) e.setCreatedAt(ts.toInstant());
                    events.add(e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching audit logs: " + e.getMessage());
        }
        return events;
    }

    public List<AuditEvent> getAllLogs() {
        return getRecentLogs(1000);
    }
}
