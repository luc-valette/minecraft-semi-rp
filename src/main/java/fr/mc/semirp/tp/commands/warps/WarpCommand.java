package fr.mc.semirp.tp.commands.warps;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class WarpCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public WarpCommand(TpManager tp, MessageHelper messages) {
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
        Location warp = tp.getWarp(name);

        if (warp == null) {
            messages.send(player, "warp-not-found", "error", Map.of("name", name));
            return true;
        }

        tp.saveBackLocation(player.getUniqueId(), player.getLocation());
        player.teleport(warp);
        messages.send(player, "warp-teleported", "success", Map.of("name", name));
        return true;
    }
}