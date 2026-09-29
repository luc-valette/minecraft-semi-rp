package fr.mc.semirp.shop.listeners;

import fr.mc.semirp.shop.ShopFeedback;
import fr.mc.semirp.shop.ShopManager;
import fr.mc.semirp.shop.gui.ShopGui;
import fr.mc.semirp.shop.gui.ShopGuiHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Gère les clics dans les GUIs du shop.
 * Tous les clics sont annulés : les items affichés ne sont que des vitrines,
 * les vrais items sont en consigne dans la base de données.
 */
public class ShopGuiListener implements Listener {

    private final JavaPlugin plugin;
    private final ShopManager shop;
    private final ShopGui gui;
    private final ShopFeedback feedback;

    public ShopGuiListener(JavaPlugin plugin, ShopManager shop, ShopGui gui, ShopFeedback feedback) {
        this.plugin = plugin;
        this.shop = shop;
        this.gui = gui;
        this.feedback = feedback;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShopGuiHolder holder)) return;

        // Bloque tout déplacement d'item, y compris depuis l'inventaire du joueur (shift-clic)
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) return;

        int slot = event.getRawSlot();

        switch (holder.getType()) {
            case MAIN -> handleMainClick(player, holder, slot, event.isShiftClick());
            case MINE -> handleMineClick(player, holder, slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ShopGuiHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMainClick(Player player, ShopGuiHolder holder, int slot, boolean shift) {
        int page = holder.getPage();

        if (slot == ShopGui.SLOT_PREVIOUS) {
            reopen(() -> gui.openMain(player, page - 1));
            return;
        }
        if (slot == ShopGui.SLOT_NEXT) {
            reopen(() -> gui.openMain(player, page + 1));
            return;
        }
        if (slot == ShopGui.SLOT_MINE) {
            reopen(() -> gui.openMine(player));
            return;
        }

        Integer listingId = holder.getListingId(slot);
        if (listingId == null) return;

        // Clic simple : 1 exemplaire. Shift-clic : une pile complète (ou le stock restant)
        int requested = 1;
        if (shift) {
            var clicked = holder.getInventory().getItem(slot);
            requested = clicked != null ? clicked.getMaxStackSize() : 1;
        }

        ShopManager.Outcome outcome = shop.buy(player, listingId, requested);
        feedback.buy(player, outcome);
        reopen(() -> gui.openMain(player, page));
    }

    private void handleMineClick(Player player, ShopGuiHolder holder, int slot) {
        if (slot == ShopGui.SLOT_BACK) {
            reopen(() -> gui.openMain(player, 1));
            return;
        }

        Integer listingId = holder.getListingId(slot);
        if (listingId == null) return;

        ShopManager.Outcome outcome = shop.withdraw(player, listingId);
        feedback.withdraw(player, outcome);
        reopen(() -> gui.openMine(player));
    }

    /** Ouvrir un inventaire pendant un clic est déconseillé par Paper : on le fait au tick suivant. */
    private void reopen(Runnable action) {
        plugin.getServer().getScheduler().runTask(plugin, action);
    }
}
