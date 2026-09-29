package fr.mc.semirp.common;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Outils partagés pour manipuler l'inventaire d'un joueur
 * (utilisés par le Shop et par les Métiers).
 */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    /**
     * Nombre d'exemplaires de l'item qui peuvent encore entrer dans l'inventaire
     * (slots vides + place restante dans les piles identiques).
     */
    public static int capacityFor(PlayerInventory inventory, ItemStack template) {
        int maxStack = template.getMaxStackSize();
        int capacity = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                capacity += maxStack;
            } else if (slot.isSimilar(template)) {
                capacity += Math.max(0, maxStack - slot.getAmount());
            }
        }
        return capacity;
    }

    /** Nombre total d'items similaires au modèle présents dans l'inventaire. */
    public static int countSimilar(PlayerInventory inventory, ItemStack template) {
        int count = 0;
        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot != null && slot.isSimilar(template)) {
                count += slot.getAmount();
            }
        }
        return count;
    }

    /**
     * Retire la quantité demandée d'items similaires au modèle.
     * À appeler seulement après avoir vérifié avec countSimilar.
     */
    public static void removeSimilar(PlayerInventory inventory, ItemStack template, int amount) {
        ItemStack[] contents = inventory.getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack slot = contents[i];
            if (slot == null || !slot.isSimilar(template)) continue;

            int taken = Math.min(remaining, slot.getAmount());
            remaining -= taken;
            if (taken == slot.getAmount()) {
                contents[i] = null;
            } else {
                slot.setAmount(slot.getAmount() - taken);
            }
        }
        inventory.setStorageContents(contents);
    }

    /**
     * Donne la quantité demandée en respectant la taille max des piles.
     * À appeler seulement après avoir vérifié avec capacityFor.
     */
    public static void give(PlayerInventory inventory, ItemStack template, int amount) {
        int maxStack = template.getMaxStackSize();
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack stack = template.clone();
            stack.setAmount(stackAmount);
            inventory.addItem(stack);
            remaining -= stackAmount;
        }
    }
}
