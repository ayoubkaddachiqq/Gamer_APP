package tn.esprit.demo.dao;

import tn.esprit.demo.model.UserGame;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameDao {

    private final DataSource ds;

    public GameDao(DataSource ds) {
        this.ds = ds;
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    public void addGame(long userId, UserGame game) {
        String sql = "INSERT INTO user_games " +
                "(user_id, rawg_id, game_name, cover_url, genre, metacritic_score, is_favorite) " +
                "VALUES (?,?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE game_name=VALUES(game_name), " +
                "cover_url=VALUES(cover_url), genre=VALUES(genre), " +
                "metacritic_score=VALUES(metacritic_score)";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, game.getRawgId());
            ps.setString(3, game.getGameName());
            ps.setString(4, game.getCoverUrl());
            ps.setString(5, game.getGenre());
            ps.setInt(6, game.getMetacriticScore());
            ps.setBoolean(7, game.isFavorite());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add game", e);
        }
    }

    public void removeGame(long userId, int rawgId) {
        String sql = "DELETE FROM user_games WHERE user_id=? AND rawg_id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, rawgId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove game", e);
        }
    }

    public void setFavorite(long userId, int rawgId, boolean isFavorite) {
        String sql = "UPDATE user_games SET is_favorite=? WHERE user_id=? AND rawg_id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, isFavorite);
            ps.setLong(2, userId);
            ps.setInt(3, rawgId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update favorite", e);
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<UserGame> getUserGames(long userId) {
        String sql = "SELECT * FROM user_games WHERE user_id=? ORDER BY added_at DESC";
        return query(sql, userId);
    }

    public List<UserGame> getFavoriteGames(long userId) {
        String sql = "SELECT * FROM user_games WHERE user_id=? AND is_favorite=TRUE " +
                     "ORDER BY added_at DESC LIMIT 4";
        return query(sql, userId);
    }

    public boolean isGameSaved(long userId, int rawgId) {
        String sql = "SELECT COUNT(*) FROM user_games WHERE user_id=? AND rawg_id=?";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, rawgId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to check game saved", e);
        }
    }

    public int countFavorites(long userId) {
        String sql = "SELECT COUNT(*) FROM user_games WHERE user_id=? AND is_favorite=TRUE";
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count favorites", e);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private List<UserGame> query(String sql, long userId) {
        List<UserGame> list = new ArrayList<>();
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query games", e);
        }
        return list;
    }

    private UserGame mapRow(ResultSet rs) throws SQLException {
        UserGame g = new UserGame();
        g.setId(rs.getInt("id"));
        g.setUserId(rs.getLong("user_id"));
        g.setRawgId(rs.getInt("rawg_id"));
        g.setGameName(rs.getString("game_name"));
        g.setCoverUrl(rs.getString("cover_url"));
        g.setGenre(rs.getString("genre"));
        g.setMetacriticScore(rs.getInt("metacritic_score"));
        g.setFavorite(rs.getBoolean("is_favorite"));
        Timestamp ts = rs.getTimestamp("added_at");
        if (ts != null) g.setAddedAt(ts.toInstant());
        return g;
    }
}
