package tn.esprit.services;

import tn.esprit.entities.Post;
import tn.esprit.utils.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePost implements IService<Post> {

    private Connection cnx;

    public ServicePost() {
        // Getting the singleton connection from your MyDatabase class
        cnx = MyDB.getInstance().getConnection();
    }

    @Override
    public Post getById(int id) {
        String qry = "SELECT * FROM posts WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Post p = new Post();
                p.setId(rs.getInt("id"));
                p.setUserId(rs.getInt("user_id"));
                p.setContent(rs.getString("content"));
                p.setGameTag(rs.getString("game_tag"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                return p;
            }
        } catch (SQLException e) {
            System.err.println("Error finding post: " + e.getMessage());
        }
        return null; // Returns null if no post is found with that ID
    }

    @Override
    public void add(Post p) {
        // We don't include 'id' or 'created_at' as they are auto-generated
        String qry = "INSERT INTO posts (user_id, content, game_tag) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, p.getUserId());
            ps.setString(2, p.getContent());
            ps.setString(3, p.getGameTag());
            ps.executeUpdate();
            System.out.println("Recruitment post created!");
        } catch (SQLException e) {
            System.err.println("Error creating post: " + e.getMessage());
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> list = new ArrayList<>();
        String qry = "SELECT * FROM posts ORDER BY created_at DESC";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(qry)) {
            while (rs.next()) {
                Post p = new Post();
                p.setId(rs.getInt("id"));
                p.setUserId(rs.getInt("user_id"));
                p.setContent(rs.getString("content"));
                p.setGameTag(rs.getString("game_tag"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching posts: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void delete(int id) {
        String qry = "DELETE FROM posts WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void update(Post p) {
        String qry = "UPDATE posts SET content = ?, game_tag = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setString(1, p.getContent());
            ps.setString(2, p.getGameTag());
            ps.setInt(3, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}