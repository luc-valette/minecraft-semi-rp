package fr.mc.semirp.shop;

import fr.mc.semirp.common.DatabaseManager;
import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.economy.EconomyManager;
import fr.mc.semirp.shop.commands.ShopCommand;
import fr.mc.semirp.shop.database.ShopDatabase;
import fr.mc.semirp.shop.gui.ShopGui;
import fr.mc.semirp.shop.listeners.ShopGuiListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ShopModule {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    private ShopManager shopManager;

    public ShopModule(JavaPlugin plugin, DatabaseManager databaseManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economyManager = economyManager;
    }

    public void initialize() {
        // Charger shop.yml
        plugin.saveResource("shop.yml", false);
        File configFile = new File(plugin.getDataFolder(), "shop.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Créer les composants
        ShopDatabase database = new ShopDatabase(databaseManager.getConnection(), plugin.getLogger());
        database.createTables();

        shopManager = new ShopManager(database, economyManager, config);
        MessageHelper messageHelper = new MessageHelper(config);
        ShopFeedback feedback = new ShopFeedback(messageHelper, shopManager);
        ShopGui gui = new ShopGui(shopManager, config);

        // Enregistrer les listeners
        plugin.getServer().getPluginManager().registerEvents(
                new ShopGuiListener(plugin, shopManager, gui, feedback), plugin);

        // Enregistrer les commandes
        ShopCommand shopCommand = new ShopCommand(shopManager, gui, feedback, messageHelper);
        plugin.getCommand("shop").setExecutor(shopCommand);
        plugin.getCommand("shop").setTabCompleter(shopCommand);

        plugin.getLogger().info("Module Shop activé.");
    }

    public void shutdown() {
        plugin.getLogger().info("Module Shop désactivé.");
    }

    public ShopManager getShopManager() {
        return shopManager;
    }
}
