package fr.mc.semirp.tp.commands.warps;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SetWarpCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public SetWarpCommand(TpManager tp, MessageHelper messages) {
        this.tp = tp;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only", "error");
            return true;
        }

        if (!sender.hasPermission("semirp.tp.admin")) {
            messages.send(sender, "no-permission", "error");
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        String name = args[0].toLowerCase();
        boolean created = tp.setWarp(name, player.getLocation(), player.getName());

        if (created) {
            messages.send(player, "warp-set", "success", Map.of("name", name));
        } else {
            messages.send(player, "warp-exists", "error", Map.of("name", name));
        }
        return true;
    }
}