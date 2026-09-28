package fr.mc.semirp.tp.commands.warps;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Map;

public class DelWarpCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public DelWarpCommand(TpManager tp, MessageHelper messages) {
        this.tp = tp;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("semirp.tp.admin")) {
            messages.send(sender, "no-permission", "error");
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        String name = args[0].toLowerCase();
        boolean deleted = tp.deleteWarp(name);

        if (deleted) {
            messages.send(sender, "warp-deleted", "success", Map.of("name", name));
        } else {
            messages.send(sender, "warp-not-found", "error", Map.of("name", name));
        }
        return true;
    }
}