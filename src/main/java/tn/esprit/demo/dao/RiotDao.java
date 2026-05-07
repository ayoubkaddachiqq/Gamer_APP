package tn.esprit.demo.dao;

import tn.esprit.demo.model.RiotAccount;

import javax.sql.DataSource;
import java.sql.*;

/**
 * DAO for the {@code riot_accounts} table.
 * The table is auto-created on construction (no manual schema migration needed).
 */
public class RiotDao {

    /*
     * DDL without a named FK constraint to avoid "Duplicate key name" errors
     * if MySQL already has a constraint called 'fk_riot_user' from a failed
     * prior migration attempt.  MySQL auto-generates the constraint name.
     */
    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS riot_accounts (
              id              INT            NOT NULL AUTO_INCREMENT,
              user_id         BIGINT         NOT NULL,
              game_name       VARCHAR(100)   NOT NULL,
              tag_line        VARCHAR(10)    NOT NULL,
              puuid           VARCHAR(100)   NULL,
              summoner_id     VARCHAR(100)   NULL,
              summoner_level  INT            NULL,
              profile_icon_id INT            NULL,
              tier            VARCHAR(20)    NULL,
              rank_division   VARCHAR(5)     NULL,
              league_points   INT            NULL,
              wins            INT            NULL,
              losses          INT            NULL,
              last_updated    TIMESTAMP      NOT NULL
                                DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,
              PRIMARY KEY (id),
              UNIQUE KEY uq_riot_user (user_id),
              FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
            )
            """;

    private final DataSource dataSource;

    public RiotDao(DataSource dataSource) {
        this.dataSource = dataSource;
        ensureTable();
    }

    // ── Schema bootstrap ───────────────────────────────────────────────────────

    /**
     * Creates the riot_accounts table if it does not already exist.
     * IF NOT EXISTS means this is a no-op after the first run.
     */
    private void ensureTable() {
        try (Connection con = dataSource.getConnection();
             Statement  st  = con.createStatement()) {
            st.execute(DDL);
            System.out.println("[RiotDao] riot_accounts table ready.");
        } catch (SQLException e) {
            System.err.println("[RiotDao] ensureTable FAILED: [" + e.getErrorCode()
                    + "] " + e.getMessage());
        }
        // Patch any columns that may be missing from an older table version
        ensureColumns();
    }

    /**
     * Adds any columns that may be absent because the table was created by an
     * older version of the schema.  MySQL error 1060 = "Duplicate column name"
     * (column already present) — that is silently ignored.
     */
    private void ensureColumns() {
        String[][] cols = {
            {"summoner_id",     "VARCHAR(100) NULL"},
            {"summoner_level",  "INT NULL"},
            {"profile_icon_id", "INT NULL"},
            {"tier",            "VARCHAR(20) NULL"},
            {"rank_division",   "VARCHAR(5)  NULL"},
            {"league_points",   "INT NULL"},
            {"wins",            "INT NULL"},
            {"losses",          "INT NULL"},
        };
        try (Connection con = dataSource.getConnection()) {
            for (String[] col : cols) {
                try (Statement st = con.createStatement()) {
                    st.execute("ALTER TABLE riot_accounts ADD COLUMN "
                            + col[0] + " " + col[1]);
                    System.out.println("[RiotDao] Added missing column: " + col[0]);
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1060) {
                        // Column already exists — expected, ignore silently
                    } else {
                        System.err.println("[RiotDao] ensureColumns warning for "
                                + col[0] + ": [" + e.getErrorCode() + "] " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[RiotDao] ensureColumns connection failed: " + e.getMessage());
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    /**
     * Returns the linked Riot account for the given user, or {@code null} if none.
     */
    public RiotAccount getRiotAccount(long userId) {
        String sql = "SELECT * FROM riot_accounts WHERE user_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (Exception e) {
            System.err.println("[RiotDao] getRiotAccount failed: " + e.getMessage());
        }
        return null;
    }

    // ── Write ──────────────────────────────────────────────────────────────────

    /**
     * Inserts or updates the Riot account for the given user (upsert).
     * Throws an {@link IllegalStateException} whose message contains the
     * underlying SQL error so the UI can display it directly.
     */
    public void saveRiotAccount(long userId, RiotAccount a) {
        String sql = """
            INSERT INTO riot_accounts
              (user_id, game_name, tag_line, puuid, summoner_id, summoner_level,
               profile_icon_id, tier, rank_division, league_points, wins, losses)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
              game_name       = VALUES(game_name),
              tag_line        = VALUES(tag_line),
              puuid           = VALUES(puuid),
              summoner_id     = VALUES(summoner_id),
              summoner_level  = VALUES(summoner_level),
              profile_icon_id = VALUES(profile_icon_id),
              tier            = VALUES(tier),
              rank_division   = VALUES(rank_division),
              league_points   = VALUES(league_points),
              wins            = VALUES(wins),
              losses          = VALUES(losses)
            """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, a.getGameName());
            ps.setString(3, a.getTagLine());
            ps.setString(4, a.getPuuid());
            ps.setString(5, a.getSummonerId() != null ? a.getSummonerId() : "");
            ps.setInt(6, a.getSummonerLevel());
            ps.setInt(7, a.getProfileIconId());
            ps.setString(8, a.getTier() != null ? a.getTier() : "");
            ps.setString(9, a.getRank() != null ? a.getRank() : "");
            ps.setInt(10, a.getLeaguePoints());
            ps.setInt(11, a.getWins());
            ps.setInt(12, a.getLosses());
            ps.executeUpdate();
            System.out.println("[RiotDao] saved riot account for userId=" + userId);
        } catch (SQLException e) {
            // Include the SQL error code + message so the UI can show it
            String detail = "[" + e.getErrorCode() + "] " + e.getMessage();
            System.err.println("[RiotDao] saveRiotAccount FAILED: " + detail);
            throw new IllegalStateException(detail, e);
        }
    }

    /**
     * Removes the Riot account link for a user.
     */
    public void deleteRiotAccount(long userId) {
        String sql = "DELETE FROM riot_accounts WHERE user_id = ?";
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("[" + e.getErrorCode() + "] " + e.getMessage(), e);
        }
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

    private RiotAccount mapRow(ResultSet rs) throws SQLException {
        RiotAccount a = new RiotAccount();
        a.setGameName(rs.getString("game_name"));
        a.setTagLine(rs.getString("tag_line"));
        a.setPuuid(rs.getString("puuid"));
        a.setSummonerId(rs.getString("summoner_id"));
        a.setSummonerLevel(rs.getInt("summoner_level"));
        a.setProfileIconId(rs.getInt("profile_icon_id"));
        a.setTier(rs.getString("tier"));
        a.setRank(rs.getString("rank_division"));
        a.setLeaguePoints(rs.getInt("league_points"));
        a.setWins(rs.getInt("wins"));
        a.setLosses(rs.getInt("losses"));
        return a;
    }
}
