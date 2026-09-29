package fr.mc.semirp.shop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Marqueur attaché aux inventaires du shop.
 * Permet au listener de reconnaître un GUI du shop et de savoir
 * quelle offre se trouve dans chaque slot.
 */
public class ShopGuiHolder implements InventoryHolder {

    public enum Type { MAIN, MINE }

    private final Type type;
    private final int page;
    private final Map<Integer, Integer> listingBySlot = new HashMap<>();
    private Inventory inventory;

    public ShopGuiHolder(Type type, int page) {
        this.type = type;
        this.page = page;
    }

    public Type getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public void bind(int slot, int listingId) {
        listingBySlot.put(slot, listingId);
    }

    /** Id de l'offre dans ce slot, ou null si le slot n'est pas une offre. */
    public Integer getListingId(int slot) {
        return listingBySlot.get(slot);
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
