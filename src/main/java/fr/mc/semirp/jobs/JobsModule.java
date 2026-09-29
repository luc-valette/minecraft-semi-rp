package fr.mc.semirp.jobs;

import fr.mc.semirp.common.DatabaseManager;
import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.economy.EconomyManager;
import fr.mc.semirp.jobs.commands.JobsCommand;
import fr.mc.semirp.jobs.database.JobsDatabase;
import fr.mc.semirp.jobs.listeners.JobActionListener;
import fr.mc.semirp.jobs.listeners.JobsSessionListener;
import fr.mc.semirp.jobs.loader.JobLoader;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class JobsModule {

    private static final long TICKS_PER_SECOND = 20L;

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final EconomyManager economyManager;
    private JobsManager jobsManager;
    private YamlConfiguration config;
    private File configFile;
    private BukkitTask payoutTask;
    private BukkitTask saveTask;

    public JobsModule(JavaPlugin plugin, DatabaseManager databaseManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.economyManager = economyManager;
    }

    public void initialize() {
        // Charger jobs.yml
        plugin.saveResource("jobs.yml", false);
        configFile = new File(plugin.getDataFolder(), "jobs.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // Copier les tables JSON par défaut (jamais écrasées si elles existent déjà)
        JobLoader loader = new JobLoader(plugin);
        loader.saveDefaults();

        // Créer les composants
        JobsDatabase database = new JobsDatabase(databaseManager.getConnection(), plugin.getLogger());
        database.createTables();

        jobsManager = new JobsManager(database, economyManager, loader, config);
        MessageHelper messageHelper = new MessageHelper(config);

        // Joueurs déjà connectés (cas d'un rechargement du plugin)
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            jobsManager.loadPlayer(player.getUniqueId(), player.getName());
        }

        // Enregistrer les listeners
        boolean showActionBar = config.getBoolean("show-action-bar", true);
        plugin.getServer().getPluginManager().registerEvents(
                new JobActionListener(jobsManager, messageHelper, showActionBar), plugin);
        plugin.getServer().getPluginManager().registerEvents(new JobsSessionListener(jobsManager), plugin);

        // Enregistrer les commandes
        JobsCommand jobsCommand = new JobsCommand(jobsManager, messageHelper, this::reload);
        plugin.getCommand("jobs").setExecutor(jobsCommand);
        plugin.getCommand("jobs").setTabCompleter(jobsCommand);

        // Tâches périodiques : versement groupé des gains et sauvegarde de la progression
        long payoutTicks = Math.max(1, config.getLong("payout-interval-seconds", 10)) * TICKS_PER_SECOND;
        long saveTicks = Math.max(10, config.getLong("save-interval-seconds", 60)) * TICKS_PER_SECOND;
        payoutTask = plugin.getServer().getScheduler().runTaskTimer(plugin, jobsManager::payoutAll, payoutTicks, payoutTicks);
        saveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, jobsManager::saveAll, saveTicks, saveTicks);

        plugin.getLogger().info("Module Métiers activé (" + jobsManager.getJobs().size() + " métiers).");
    }

    /**
     * Recharge jobs.yml (constantes, courbes, messages) et les JSON sans redémarrer.
     * Le fichier est relu dans le même objet pour que les MessageHelper existants voient les nouveaux textes.
     * Les intervalles de versement et de sauvegarde ne changent qu'au redémarrage.
     */
    private void reload() {
        jobsManager.payoutAll();
        jobsManager.saveAll();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.SEVERE, "jobs.yml invalide, ancienne configuration conservée", e);
            return;
        }
        jobsManager.reload();
    }

    public void shutdown() {
        if (payoutTask != null) payoutTask.cancel();
        if (saveTask != null) saveTask.cancel();
        if (jobsManager != null) {
            jobsManager.payoutAll();
            jobsManager.saveAll();
        }
        plugin.getLogger().info("Module Métiers désactivé.");
    }

    public JobsManager getJobsManager() {
        return jobsManager;
    }
}
