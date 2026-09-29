package fr.mc.semirp;

import fr.mc.semirp.common.DatabaseManager;
import fr.mc.semirp.economy.EconomyModule;
import fr.mc.semirp.jobs.JobsModule;
import fr.mc.semirp.shop.ShopModule;
import fr.mc.semirp.tp.TpModule;
import org.bukkit.plugin.java.JavaPlugin;

public class MainPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private EconomyModule economyModule;
    private TpModule tpModule;
    private ShopModule shopModule;
    private JobsModule jobsModule;

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

        // Shop et Métiers dépendent de l'Économie : ils ne démarrent pas sans elle
        if (getConfig().getBoolean("modules.shop", false)) {
            if (economyModule != null) {
                shopModule = new ShopModule(this, databaseManager, economyModule.getEconomyManager());
                shopModule.initialize();
            } else {
                getLogger().severe("Module Shop non chargé : il nécessite le module Économie (modules.economy: true).");
            }
        }

        if (getConfig().getBoolean("modules.jobs", false)) {
            if (economyModule != null) {
                jobsModule = new JobsModule(this, databaseManager, economyModule.getEconomyManager());
                jobsModule.initialize();
            } else {
                getLogger().severe("Module Métiers non chargé : il nécessite le module Économie (modules.economy: true).");
            }
        }

        getLogger().info("SemiRP activé.");
    }

    @Override
    public void onDisable() {
        // Ordre inverse du démarrage : Métiers verse ses gains avant que l'Économie s'arrête
        if (jobsModule != null) {
            jobsModule.shutdown();
        }
        if (shopModule != null) {
            shopModule.shutdown();
        }
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

    public ShopModule getShopModule() {
        return shopModule;
    }

    public JobsModule getJobsModule() {
        return jobsModule;
    }
}
