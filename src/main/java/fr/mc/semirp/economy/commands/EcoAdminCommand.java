package fr.mc.semirp.economy.commands;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class EcoAdminCommand implements CommandExecutor {

    private final EconomyManager economy;
    private final MessageHelper messages;

    public EcoAdminCommand(EconomyManager economy, MessageHelper messages) {
        this.economy = economy;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("semirp.eco.admin")) {
            messages.send(sender, "no-permission", "error");
            return true;
        }

        if (args.length != 3) {
            messages.send(sender, "usage-eco", "error");
            return true;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            messages.send(sender, "player-not-found", "error", Map.of("value", args[1]));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            messages.send(sender, "invalid-amount", "error", Map.of("value", args[2]));
            return true;
        }

        if (amount < 0) {
            messages.send(sender, "pay-negative", "error");
            return true;
        }

        amount = Math.round(amount * 100.0) / 100.0;
        String formatted = economy.formatMoney(amount);

        switch (action) {
            case "give" -> {
                boolean success = economy.deposit(target.getUniqueId(), amount, "Admin give par " + sender.getName());
                if (success) {
                    messages.send(sender, "eco-give", "success", Map.of("amount", formatted, "player", target.getName()));
                    messages.send(target, "eco-received", "success", Map.of("amount", formatted));
                }
            }
            case "take" -> {
                boolean success = economy.withdraw(target.getUniqueId(), amount, "Admin take par " + sender.getName());
                if (success) {
                    messages.send(sender, "eco-take", "success", Map.of("amount", formatted, "player", target.getName()));
                    messages.send(target, "eco-taken", "info", Map.of("amount", formatted));
                } else {
                    messages.send(sender, "insufficient-funds", "error");
                }
            }
            case "set" -> {
                boolean success = economy.setBalance(target.getUniqueId(), amount);
                if (success) {
                    messages.send(sender, "eco-set", "success", Map.of("amount", formatted, "player", target.getName()));
                    messages.send(target, "eco-set-notify", "info", Map.of("amount", formatted));
                }
            }
            default -> {
                messages.send(sender, "eco-unknown-action", "error");
            }
        }

        return true;
    }
}