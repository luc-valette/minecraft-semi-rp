package fr.mc.semirp.tp.commands.homes;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class HomesCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public HomesCommand(TpManager tp, MessageHelper messages) {
        this.tp = tp;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only", "error");
            return true;
        }

        List<String> homes = tp.listHomes(player.getUniqueId());
        int count = homes.size();
        int max = tp.getMaxHomes();

        messages.send(player, "homes-header", "primary", Map.of(
                "count", String.valueOf(count),
                "max", String.valueOf(max)
        ));

        if (homes.isEmpty()) {
            messages.send(player, "homes-empty", "info");
        } else {
            for (String name : homes) {
                messages.send(player, "homes-entry", "info", Map.of("name", name));
            }
        }
        return true;
    }
}