package tn.esprit.services;

import tn.esprit.utils.MyDB;

import java.sql.*;

public class AvatarService {

    public String getAvatarUrl(int userId) {
        String sql = "SELECT avatar_url FROM user_profiles WHERE user_id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String url = rs.getString("avatar_url");
                    if (url != null && !url.isEmpty()) return url;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching avatar: " + e.getMessage());
        }
        return "avatars/avatar1.png";
    }

    public void updateAvatar(int userId, String avatarUrl) {
        String sql = "UPDATE user_profiles SET avatar_url = ? WHERE user_id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setString(1, avatarUrl);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating avatar: " + e.getMessage());
        }
    }
}
