package fr.mc.semirp.economy.database;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyDatabase {

    private final Connection connection;
    private final Logger logger;

    public EconomyDatabase(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS balances (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    balance REAL NOT NULL DEFAULT 0.0
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL DEFAULT (datetime('now')),
                    from_uuid TEXT,
                    to_uuid TEXT,
                    amount REAL NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT
                )
            """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_from ON transactions (from_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_to ON transactions (to_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions (timestamp)");

            logger.info("Tables économie créées.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la création des tables économie", e);
        }
    }

    public boolean playerExists(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM balances WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            return ps.executeQuery().next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (playerExists)", e);
            return false;
        }
    }

    public void createPlayer(UUID uuid, String name, double startingBalance) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO balances (uuid, name, balance) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setDouble(3, startingBalance);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (createPlayer)", e);
        }
    }

    public double getBalance(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT balance FROM balances WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (getBalance)", e);
        }
        return 0.0;
    }

    public boolean setBalance(UUID uuid, double amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE balances SET balance = ? WHERE uuid = ?")) {
            ps.setDouble(1, amount);
            ps.setString(2, uuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (setBalance)", e);
            return false;
        }
    }

    public void updatePlayerName(UUID uuid, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE balances SET name = ? WHERE uuid = ?")) {
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (updatePlayerName)", e);
        }
    }

    public List<Map.Entry<String, Double>> getTopBalances(int limit) {
        List<Map.Entry<String, Double>> top = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name, balance FROM balances ORDER BY balance DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                top.add(Map.entry(rs.getString("name"), rs.getDouble("balance")));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (getTopBalances)", e);
        }
        return top;
    }

    public void logTransaction(UUID from, UUID to, double amount, String type, String description) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO transactions (from_uuid, to_uuid, amount, type, description) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, from != null ? from.toString() : null);
            ps.setString(2, to != null ? to.toString() : null);
            ps.setDouble(3, amount);
            ps.setString(4, type);
            ps.setString(5, description);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (logTransaction)", e);
        }
    }
}