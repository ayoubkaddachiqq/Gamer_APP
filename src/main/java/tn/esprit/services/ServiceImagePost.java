package tn.esprit.services;

import tn.esprit.entities.ImagePost;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceImagePost {

    public ServiceImagePost() {
    }

    private Connection getConnection() {
        return MyDB.getInstance().getConnection();
    }

    public void addImage(int postId, String imagePath) {
        String qry = "INSERT INTO imagepost (post_id, image_path) VALUES (?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(qry)) {
            ps.setInt(1, postId);
            ps.setString(2, imagePath);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding image: " + e.getMessage());
        }
    }

    public void removeImage(int imageId) {
        String qry = "DELETE FROM imagepost WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(qry)) {
            ps.setInt(1, imageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing image: " + e.getMessage());
        }
    }

    public List<ImagePost> getImagesByPost(int postId) {
        List<ImagePost> list = new ArrayList<>();
        String qry = "SELECT * FROM imagepost WHERE post_id = ? ORDER BY created_at ASC";
        try (PreparedStatement ps = getConnection().prepareStatement(qry)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ImagePost img = new ImagePost(
                    rs.getInt("id"),
                    rs.getInt("post_id"),
                    rs.getString("image_path"),
                    rs.getTimestamp("created_at")
                );
                list.add(img);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching images: " + e.getMessage());
        }
        return list;
    }

    public void removeImagesByPost(int postId) {
        String qry = "DELETE FROM imagepost WHERE post_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(qry)) {
            ps.setInt(1, postId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing images by post: " + e.getMessage());
        }
    }

    public boolean hasMedia(int postId) {
        String qry = "SELECT COUNT(*) FROM imagepost WHERE post_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(qry)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking media: " + e.getMessage());
        }
        return false;
    }
}
