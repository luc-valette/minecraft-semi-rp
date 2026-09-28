package fr.mc.semirp.tp.commands.tpa;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class TpDenyCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public TpDenyCommand(TpManager tp, MessageHelper messages) {
        this.tp = tp;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only", "error");
            return true;
        }

        UUID fromUuid = tp.getPendingRequestFrom(player.getUniqueId());
        if (fromUuid == null) {
            messages.send(player, "tpa-no-pending", "error");
            return true;
        }

        Player fromPlayer = Bukkit.getPlayer(fromUuid);
        String fromName = fromPlayer != null ? fromPlayer.getName() : "inconnu";

        tp.denyTpa(player.getUniqueId());

        messages.send(player, "tpa-denied", "success", Map.of("player", fromName));
        if (fromPlayer != null) {
            messages.send(fromPlayer, "tpa-denied-notify", "error", Map.of("player", player.getName()));
        }
        return true;
    }
}