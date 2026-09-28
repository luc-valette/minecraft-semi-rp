package fr.mc.semirp.tp.commands.backs;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BackCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public BackCommand(TpManager tp, MessageHelper messages) {
        this.tp = tp;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only", "error");
            return true;
        }

        Location back = tp.getBackLocation(player.getUniqueId());

        if (back == null) {
            messages.send(player, "back-no-position", "error");
            return true;
        }

        tp.saveBackLocation(player.getUniqueId(), player.getLocation());
        player.teleport(back);
        messages.send(player, "back-teleported", "success");
        return true;
    }
}