package tn.esprit.services;

import tn.esprit.entities.Share;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceShare {

    public ServiceShare() {
    }

    private Connection getConnection() {
        return MyDB.getInstance().getConnection();
    }

    public void addShare(Share share) {
        String qry = "INSERT INTO shares (original_post_id, user_id, share_text) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(qry)) {
            ps.setInt(1, share.getOriginalPostId());
            ps.setInt(2, share.getUserId());
            ps.setString(3, share.getShareText());
            ps.executeUpdate();
            System.out.println("Post shared successfully!");
        } catch (SQLException e) {
            System.err.println("Error sharing post: " + e.getMessage());
        }
    }

    public int getShareCount(int postId) {
        String qry = "SELECT COUNT(*) FROM shares WHERE original_post_id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(qry)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting share count: " + e.getMessage());
        }
        return 0;
    }

    public List<Share> getSharesByPost(int postId) {
        List<Share> list = new ArrayList<>();
        String qry = "SELECT * FROM shares WHERE original_post_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = getConnection().prepareStatement(qry)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Share share = new Share(
                    rs.getInt("id"),
                    rs.getInt("original_post_id"),
                    rs.getInt("user_id"),
                    rs.getString("share_text"),
                    rs.getTimestamp("created_at")
                );
                list.add(share);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching shares: " + e.getMessage());
        }
        return list;
    }
}
