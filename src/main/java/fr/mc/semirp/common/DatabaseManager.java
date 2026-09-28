package fr.mc.semirp.common;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            plugin.getDataFolder().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("Connexion à la base de données établie.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de se connecter à la base de données", e);
        }
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Connexion à la base de données fermée.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur lors de la fermeture de la base de données", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }
}