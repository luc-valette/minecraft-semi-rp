package fr.mc.semirp.jobs.listeners;

import fr.mc.semirp.jobs.JobsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Charge la progression d'un joueur à la connexion et la sauvegarde à la déconnexion.
 */
public class JobsSessionListener implements Listener {

    private final JobsManager jobs;

    public JobsSessionListener(JobsManager jobs) {
        this.jobs = jobs;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        jobs.loadPlayer(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        jobs.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
