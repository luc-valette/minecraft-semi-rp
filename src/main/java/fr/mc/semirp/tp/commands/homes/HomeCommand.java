package fr.mc.semirp.tp.commands.homes;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class HomeCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public HomeCommand(TpManager tp, MessageHelper messages) {
        this.tp = tp;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only", "error");
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        String name = args[0].toLowerCase();
        Location home = tp.getHome(player.getUniqueId(), name);

        if (home == null) {
            messages.send(player, "home-not-found", "error", Map.of("name", name));
            return true;
        }

        tp.saveBackLocation(player.getUniqueId(), player.getLocation());
        player.teleport(home);
        messages.send(player, "home-teleported", "success", Map.of("name", name));
        return true;
    }
}