package tn.esprit.services;

import tn.esprit.entities.Like;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceLike {

    private Connection cnx;

    public ServiceLike() {
        cnx = MyDB.getInstance().getConnection();
    }

    public void addLike(int postId, int userId) {
        String qry = "INSERT INTO likes (post_id, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                System.out.println("User already liked this post.");
            } else {
                System.err.println("Error adding like: " + e.getMessage());
            }
        }
    }

    public void removeLike(int postId, int userId) {
        String qry = "DELETE FROM likes WHERE post_id = ? AND user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing like: " + e.getMessage());
        }
    }

    public boolean hasUserLiked(int postId, int userId) {
        String qry = "SELECT COUNT(*) FROM likes WHERE post_id = ? AND user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking like: " + e.getMessage());
        }
        return false;
    }

    public int getLikeCount(int postId) {
        String qry = "SELECT COUNT(*) FROM likes WHERE post_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting like count: " + e.getMessage());
        }
        return 0;
    }

    public List<Like> getLikesByPost(int postId) {
        List<Like> list = new ArrayList<>();
        String qry = "SELECT * FROM likes WHERE post_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Like like = new Like(
                    rs.getInt("id"),
                    rs.getInt("post_id"),
                    rs.getInt("user_id"),
                    rs.getTimestamp("created_at")
                );
                list.add(like);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching likes: " + e.getMessage());
        }
        return list;
    }

    public void toggleLike(int postId, int userId) {
        if (hasUserLiked(postId, userId)) {
            removeLike(postId, userId);
        } else {
            addLike(postId, userId);
        }
    }
}
