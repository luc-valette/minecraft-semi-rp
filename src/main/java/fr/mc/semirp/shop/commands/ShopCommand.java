package fr.mc.semirp.shop.commands;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.shop.ShopFeedback;
import fr.mc.semirp.shop.ShopManager;
import fr.mc.semirp.shop.gui.ShopGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * /shop                          : ouvre le GUI central
 * /shop sell <prix> [quantité]   : met en vente l'item tenu en main (prix à l'unité)
 * /shop mine                     : ouvre la liste de ses offres pour les retirer
 */
public class ShopCommand implements TabExecutor {

    private final ShopManager shop;
    private final ShopGui gui;
    private final ShopFeedback feedback;
    private final MessageHelper messages;

    public ShopCommand(ShopManager shop, ShopGui gui, ShopFeedback feedback, MessageHelper messages) {
        this.shop = shop;
        this.gui = gui;
        this.feedback = feedback;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only", "error");
            return true;
        }

        if (args.length == 0) {
            gui.openMain(player, 1);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell" -> handleSell(player, args);
            case "mine" -> gui.openMine(player);
            default -> messages.send(player, "usage-shop", "error");
        }
        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2 || args.length > 3) {
            messages.send(player, "usage-sell", "error");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1].replace(',', '.'));
        } catch (NumberFormatException e) {
            messages.send(player, "sell-invalid-price", "error", Map.of("value", args[1]));
            return;
        }

        Integer quantity = null;
        if (args.length == 3) {
            try {
                quantity = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                messages.send(player, "sell-invalid-quantity", "error", Map.of("value", args[2]));
                return;
            }
        }

        ShopManager.Outcome outcome = shop.createListing(player, price, quantity);
        feedback.sell(player, outcome, args[1], args.length == 3 ? args[2] : null);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("sell", "mine").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            return List.of("<prix_unitaire>");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("sell")) {
            return List.of("[quantité]");
        }
        return List.of();
    }
}
