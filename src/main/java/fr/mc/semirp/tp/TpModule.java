package fr.mc.semirp.tp;

import fr.mc.semirp.common.DatabaseManager;
import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.commands.backs.BackCommand;
import fr.mc.semirp.tp.commands.homes.DelHomeCommand;
import fr.mc.semirp.tp.commands.homes.HomeCommand;
import fr.mc.semirp.tp.commands.homes.HomesCommand;
import fr.mc.semirp.tp.commands.homes.SetHomeCommand;
import fr.mc.semirp.tp.commands.tpa.TpaCommand;
import fr.mc.semirp.tp.commands.tpa.TpAcceptCommand;
import fr.mc.semirp.tp.commands.tpa.TpDenyCommand;
import fr.mc.semirp.tp.commands.warps.DelWarpCommand;
import fr.mc.semirp.tp.commands.warps.SetWarpCommand;
import fr.mc.semirp.tp.commands.warps.WarpCommand;
import fr.mc.semirp.tp.commands.warps.WarpsCommand;
import fr.mc.semirp.tp.database.TpDatabase;
import fr.mc.semirp.tp.listeners.TpListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TpModule {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private TpManager tpManager;

    public TpModule(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void initialize() {
        // Charger tp.yml
        plugin.saveResource("tp.yml", false);
        File configFile = new File(plugin.getDataFolder(), "tp.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Créer les composants
        TpDatabase database = new TpDatabase(databaseManager.getConnection(), plugin.getLogger());
        database.createTables();

        tpManager = new TpManager(database, config);
        MessageHelper messageHelper = new MessageHelper(config);

        // Enregistrer les listeners
        plugin.getServer().getPluginManager().registerEvents(new TpListener(tpManager), plugin);

        // Commandes homes
        plugin.getCommand("sethome").setExecutor(new SetHomeCommand(tpManager, messageHelper));
        plugin.getCommand("home").setExecutor(new HomeCommand(tpManager, messageHelper));
        plugin.getCommand("delhome").setExecutor(new DelHomeCommand(tpManager, messageHelper));
        plugin.getCommand("homes").setExecutor(new HomesCommand(tpManager, messageHelper));

        // Commandes TPA
        plugin.getCommand("tpa").setExecutor(new TpaCommand(tpManager, messageHelper));
        plugin.getCommand("tpaccept").setExecutor(new TpAcceptCommand(tpManager, messageHelper));
        plugin.getCommand("tpdeny").setExecutor(new TpDenyCommand(tpManager, messageHelper));

        // Commandes warps
        plugin.getCommand("setwarp").setExecutor(new SetWarpCommand(tpManager, messageHelper));
        plugin.getCommand("warp").setExecutor(new WarpCommand(tpManager, messageHelper));
        plugin.getCommand("delwarp").setExecutor(new DelWarpCommand(tpManager, messageHelper));
        plugin.getCommand("warps").setExecutor(new WarpsCommand(tpManager, messageHelper));

        // Commande back
        plugin.getCommand("back").setExecutor(new BackCommand(tpManager, messageHelper));

        plugin.getLogger().info("Module TP activé.");
    }

    public void shutdown() {
        plugin.getLogger().info("Module TP désactivé.");
    }

    public TpManager getTpManager() {
        return tpManager;
    }
}