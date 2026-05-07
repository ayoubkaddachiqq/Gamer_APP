package tn.esprit.services;



import tn.esprit.entities.Comment;
import tn.esprit.utils.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceComment implements IService<Comment> {

    private Connection cnx;

    public ServiceComment() {
        cnx = MyDB.getInstance().getConnection();
    }

    @Override
    public int add(Comment c) {
        String qry = "INSERT INTO comments (post_id, user_id, comment_text) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(qry, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getPostId());
            ps.setInt(2, c.getUserId());
            ps.setString(3, c.getCommentText());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error adding comment: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public List<Comment> getAll() {
        List<Comment> list = new ArrayList<>();
        String qry = "SELECT * FROM comments";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(qry)) {
            while (rs.next()) {
                list.add(new Comment(
                        rs.getInt("id"),
                        rs.getInt("post_id"),
                        rs.getInt("user_id"),
                        rs.getString("comment_text"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public void update(Comment c) {
        String qry = "UPDATE comments SET comment_text = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setString(1, c.getCommentText());
            ps.setInt(2, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM        comments WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public Comment getById(int id) {
        String qry = "SELECT * FROM comments WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Comment(
                        rs.getInt("id"),
                        rs.getInt("post_id"),
                        rs.getInt("user_id"),
                        rs.getString("comment_text"),
                        rs.getTimestamp("created_at")
                );
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    // Specialized method for Team Hub: Fetch comments for a specific recruitment post
    public List<Comment> getCommentsByPost(int postId) {
        List<Comment> list = new ArrayList<>();
        String qry = "SELECT * FROM comments WHERE post_id = ? ORDER BY created_at ASC";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Comment(
                        rs.getInt("id"),
                        rs.getInt("post_id"),
                        rs.getInt("user_id"),
                        rs.getString("comment_text"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }
}