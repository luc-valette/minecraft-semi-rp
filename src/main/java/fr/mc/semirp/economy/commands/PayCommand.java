package fr.mc.semirp.economy.commands;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class PayCommand implements CommandExecutor {

    private final EconomyManager economy;
    private final MessageHelper messages;

    public PayCommand(EconomyManager economy, MessageHelper messages) {
        this.economy = economy;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only", "error");
            return true;
        }

        if (args.length != 2) {
            messages.send(player, "usage-pay", "error");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messages.send(player, "player-not-found", "error", Map.of("value", args[0]));
            return true;
        }

        if (target.equals(player)) {
            messages.send(player, "pay-self", "error");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            messages.send(player, "invalid-amount", "error", Map.of("value", args[1]));
            return true;
        }

        if (amount <= 0) {
            messages.send(player, "pay-negative", "error");
            return true;
        }

        amount = Math.round(amount * 100.0) / 100.0;

        String formatted = economy.formatMoney(amount);
        String reason = player.getName() + " → " + target.getName();
        boolean success = economy.transfer(player.getUniqueId(), target.getUniqueId(), amount, reason);

        if (success) {
            messages.send(player, "pay-sent", "success", Map.of("amount", formatted, "player", target.getName()));
            messages.send(target, "pay-received", "success", Map.of("amount", formatted, "player", player.getName()));
        } else {
            messages.send(player, "insufficient-funds", "error");
        }

        return true;
    }
}