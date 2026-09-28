package fr.mc.semirp.tp.commands.warps;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.tp.TpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class WarpsCommand implements CommandExecutor {

    private final TpManager tp;
    private final MessageHelper messages;

    public WarpsCommand(TpManager tp, MessageHelper messages) {
        this.tp = tp;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        List<String> warps = tp.listWarps();

        messages.send(sender, "warps-header", "primary", Map.of(
                "count", String.valueOf(warps.size())
        ));

        if (warps.isEmpty()) {
            messages.send(sender, "warps-empty", "info");
        } else {
            for (String name : warps) {
                messages.send(sender, "warps-entry", "info", Map.of("name", name));
            }
        }
        return true;
    }
}