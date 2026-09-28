package fr.mc.semirp.economy.commands;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class BalanceCommand implements CommandExecutor {

    private final EconomyManager economy;
    private final MessageHelper messages;

    public BalanceCommand(EconomyManager economy, MessageHelper messages) {
        this.economy = economy;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only", "error");
                return true;
            }

            String balance = economy.formatMoney(economy.getBalance(player.getUniqueId()));
            messages.send(player, "balance-self", "primary", Map.of("balance", balance));
            return true;
        }

        if (args.length == 1) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                messages.send(sender, "player-not-found", "error", Map.of("value", args[0]));
                return true;
            }

            String balance = economy.formatMoney(economy.getBalance(target.getUniqueId()));
            messages.send(sender, "balance-other", "primary", Map.of(
                    "player", target.getName(),
                    "balance", balance
            ));
            return true;
        }

        return false;
    }
}