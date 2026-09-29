package fr.mc.semirp.jobs.database;

import fr.mc.semirp.jobs.model.PlayerJob;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JobsDatabase {

    /** Une ligne du classement /jobs top. */
    public record TopEntry(String playerName, String jobId, int level, double xp) {
    }

    private final Connection connection;
    private final Logger logger;

    public JobsDatabase(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_jobs (
                    uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    job TEXT NOT NULL,
                    level INTEGER NOT NULL DEFAULT 1,
                    xp REAL NOT NULL DEFAULT 0.0,
                    active INTEGER NOT NULL DEFAULT 1,
                    joined_at TEXT NOT NULL DEFAULT (datetime('now')),
                    PRIMARY KEY (uuid, job)
                )
            """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player_jobs_ranking ON player_jobs (job, level, xp)");

            logger.info("Tables métiers créées.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la création des tables métiers", e);
        }
    }

    /** Tous les métiers d'un joueur, actifs et quittés. */
    public List<PlayerJob> loadPlayer(UUID uuid) {
        List<PlayerJob> jobs = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT job, level, xp, active FROM player_jobs WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                jobs.add(new PlayerJob(
                        rs.getString("job"),
                        rs.getInt("level"),
                        rs.getDouble("xp"),
                        rs.getInt("active") == 1));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (loadPlayer)", e);
        }
        return jobs;
    }

    /** Insère ou met à jour la progression d'un joueur dans un métier. */
    public boolean save(UUID uuid, String name, PlayerJob job) {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO player_jobs (uuid, name, job, level, xp, active) VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (uuid, job) DO UPDATE SET
                    name = excluded.name,
                    level = excluded.level,
                    xp = excluded.xp,
                    active = excluded.active
            """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, job.getJobId());
            ps.setInt(4, job.getLevel());
            ps.setDouble(5, job.getXp());
            ps.setInt(6, job.isActive() ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (save player_job)", e);
            return false;
        }
    }

    /**
     * Classement des métiers actifs.
     *
     * @param jobId métier ciblé, ou null pour tous les métiers confondus
     */
    public List<TopEntry> getTop(String jobId, int limit) {
        List<TopEntry> top = new ArrayList<>();
        String sql = jobId == null
                ? "SELECT name, job, level, xp FROM player_jobs WHERE active = 1 ORDER BY level DESC, xp DESC LIMIT ?"
                : "SELECT name, job, level, xp FROM player_jobs WHERE active = 1 AND job = ? ORDER BY level DESC, xp DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            if (jobId != null) {
                ps.setString(index++, jobId);
            }
            ps.setInt(index, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                top.add(new TopEntry(rs.getString("name"), rs.getString("job"), rs.getInt("level"), rs.getDouble("xp")));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (getTop)", e);
        }
        return top;
    }
}
