package fr.mc.semirp;

import fr.mc.semirp.common.DatabaseManager;
import fr.mc.semirp.economy.EconomyModule;
import fr.mc.semirp.tp.TpModule;
import org.bukkit.plugin.java.JavaPlugin;

public class MainPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private EconomyModule economyModule;
    private TpModule tpModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        if (getConfig().getBoolean("modules.economy", false)) {
            economyModule = new EconomyModule(this, databaseManager);
            economyModule.initialize();
        }

        if (getConfig().getBoolean("modules.tp", false)) {
            tpModule = new TpModule(this, databaseManager);
            tpModule.initialize();
        }

        getLogger().info("SemiRP activé.");
    }

    @Override
    public void onDisable() {
        if (tpModule != null) {
            tpModule.shutdown();
        }
        if (economyModule != null) {
            economyModule.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("SemiRP désactivé.");
    }

    public EconomyModule getEconomyModule() {
        return economyModule;
    }

    public TpModule getTpModule() {
        return tpModule;
    }
}