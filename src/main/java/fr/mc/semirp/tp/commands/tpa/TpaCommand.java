package fr.mc.semirp.tp.commands.tpa;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class TpaCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public TpaCommand(TpManager tp, MessageHelper messages) {
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

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messages.send(player, "player-not-found", "error", Map.of("value", args[0]));
            return true;
        }

        if (target.equals(player)) {
            messages.send(player, "tpa-no-self", "error");
            return true;
        }

        boolean sent = tp.sendTpaRequest(player.getUniqueId(), target.getUniqueId());
        if (!sent) {
            messages.send(player, "tpa-already-pending", "error", Map.of("player", target.getName()));
            return true;
        }

        messages.send(player, "tpa-sent", "success", Map.of("player", target.getName()));
        messages.send(target, "tpa-received", "info", Map.of("player", player.getName()));
        return true;
    }
}