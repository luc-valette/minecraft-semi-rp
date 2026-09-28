package fr.mc.semirp.tp.database;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TpDatabase {

    private final Connection connection;
    private final Logger logger;

    public TpDatabase(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS homes (
                    uuid TEXT NOT NULL,
                    name TEXT NOT NULL,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    PRIMARY KEY (uuid, name)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS warps (
                    name TEXT PRIMARY KEY,
                    world TEXT NOT NULL,
                    x REAL NOT NULL,
                    y REAL NOT NULL,
                    z REAL NOT NULL,
                    yaw REAL NOT NULL,
                    pitch REAL NOT NULL,
                    created_by TEXT NOT NULL
                )
            """);

            logger.info("Tables TP créées.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la création des tables TP", e);
        }
    }

    // === Homes ===

    public boolean homeExists(UUID uuid, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM homes WHERE uuid = ? AND name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (homeExists)", e);
            return false;
        }
    }

    public int countHomes(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM homes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (countHomes)", e);
        }
        return 0;
    }

    public void setHome(UUID uuid, String name, String world, double x, double y, double z, float yaw, float pitch) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, world);
            ps.setDouble(4, x);
            ps.setDouble(5, y);
            ps.setDouble(6, z);
            ps.setFloat(7, yaw);
            ps.setFloat(8, pitch);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (setHome)", e);
        }
    }

    public Map<String, Object> getHome(UUID uuid, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT world, x, y, z, yaw, pitch FROM homes WHERE uuid = ? AND name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> home = new HashMap<>();
                home.put("world", rs.getString("world"));
                home.put("x", rs.getDouble("x"));
                home.put("y", rs.getDouble("y"));
                home.put("z", rs.getDouble("z"));
                home.put("yaw", rs.getFloat("yaw"));
                home.put("pitch", rs.getFloat("pitch"));
                return home;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (getHome)", e);
        }
        return null;
    }

    public boolean deleteHome(UUID uuid, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM homes WHERE uuid = ? AND name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (deleteHome)", e);
            return false;
        }
    }

    public List<String> listHomes(UUID uuid) {
        List<String> homes = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM homes WHERE uuid = ? ORDER BY name")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                homes.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (listHomes)", e);
        }
        return homes;
    }

    // === Warps ===

    public boolean warpExists(String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM warps WHERE name = ?")) {
            ps.setString(1, name);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (warpExists)", e);
            return false;
        }
    }

    public void setWarp(String name, String world, double x, double y, double z, float yaw, float pitch, String createdBy) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO warps (name, world, x, y, z, yaw, pitch, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, name);
            ps.setString(2, world);
            ps.setDouble(3, x);
            ps.setDouble(4, y);
            ps.setDouble(5, z);
            ps.setFloat(6, yaw);
            ps.setFloat(7, pitch);
            ps.setString(8, createdBy);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (setWarp)", e);
        }
    }

    public Map<String, Object> getWarp(String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT world, x, y, z, yaw, pitch FROM warps WHERE name = ?")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> warp = new HashMap<>();
                warp.put("world", rs.getString("world"));
                warp.put("x", rs.getDouble("x"));
                warp.put("y", rs.getDouble("y"));
                warp.put("z", rs.getDouble("z"));
                warp.put("yaw", rs.getFloat("yaw"));
                warp.put("pitch", rs.getFloat("pitch"));
                return warp;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (getWarp)", e);
        }
        return null;
    }

    public boolean deleteWarp(String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM warps WHERE name = ?")) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (deleteWarp)", e);
            return false;
        }
    }

    public List<String> listWarps() {
        List<String> warps = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM warps ORDER BY name")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                warps.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (listWarps)", e);
        }
        return warps;
    }
}