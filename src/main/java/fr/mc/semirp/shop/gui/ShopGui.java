package fr.mc.semirp.shop.gui;

import fr.mc.semirp.shop.ShopManager;
import fr.mc.semirp.shop.model.ShopListing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Construit les deux écrans du shop :
 * - MAIN : toutes les offres de tous les joueurs, paginées (45 par page)
 * - MINE : les offres du joueur, pour les retirer de la vente
 * Tous les textes viennent de la section "gui" de shop.yml.
 */
public class ShopGui {

    public static final int SIZE = 54;
    public static final int PER_PAGE = 45;
    public static final int SLOT_PREVIOUS = 45;
    public static final int SLOT_MINE = 48;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_BACK = 49;
    public static final int SLOT_NEXT = 53;

    private final ShopManager shop;
    private final FileConfiguration config;

    public ShopGui(ShopManager shop, FileConfiguration config) {
        this.shop = shop;
        this.config = config;
    }

    public void openMain(Player player, int requestedPage) {
        List<ShopListing> listings = shop.getAllListings();
        int pages = Math.max(1, (int) Math.ceil(listings.size() / (double) PER_PAGE));
        int page = Math.max(1, Math.min(requestedPage, pages));

        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.MAIN, page);
        String title = text("gui.title-main", "Shop ({page}/{pages})")
                .replace("{page}", String.valueOf(page))
                .replace("{pages}", String.valueOf(pages));
        Inventory inventory = Bukkit.createInventory(holder, SIZE, colored(title, "primary"));
        holder.setInventory(inventory);

        int start = (page - 1) * PER_PAGE;
        int end = Math.min(start + PER_PAGE, listings.size());
        for (int i = start; i < end; i++) {
            ShopListing listing = listings.get(i);
            int slot = i - start;
            inventory.setItem(slot, listingIcon(listing, "gui.lore-listing"));
            holder.bind(slot, listing.id());
        }

        if (page > 1) {
            inventory.setItem(SLOT_PREVIOUS, button(Material.ARROW, "gui.button-previous", "Page précédente"));
        }
        if (page < pages) {
            inventory.setItem(SLOT_NEXT, button(Material.ARROW, "gui.button-next", "Page suivante"));
        }
        inventory.setItem(SLOT_MINE, button(Material.CHEST, "gui.button-mine", "Mes offres"));

        String info = text("gui.info", "{count} offre(s)").replace("{count}", String.valueOf(listings.size()));
        inventory.setItem(SLOT_INFO, named(new ItemStack(Material.PAPER), info, "info"));

        player.openInventory(inventory);
    }

    public void openMine(Player player) {
        List<ShopListing> listings = shop.getListingsOf(player.getUniqueId());

        ShopGuiHolder holder = new ShopGuiHolder(ShopGuiHolder.Type.MINE, 1);
        String title = text("gui.title-mine", "Mes offres ({count}/{max})")
                .replace("{count}", String.valueOf(listings.size()))
                .replace("{max}", maxLabel());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, colored(title, "primary"));
        holder.setInventory(inventory);

        int shown = Math.min(listings.size(), PER_PAGE);
        for (int slot = 0; slot < shown; slot++) {
            ShopListing listing = listings.get(slot);
            inventory.setItem(slot, listingIcon(listing, "gui.lore-mine"));
            holder.bind(slot, listing.id());
        }

        inventory.setItem(SLOT_BACK, button(Material.BARRIER, "gui.button-back", "Retour au shop"));
        player.openInventory(inventory);
    }

    // ── Construction des icônes ───────────────────────────────────

    private ItemStack listingIcon(ShopListing listing, String lorePath) {
        ItemStack icon = listing.item().clone();
        icon.setAmount(Math.max(1, Math.min(listing.quantity(), icon.getMaxStackSize())));

        Map<String, String> values = Map.of(
                "seller", listing.sellerName(),
                "price", shop.formatMoney(listing.unitPrice()),
                "quantity", String.valueOf(listing.quantity()),
                "stack", String.valueOf(Math.min(listing.quantity(), icon.getMaxStackSize()))
        );

        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            for (String line : config.getStringList(lorePath)) {
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    line = line.replace("{" + entry.getKey() + "}", entry.getValue());
                }
                lore.add(colored(line, "info"));
            }
            meta.lore(lore);
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private ItemStack button(Material material, String path, String fallback) {
        return named(new ItemStack(material), text(path, fallback), "primary");
    }

    private ItemStack named(ItemStack item, String name, String colorKey) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(colored(name, colorKey));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String maxLabel() {
        int max = shop.getMaxListingsPerPlayer();
        return max > 0 ? String.valueOf(max) : "∞";
    }

    private String text(String path, String fallback) {
        return config.getString(path, fallback);
    }

    /** Texte coloré selon shop.yml, sans l'italique que Minecraft applique par défaut aux noms d'items. */
    private Component colored(String text, String colorKey) {
        String colorName = config.getString("colors." + colorKey, "WHITE");
        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase());
        return Component.text(text)
                .color(color != null ? color : NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false);
    }
}
