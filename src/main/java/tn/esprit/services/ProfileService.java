package tn.esprit.services;

import tn.esprit.entities.UserProfile;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.Instant;

public class ProfileService {

    public UserProfile getProfile(int userId) {
        String sql = "SELECT * FROM user_profiles WHERE user_id = ?";
        try (Connection conn = MyDB.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProfile(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching profile: " + e.getMessage());
        }
        return null;
    }

    public void createProfile(int userId, String displayName) {
        String sql = "INSERT INTO user_profiles (user_id, display_name, avatar_url) VALUES (?, ?, ?)";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, displayName);
            ps.setString(3, "avatars/avatar1.png");
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating profile: " + e.getMessage());
        }
    }

    public void updateProfile(int userId, String displayName, String bio) {
        String sql = "UPDATE user_profiles SET display_name = ?, bio = ? WHERE user_id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, displayName);
            ps.setString(2, bio);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating profile: " + e.getMessage());
        }
    }

    public void updateLastLogin(int userId) {
        String sql = "UPDATE user_profiles SET last_login_at = NOW() WHERE user_id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }

    private UserProfile mapProfile(ResultSet rs) throws SQLException {
        UserProfile profile = new UserProfile();
        profile.setUserId(rs.getInt("user_id"));
        profile.setDisplayName(rs.getString("display_name"));
        profile.setBio(rs.getString("bio"));
        profile.setAvatarUrl(rs.getString("avatar_url"));
        Timestamp ts = rs.getTimestamp("last_login_at");
        if (ts != null) profile.setLastLoginAt(ts.toInstant());
        ts = rs.getTimestamp("updated_at");
        if (ts != null) profile.setUpdatedAt(ts.toInstant());
        return profile;
    }
}
