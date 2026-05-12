package tn.esprit.services;

import tn.esprit.entities.RiotAccount;
import tn.esprit.utils.MyDB;

import java.sql.*;

public class RiotService {

    public RiotAccount getRiotAccount(int userId) {
        String sql = "SELECT * FROM riot_accounts WHERE user_id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRiotAccount(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching Riot account: " + e.getMessage());
        }
        return null;
    }

    public void saveRiotAccount(RiotAccount acc) {
        String sql = "INSERT INTO riot_accounts (user_id, game_name, tag_line, puuid, summoner_id, summoner_level, profile_icon_id, tier, rank_division, league_points, wins, losses) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE "
                   + "game_name = VALUES(game_name), tag_line = VALUES(tag_line), "
                   + "puuid = VALUES(puuid), summoner_id = VALUES(summoner_id), "
                   + "summoner_level = VALUES(summoner_level), profile_icon_id = VALUES(profile_icon_id), "
                   + "tier = VALUES(tier), rank_division = VALUES(rank_division), "
                   + "league_points = VALUES(league_points), wins = VALUES(wins), losses = VALUES(losses)";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, acc.getUserId());
            ps.setString(2, acc.getGameName());
            ps.setString(3, acc.getTagLine());
            ps.setString(4, acc.getPuuid());
            ps.setString(5, acc.getSummonerId());
            ps.setInt(6, acc.getSummonerLevel());
            ps.setInt(7, acc.getProfileIconId());
            ps.setString(8, acc.getTier());
            ps.setString(9, acc.getRankDivision());
            ps.setInt(10, acc.getLeaguePoints());
            ps.setInt(11, acc.getWins());
            ps.setInt(12, acc.getLosses());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving Riot account: " + e.getMessage());
        }
    }

    public void deleteRiotAccount(int userId) {
        String sql = "DELETE FROM riot_accounts WHERE user_id = ?";
        try (PreparedStatement ps = MyDB.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting Riot account: " + e.getMessage());
        }
    }

    private RiotAccount mapRiotAccount(ResultSet rs) throws SQLException {
        RiotAccount acc = new RiotAccount();
        acc.setId(rs.getInt("id"));
        acc.setUserId(rs.getInt("user_id"));
        acc.setGameName(rs.getString("game_name"));
        acc.setTagLine(rs.getString("tag_line"));
        acc.setPuuid(rs.getString("puuid"));
        acc.setSummonerId(rs.getString("summoner_id"));
        acc.setSummonerLevel(rs.getInt("summoner_level"));
        acc.setProfileIconId(rs.getInt("profile_icon_id"));
        acc.setTier(rs.getString("tier"));
        acc.setRankDivision(rs.getString("rank_division"));
        acc.setLeaguePoints(rs.getInt("league_points"));
        acc.setWins(rs.getInt("wins"));
        acc.setLosses(rs.getInt("losses"));
        Timestamp ts = rs.getTimestamp("last_updated_at");
        if (ts != null) acc.setLastUpdatedAt(ts.toInstant());
        return acc;
    }
}
