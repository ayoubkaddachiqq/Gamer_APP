package tn.esprit.services;

import tn.esprit.utils.MyDB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserService {

    private static final String DEFAULT_AVATAR = "uploads/profiles/default.png";

    public String getProfilePhoto(int userId) {
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement("SELECT profile_photo FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String path = rs.getString("profile_photo");
                if (path != null && !path.isEmpty()) return path;
            }
        } catch (Exception e) { /* ignore */ }
        return DEFAULT_AVATAR;
    }

    public String getUsername(int userId) {
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement("SELECT username FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("username");
        } catch (Exception e) { /* ignore */ }
        return "Player " + userId;
    }
}
