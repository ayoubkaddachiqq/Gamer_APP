package tn.esprit.services;

import tn.esprit.entities.ImagePost;
import tn.esprit.entities.Post;
import tn.esprit.entities.UserRanking;
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
    public int add(Post p) {
        String qry = "INSERT INTO posts (user_id, username, content, game_tag) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(qry, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getUserId());
            ps.setString(2, p.getUsername() != null ? p.getUsername() : "");
            ps.setString(3, p.getContent());
            ps.setString(4, p.getGameTag());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("Post created with ID: " + id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("Error creating post: " + e.getMessage());
        }
        return -1;
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

    public List<Post> getTrendingPosts() {
        List<Post> posts = getAll();
        ServiceLike serviceLike = new ServiceLike();
        ServiceShare serviceShare = new ServiceShare();
        ServiceComment serviceComment = new ServiceComment();

        double now = System.currentTimeMillis();
        for (Post post : posts) {
            int likes = serviceLike.getLikeCount(post.getId());
            int comments = serviceComment.getCommentsByPost(post.getId()).size();
            int shares = serviceShare.getShareCount(post.getId());
            double hoursAgo = (now - post.getCreatedAt().getTime()) / (1000.0 * 60 * 60);
            if (hoursAgo < 1) hoursAgo = 1;
            double score = ((likes * 2) + (comments * 3) + (shares * 4)) / Math.pow(hoursAgo, 1.5);
            post.setTrendingScore(score);
        }
        posts.sort((a, b) -> Double.compare(b.getTrendingScore(), a.getTrendingScore()));
        return posts;
    }

    public List<UserRanking> getUserLeaderboard() {
        String qry = "SELECT user_id, username FROM posts GROUP BY user_id, username";
        List<UserRanking> rankings = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(qry)) {
            ServiceLike serviceLike = new ServiceLike();
            ServiceComment serviceComment = new ServiceComment();
            ServiceShare serviceShare = new ServiceShare();
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String username = rs.getString("username");
                if (username == null || username.isEmpty()) {
                    username = "Player " + userId;
                }
                List<Post> userPosts = getPostsByUser(userId);
                int totalPosts = userPosts.size();
                int totalLikes = 0, totalComments = 0, totalShares = 0;
                for (Post p : userPosts) {
                    totalLikes += serviceLike.getLikeCount(p.getId());
                    totalComments += serviceComment.getCommentsByPost(p.getId()).size();
                    totalShares += serviceShare.getShareCount(p.getId());
                }
                int score = (totalPosts * 5) + (totalLikes * 2) + (totalComments * 1) + (totalShares * 3);
                String title;
                if (score < 100) title = "Rookie";
                else if (score < 500) title = "Veteran";
                else if (score < 1000) title = "Elite";
                else title = "Legend";
                rankings.add(new UserRanking(userId, username, score, totalPosts, totalLikes, title));
            }
        } catch (SQLException e) {
            System.err.println("Error calculating leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
        rankings.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return rankings;
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
