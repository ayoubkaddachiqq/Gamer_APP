package tn.esprit.demo.dao;

import tn.esprit.demo.model.UserProfile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class ProfileDao {
    private final DataSource dataSource;

    public ProfileDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<UserProfile> findByUserId(long userId) {
        String sql = "SELECT * FROM user_profiles WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load profile", e);
        }
        return Optional.empty();
    }

    public void upsert(UserProfile profile) {
        String sql = "INSERT INTO user_profiles (user_id, display_name, bio, avatar_url, last_login_at) "
                + "VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), bio = VALUES(bio), "
                + "avatar_url = VALUES(avatar_url), last_login_at = VALUES(last_login_at)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, profile.getUserId());
            statement.setString(2, profile.getDisplayName());
            statement.setString(3, profile.getBio());
            statement.setString(4, profile.getAvatarUrl());
            if (profile.getLastLoginAt() != null) {
                statement.setTimestamp(5, Timestamp.from(profile.getLastLoginAt()));
            } else {
                statement.setTimestamp(5, null);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert profile", e);
        }
    }

    public void updateLastLogin(long userId, Instant loginAt) {
        String sql = "UPDATE user_profiles SET last_login_at = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(loginAt));
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update last login", e);
        }
    }

    public void updateAvatarUrl(long userId, String avatarUrl) {
        String sql = "UPDATE user_profiles SET avatar_url = ? WHERE user_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, avatarUrl);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update avatar URL", e);
        }
    }

    private UserProfile mapRow(ResultSet rs) throws SQLException {
        UserProfile profile = new UserProfile();
        profile.setUserId(rs.getLong("user_id"));
        profile.setDisplayName(rs.getString("display_name"));
        profile.setBio(rs.getString("bio"));
        profile.setAvatarUrl(rs.getString("avatar_url"));
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (lastLogin != null) {
            profile.setLastLoginAt(lastLogin.toInstant());
        }
        if (updatedAt != null) {
            profile.setUpdatedAt(updatedAt.toInstant());
        }
        return profile;
    }
}
