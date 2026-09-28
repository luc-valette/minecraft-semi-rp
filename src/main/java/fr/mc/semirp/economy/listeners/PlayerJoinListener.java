package fr.mc.semirp.economy.listeners;

import fr.mc.semirp.economy.EconomyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final EconomyManager economy;

    public PlayerJoinListener(EconomyManager economy) {
        this.economy = economy;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        economy.createPlayerIfNotExists(player.getUniqueId(), player.getName());
    }
}