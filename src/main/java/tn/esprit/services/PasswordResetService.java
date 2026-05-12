package tn.esprit.services;

import tn.esprit.utils.MyDB;
import tn.esprit.utils.PasswordHasher;

import java.sql.*;
import java.util.UUID;

public class PasswordResetService {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    public String createResetToken(String email) {
        String token = UUID.randomUUID().toString();
        String hash = passwordHasher.hash(token);

        String sql = "INSERT INTO password_reset_tokens (user_id, code_hash, expires_at) "
                   + "SELECT id, ?, DATE_ADD(NOW(), INTERVAL 1 HOUR) FROM users WHERE email = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, email.trim().toLowerCase());
            int affected = ps.executeUpdate();
            if (affected > 0) return token;
        } catch (SQLException e) {
            System.err.println("Error creating reset token: " + e.getMessage());
        }
        return null;
    }

    public boolean validateAndReset(String token, String newPassword) {
        String sql = "SELECT * FROM password_reset_tokens "
                   + "WHERE expires_at > NOW() AND used_at IS NULL ORDER BY created_at DESC";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String storedHash = rs.getString("code_hash");
                if (passwordHasher.matches(token, storedHash)) {
                    int tokenId = rs.getInt("id");
                    int userId = rs.getInt("user_id");
                    markUsed(tokenId);
                    updatePassword(userId, newPassword);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error validating reset token: " + e.getMessage());
        }
        return false;
    }

    private void markUsed(int tokenId) {
        String sql = "UPDATE password_reset_tokens SET used_at = NOW() WHERE id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, tokenId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error marking token used: " + e.getMessage());
        }
    }

    private void updatePassword(int userId, String password) {
        String hash = passwordHasher.hash(password);
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
        }
    }
}
