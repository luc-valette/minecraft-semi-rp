package fr.mc.semirp.tp.listeners;

import fr.mc.semirp.tp.TpManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class TpListener implements Listener {

    private final TpManager tpManager;

    public TpListener(TpManager tpManager) {
        this.tpManager = tpManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        tpManager.saveBackLocation(player.getUniqueId(), player.getLocation());
    }
}