package fr.mc.semirp.tp.commands.homes;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SetHomeCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public SetHomeCommand(TpManager tp, MessageHelper messages) {
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
            messages.send(player, "home-set", "error", Map.of("name", "<nom>"));
            return true;
        }

        String name = args[0].toLowerCase();

        if (tp.homeExists(player.getUniqueId(), name)) {
            messages.send(player, "home-exists", "error", Map.of("name", name));
            return true;
        }

        if (tp.countHomes(player.getUniqueId()) >= tp.getMaxHomes()) {
            messages.send(player, "home-limit", "error", Map.of("max", String.valueOf(tp.getMaxHomes())));
            return true;
        }

        tp.setHome(player.getUniqueId(), name, player.getLocation());
        messages.send(player, "home-set", "success", Map.of("name", name));
        return true;
    }
}