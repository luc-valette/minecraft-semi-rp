package fr.mc.semirp.tp.commands.homes;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class DelHomeCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public DelHomeCommand(TpManager tp, MessageHelper messages) {
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
        boolean deleted = tp.deleteHome(player.getUniqueId(), name);

        if (deleted) {
            messages.send(player, "home-deleted", "success", Map.of("name", name));
        } else {
            messages.send(player, "home-not-found", "error", Map.of("name", name));
        }
        return true;
    }
}