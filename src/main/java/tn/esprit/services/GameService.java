package tn.esprit.services;

import tn.esprit.entities.UserGame;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GameService {

    public List<UserGame> getUserGames(int userId) {
        List<UserGame> games = new ArrayList<>();
        String sql = "SELECT * FROM user_games WHERE user_id = ? ORDER BY added_at DESC";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    games.add(mapGame(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching games: " + e.getMessage());
        }
        return games;
    }

    public void addGame(UserGame game) {
        String sql = "INSERT INTO user_games (user_id, rawg_id, game_name, cover_url, genre, metacritic_score, is_favorite) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, game.getUserId());
            ps.setInt(2, game.getRawgId());
            ps.setString(3, game.getGameName());
            ps.setString(4, game.getCoverUrl());
            ps.setString(5, game.getGenre());
            ps.setInt(6, game.getMetacriticScore());
            ps.setBoolean(7, game.isFavorite());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding game: " + e.getMessage());
        }
    }

    public void removeGame(int gameId) {
        String sql = "DELETE FROM user_games WHERE id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing game: " + e.getMessage());
        }
    }

    public void toggleFavorite(int gameId) {
        String sql = "UPDATE user_games SET is_favorite = NOT is_favorite WHERE id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, gameId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error toggling favorite: " + e.getMessage());
        }
    }

    public int getGameCount(int userId) {
        String sql = "SELECT COUNT(*) FROM user_games WHERE user_id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting games: " + e.getMessage());
        }
        return 0;
    }

    private UserGame mapGame(ResultSet rs) throws SQLException {
        UserGame game = new UserGame();
        game.setId(rs.getInt("id"));
        game.setUserId(rs.getInt("user_id"));
        game.setRawgId(rs.getInt("rawg_id"));
        game.setGameName(rs.getString("game_name"));
        game.setCoverUrl(rs.getString("cover_url"));
        game.setGenre(rs.getString("genre"));
        game.setMetacriticScore(rs.getInt("metacritic_score"));
        game.setFavorite(rs.getBoolean("is_favorite"));
        Timestamp ts = rs.getTimestamp("added_at");
        if (ts != null) game.setAddedAt(ts.toInstant());
        return game;
    }
}
