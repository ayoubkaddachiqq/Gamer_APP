package tn.esprit.services;

import tn.esprit.entities.ImagePost;
import tn.esprit.entities.Post;
import tn.esprit.utils.MyDB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePost implements IService<Post> {

    private Connection cnx;

    public ServicePost() {
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
                p.setUsername(rs.getString("username"));
                p.setContent(rs.getString("content"));
                p.setGameTag(rs.getString("game_tag"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                return p;
            }
        } catch (SQLException e) {
            System.err.println("Error finding post: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void add(Post p) {
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
                p.setUsername(rs.getString("username"));
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

    public void loadImagesForPosts(List<Post> posts) {
        ServiceImagePost serviceImagePost = new ServiceImagePost();
        for (Post post : posts) {
            List<ImagePost> images = serviceImagePost.getImagesByPost(post.getId());
            for (ImagePost img : images) {
                post.addImagePath(img.getImagePath());
            }
        }
    }

    public List<Post> search(String query) {
        List<Post> list = new ArrayList<>();
        String qry = "SELECT * FROM posts WHERE content LIKE ? OR game_tag LIKE ? OR username LIKE ? ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            String searchPattern = "%" + query + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Post p = new Post();
                p.setId(rs.getInt("id"));
                p.setUserId(rs.getInt("user_id"));
                p.setUsername(rs.getString("username"));
                p.setContent(rs.getString("content"));
                p.setGameTag(rs.getString("game_tag"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error searching posts: " + e.getMessage());
        }
        return list;
    }

    public List<Post> searchCombined(String query, String gameTag, String timeFilter, String typeFilter) {
        List<Post> list = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        List<String> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            if ("Content".equals(typeFilter) || "All".equals(typeFilter)) {
                conditions.add("(content LIKE ? OR username LIKE ?)");
                params.add("%" + query.trim() + "%");
                params.add("%" + query.trim() + "%");
            }
            if ("Users".equals(typeFilter)) {
                conditions.add("username LIKE ?");
                params.add("%" + query.trim() + "%");
            }
        }

        if (gameTag != null && !"All".equals(gameTag) && !gameTag.isEmpty()) {
            conditions.add("game_tag = ?");
            params.add(gameTag);
        }

        if ("Last Hour".equals(timeFilter)) {
            conditions.add("created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)");
        } else if ("Today".equals(timeFilter)) {
            conditions.add("created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)");
        } else if ("This Week".equals(timeFilter)) {
            conditions.add("created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
        }

        String baseQry = "SELECT * FROM posts";
        if (!conditions.isEmpty()) {
            baseQry += " WHERE " + String.join(" AND ", conditions);
        }
        baseQry += " ORDER BY created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(baseQry)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Post p = new Post();
                p.setId(rs.getInt("id"));
                p.setUserId(rs.getInt("user_id"));
                p.setUsername(rs.getString("username"));
                p.setContent(rs.getString("content"));
                p.setGameTag(rs.getString("game_tag"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error searching posts combined: " + e.getMessage());
        }
        return list;
    }

    public List<Post> getPostsByGameTag(String gameTag) {
        List<Post> list = new ArrayList<>();
        String qry = "SELECT * FROM posts WHERE game_tag = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setString(1, gameTag);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Post p = new Post();
                p.setId(rs.getInt("id"));
                p.setUserId(rs.getInt("user_id"));
                p.setUsername(rs.getString("username"));
                p.setContent(rs.getString("content"));
                p.setGameTag(rs.getString("game_tag"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching posts by game tag: " + e.getMessage());
        }
        return list;
    }

    public List<Post> getPostsByUser(int userId) {
        List<Post> list = new ArrayList<>();
        String qry = "SELECT * FROM posts WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = cnx.prepareStatement(qry)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Post p = new Post();
                p.setId(rs.getInt("id"));
                p.setUserId(rs.getInt("user_id"));
                p.setUsername(rs.getString("username"));
                p.setContent(rs.getString("content"));
                p.setGameTag(rs.getString("game_tag"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching posts by user: " + e.getMessage());
        }
        return list;
    }

    @Override
    public void delete(int id) {
        ServiceImagePost serviceImagePost = new ServiceImagePost();
        serviceImagePost.removeImagesByPost(id);
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
