package fr.mc.semirp.economy;

import fr.mc.semirp.common.DatabaseManager;
import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.economy.commands.BalanceCommand;
import fr.mc.semirp.economy.commands.BalTopCommand;
import fr.mc.semirp.economy.commands.EcoAdminCommand;
import fr.mc.semirp.economy.commands.PayCommand;
import fr.mc.semirp.economy.database.EconomyDatabase;
import fr.mc.semirp.economy.listeners.PlayerJoinListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class EconomyModule {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private MessageHelper messageHelper;

    public EconomyModule(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void initialize() {
        // Charger economy.yml
        plugin.saveResource("economy.yml", false);
        File configFile = new File(plugin.getDataFolder(), "economy.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Créer les composants
        EconomyDatabase database = new EconomyDatabase(databaseManager.getConnection(), plugin.getLogger());
        database.createTables();

        economyManager = new EconomyManager(database, config);
        messageHelper = new MessageHelper(config);

        // Enregistrer les listeners
        plugin.getServer().getPluginManager().registerEvents(new PlayerJoinListener(economyManager), plugin);

        // Enregistrer les commandes
        plugin.getCommand("balance").setExecutor(new BalanceCommand(economyManager, messageHelper));
        plugin.getCommand("pay").setExecutor(new PayCommand(economyManager, messageHelper));
        plugin.getCommand("baltop").setExecutor(new BalTopCommand(economyManager, messageHelper));
        plugin.getCommand("eco").setExecutor(new EcoAdminCommand(economyManager, messageHelper));

        plugin.getLogger().info("Module Économie activé.");
    }

    public void shutdown() {
        plugin.getLogger().info("Module Économie désactivé.");
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}