package fr.mc.semirp.economy.commands;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.economy.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class BalTopCommand implements CommandExecutor {

    private final EconomyManager economy;
    private final MessageHelper messages;

    public BalTopCommand(EconomyManager economy, MessageHelper messages) {
        this.economy = economy;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        List<Map.Entry<String, Double>> top = economy.getTopBalances(10);

        messages.send(sender, "baltop-header", "primary");

        if (top.isEmpty()) {
            messages.send(sender, "baltop-empty", "info");
            return true;
        }

        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, Double> entry = top.get(i);
            messages.send(sender, "baltop-entry", "info", Map.of(
                    "rank", String.valueOf(i + 1),
                    "player", entry.getKey(),
                    "balance", economy.formatMoney(entry.getValue())
            ));
        }

        return true;
    }
}