package fr.mc.semirp.shop;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.shop.ShopManager.Outcome;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Traduit les résultats du ShopManager en messages (clés de shop.yml).
 * Centralisé ici pour que la commande et le GUI affichent exactement les mêmes textes.
 */
public class ShopFeedback {

    private final MessageHelper messages;
    private final ShopManager shop;

    public ShopFeedback(MessageHelper messages, ShopManager shop) {
        this.messages = messages;
        this.shop = shop;
    }

    public void sell(Player player, Outcome outcome, String rawPrice, String rawQuantity) {
        switch (outcome.status()) {
            case SUCCESS -> messages.send(player, "sell-success", "success", Map.of(
                    "quantity", String.valueOf(outcome.quantity()),
                    "item", ShopManager.displayName(outcome.listing().item()),
                    "price", shop.formatMoney(outcome.listing().unitPrice())));
            case NO_ITEM -> messages.send(player, "sell-no-item", "error");
            case INVALID_PRICE -> messages.send(player, "sell-invalid-price", "error", Map.of("value", rawPrice));
            case INVALID_QUANTITY -> messages.send(player, "sell-invalid-quantity", "error",
                    Map.of("value", rawQuantity != null ? rawQuantity : "?"));
            case NOT_ENOUGH_ITEMS -> messages.send(player, "sell-not-enough", "error",
                    Map.of("available", String.valueOf(outcome.quantity())));
            case LIMIT_REACHED -> messages.send(player, "sell-limit", "error",
                    Map.of("max", String.valueOf(outcome.remaining())));
            default -> error(player);
        }
    }

    public void buy(Player buyer, Outcome outcome) {
        switch (outcome.status()) {
            case SUCCESS -> {
                String item = ShopManager.displayName(outcome.listing().item());
                String total = shop.formatMoney(outcome.total());
                messages.send(buyer, "buy-success", "success", Map.of(
                        "quantity", String.valueOf(outcome.quantity()),
                        "item", item,
                        "seller", outcome.listing().sellerName(),
                        "total", total));

                // Le vendeur est prévenu s'il est connecté (l'argent est versé même hors ligne)
                Player seller = Bukkit.getPlayer(outcome.listing().sellerUuid());
                if (seller != null) {
                    messages.send(seller, "buy-notify-seller", "success", Map.of(
                            "buyer", buyer.getName(),
                            "quantity", String.valueOf(outcome.quantity()),
                            "item", item,
                            "total", total));
                }
            }
            case UNAVAILABLE -> messages.send(buyer, "buy-unavailable", "error");
            case OWN_LISTING -> messages.send(buyer, "buy-own", "error");
            case NO_SPACE -> messages.send(buyer, "no-space", "error");
            case INSUFFICIENT_FUNDS -> messages.send(buyer, "insufficient-funds", "error");
            default -> error(buyer);
        }
    }

    public void withdraw(Player player, Outcome outcome) {
        switch (outcome.status()) {
            case SUCCESS -> messages.send(player, "withdraw-success", "success", Map.of(
                    "quantity", String.valueOf(outcome.quantity()),
                    "item", ShopManager.displayName(outcome.listing().item())));
            case PARTIAL -> messages.send(player, "withdraw-partial", "info", Map.of(
                    "quantity", String.valueOf(outcome.quantity()),
                    "item", ShopManager.displayName(outcome.listing().item()),
                    "remaining", String.valueOf(outcome.remaining())));
            case NO_SPACE -> messages.send(player, "no-space", "error");
            case UNAVAILABLE, NOT_OWNER -> messages.send(player, "buy-unavailable", "error");
            default -> error(player);
        }
    }

    /** Erreur générique pour les joueurs, détaillée pour les ops (décision d'architecture). */
    public void error(Player player) {
        if (player.isOp()) {
            messages.send(player, "error-detailed", "error",
                    Map.of("details", "voir la console serveur (module Shop)"));
        } else {
            messages.send(player, "error-generic", "error");
        }
    }
}
