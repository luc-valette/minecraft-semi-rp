package fr.mc.semirp.shop;

import fr.mc.semirp.common.InventoryUtil;
import fr.mc.semirp.economy.EconomyManager;
import fr.mc.semirp.shop.database.ShopDatabase;
import fr.mc.semirp.shop.model.ShopListing;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Logique métier du shop joueur.
 * Ne gère aucun message : chaque méthode renvoie un résultat que la commande
 * ou le GUI traduit en message via shop.yml.
 */
public class ShopManager {

    public enum Status {
        SUCCESS, NO_ITEM, INVALID_PRICE, INVALID_QUANTITY, NOT_ENOUGH_ITEMS, LIMIT_REACHED,
        UNAVAILABLE, OWN_LISTING, NO_SPACE, INSUFFICIENT_FUNDS, NOT_OWNER, PARTIAL, ERROR
    }

    /** Résultat détaillé d'une opération (quantité effectivement traitée, montant, offre concernée). */
    public record Outcome(Status status, int quantity, double total, int remaining, ShopListing listing) {
        static Outcome of(Status status) {
            return new Outcome(status, 0, 0, 0, null);
        }
    }

    private final ShopDatabase database;
    private final EconomyManager economy;
    private final int maxListingsPerPlayer;
    private final double maxUnitPrice;

    public ShopManager(ShopDatabase database, EconomyManager economy, FileConfiguration config) {
        this.database = database;
        this.economy = economy;
        this.maxListingsPerPlayer = config.getInt("max-listings-per-player", 20);
        this.maxUnitPrice = config.getDouble("max-unit-price", 1_000_000.0);
    }

    // ── Mise en vente ─────────────────────────────────────────────

    /**
     * Met en vente l'item tenu en main. L'item est retiré immédiatement de l'inventaire (consigne).
     *
     * @param quantity quantité voulue, ou null pour vendre toute la pile tenue en main
     */
    public Outcome createListing(Player seller, double unitPrice, Integer quantity) {
        ItemStack hand = seller.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            return Outcome.of(Status.NO_ITEM);
        }

        unitPrice = Math.round(unitPrice * 100.0) / 100.0;
        if (unitPrice <= 0 || unitPrice > maxUnitPrice) {
            return Outcome.of(Status.INVALID_PRICE);
        }

        int amount = quantity != null ? quantity : hand.getAmount();
        if (amount <= 0) {
            return Outcome.of(Status.INVALID_QUANTITY);
        }

        if (maxListingsPerPlayer > 0 && database.countListings(seller.getUniqueId()) >= maxListingsPerPlayer) {
            return new Outcome(Status.LIMIT_REACHED, 0, 0, maxListingsPerPlayer, null);
        }

        ItemStack template = hand.clone();
        template.setAmount(1);

        int available = InventoryUtil.countSimilar(seller.getInventory(), template);
        if (available < amount) {
            return new Outcome(Status.NOT_ENOUGH_ITEMS, available, 0, 0, null);
        }

        int id = database.insertListing(seller.getUniqueId(), seller.getName(), template,
                materialKey(template), unitPrice, amount);
        if (id < 0) {
            return Outcome.of(Status.ERROR);
        }

        // Retrait seulement une fois l'offre enregistrée : aucun item perdu si la BDD échoue
        InventoryUtil.removeSimilar(seller.getInventory(), template, amount);

        ShopListing listing = new ShopListing(id, seller.getUniqueId(), seller.getName(), template, unitPrice, amount);
        return new Outcome(Status.SUCCESS, amount, unitPrice * amount, amount, listing);
    }

    // ── Achat ─────────────────────────────────────────────────────

    /**
     * Achète jusqu'à `requested` exemplaires d'une offre.
     * L'offre est relue en base pour éviter d'acheter un stock déjà vendu.
     */
    public Outcome buy(Player buyer, int listingId, int requested) {
        ShopListing listing = database.getListing(listingId);
        if (listing == null || listing.quantity() <= 0) {
            return Outcome.of(Status.UNAVAILABLE);
        }
        if (listing.sellerUuid().equals(buyer.getUniqueId())) {
            return Outcome.of(Status.OWN_LISTING);
        }

        int quantity = Math.min(requested, listing.quantity());
        if (InventoryUtil.capacityFor(buyer.getInventory(), listing.item()) < quantity) {
            return Outcome.of(Status.NO_SPACE);
        }

        double total = Math.round(listing.unitPrice() * quantity * 100.0) / 100.0;
        String reason = "Shop : " + quantity + "x " + materialKey(listing.item())
                + " (" + buyer.getName() + " -> " + listing.sellerName() + ")";

        // transfer() refuse si le solde de l'acheteur est insuffisant
        if (!economy.transfer(buyer.getUniqueId(), listing.sellerUuid(), total, reason)) {
            return Outcome.of(Status.INSUFFICIENT_FUNDS);
        }

        int remaining = listing.quantity() - quantity;
        if (remaining > 0) {
            database.updateQuantity(listingId, remaining);
        } else {
            database.deleteListing(listingId);
        }

        InventoryUtil.give(buyer.getInventory(), listing.item(), quantity);
        database.logSale(listingId, buyer.getUniqueId(), listing.sellerUuid(), materialKey(listing.item()),
                quantity, listing.unitPrice(), total);

        return new Outcome(Status.SUCCESS, quantity, total, remaining, listing);
    }

    // ── Retrait d'une offre ───────────────────────────────────────

    /** Retire une de ses propres offres et rend les items (autant que l'inventaire le permet). */
    public Outcome withdraw(Player seller, int listingId) {
        ShopListing listing = database.getListing(listingId);
        if (listing == null) {
            return Outcome.of(Status.UNAVAILABLE);
        }
        if (!listing.sellerUuid().equals(seller.getUniqueId())) {
            return Outcome.of(Status.NOT_OWNER);
        }

        int capacity = InventoryUtil.capacityFor(seller.getInventory(), listing.item());
        if (capacity <= 0) {
            return Outcome.of(Status.NO_SPACE);
        }

        int returned = Math.min(capacity, listing.quantity());
        int remaining = listing.quantity() - returned;

        boolean saved = remaining > 0
                ? database.updateQuantity(listingId, remaining)
                : database.deleteListing(listingId);
        if (!saved) {
            return Outcome.of(Status.ERROR);
        }

        InventoryUtil.give(seller.getInventory(), listing.item(), returned);
        Status status = remaining > 0 ? Status.PARTIAL : Status.SUCCESS;
        return new Outcome(status, returned, 0, remaining, listing);
    }

    // ── Lecture ───────────────────────────────────────────────────

    public List<ShopListing> getAllListings() {
        return database.getAllListings();
    }

    public List<ShopListing> getListingsOf(UUID seller) {
        return database.getListingsBySeller(seller);
    }

    public int getMaxListingsPerPlayer() {
        return maxListingsPerPlayer;
    }

    public String formatMoney(double amount) {
        return economy.formatMoney(amount);
    }

    /** Nom lisible d'un item : son nom personnalisé s'il en a un, sinon le nom du matériau. */
    public static String displayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        return materialKey(item).toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    public static String materialKey(ItemStack item) {
        return item.getType().getKey().getKey().toUpperCase(Locale.ROOT);
    }
}
