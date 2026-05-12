package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.entities.UserRole;
import tn.esprit.entities.UserStatus;
import tn.esprit.utils.MyDB;
import tn.esprit.utils.PasswordHasher;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.Validators;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class AuthService {
    private final PasswordHasher passwordHasher = new PasswordHasher();
    private final Validators validators = new Validators();

    public User login(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Invalid email or password");
                }

                User user = mapUser(rs);
                String storedHash = user.getPasswordHash();

                if (storedHash == null || storedHash.isEmpty()) {
                    throw new IllegalArgumentException("Invalid email or password");
                }

                if (passwordHasher.matches(password, storedHash)) {
                    SessionManager.setCurrentUser(user);
                    return user;
                }

                if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$")) {
                    throw new IllegalArgumentException("Invalid email or password");
                }

                if (!password.equals(storedHash)) {
                    throw new IllegalArgumentException("Invalid email or password");
                }

                rehashPassword(conn, user.getId(), password);
                SessionManager.setCurrentUser(user);
                return user;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error during login", e);
        }
    }

    private void rehashPassword(Connection conn, int userId, String plainPassword) throws SQLException {
        String hash = passwordHasher.hash(plainPassword);
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public User register(String username, String email, String password, UserRole role) {
        email = email.trim().toLowerCase();
        username = username.trim();

        if (!validators.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (!validators.isValidUsername(username)) {
            throw new IllegalArgumentException("Username must be 3-20 alphanumeric characters");
        }
        if (!validators.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must contain uppercase, lowercase, digit, and be 8+ characters");
        }

        try (Connection conn = getConnection()) {
            if (emailExists(conn, email)) {
                throw new IllegalArgumentException("Email already registered");
            }
            if (usernameExists(conn, username)) {
                throw new IllegalArgumentException("Username already taken");
            }

            String hash = passwordHasher.hash(password);
            String sql = "INSERT INTO users (username, email, password_hash, role, status, email_verified, profile_photo) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, username);
                ps.setString(2, email);
                ps.setString(3, hash);
                ps.setString(4, role != null ? role.name() : UserRole.PLAYER.name());
                ps.setString(5, UserStatus.ACTIVE.name());
                ps.setBoolean(6, true);
                ps.setString(7, "uploads/profiles/default.png");
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newId = keys.getInt(1);

                        try (PreparedStatement pp = conn.prepareStatement(
                            "INSERT INTO user_profiles (user_id, display_name) VALUES (?, ?)")) {
                            pp.setInt(1, newId);
                            pp.setString(2, username);
                            pp.executeUpdate();
                        }

                        User user = new User();
                        user.setId(newId);
                        user.setUsername(username);
                        user.setEmail(email);
                        user.setPasswordHash(hash);
                        user.setRole(role != null ? role : UserRole.PLAYER);
                        user.setStatus(UserStatus.ACTIVE);
                        user.setEmailVerified(true);
                        user.setProfilePhoto("uploads/profiles/default.png");

                        SessionManager.setCurrentUser(user);
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error during registration", e);
        }
        throw new IllegalStateException("Registration failed");
    }

    public void logout() {
        SessionManager.logout();
    }

    private boolean emailExists(Connection conn, String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean usernameExists(Connection conn, String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));

        user.setPasswordHash(rs.getString("password_hash"));

        try {
            user.setRole(UserRole.valueOf(rs.getString("role")));
        } catch (Exception e) {
            user.setRole(UserRole.PLAYER);
        }
        try {
            user.setStatus(UserStatus.valueOf(rs.getString("status")));
        } catch (Exception e) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setEmailVerified(rs.getBoolean("email_verified"));
        user.setProfilePhoto(rs.getString("profile_photo"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toInstant());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) user.setUpdatedAt(updatedAt.toInstant());

        return user;
    }

    private Connection getConnection() {
        return MyDB.getInstance().getConnection();
    }
}
