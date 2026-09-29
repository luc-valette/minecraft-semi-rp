package fr.mc.semirp.shop.database;

import fr.mc.semirp.shop.model.ShopListing;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShopDatabase {

    private final Connection connection;
    private final Logger logger;

    public ShopDatabase(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS shop_listings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    seller_uuid TEXT NOT NULL,
                    seller_name TEXT NOT NULL,
                    item BLOB NOT NULL,
                    material TEXT NOT NULL,
                    unit_price REAL NOT NULL,
                    quantity INTEGER NOT NULL,
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS shop_sales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL DEFAULT (datetime('now')),
                    listing_id INTEGER NOT NULL,
                    buyer_uuid TEXT NOT NULL,
                    seller_uuid TEXT NOT NULL,
                    material TEXT NOT NULL,
                    quantity INTEGER NOT NULL,
                    unit_price REAL NOT NULL,
                    total_price REAL NOT NULL
                )
            """);

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_shop_listings_seller ON shop_listings (seller_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_shop_sales_timestamp ON shop_sales (timestamp)");

            logger.info("Tables shop créées.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur lors de la création des tables shop", e);
        }
    }

    /** Insère une offre et renvoie son id, ou -1 en cas d'erreur. */
    public int insertListing(UUID seller, String sellerName, ItemStack template, String material,
                             double unitPrice, int quantity) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO shop_listings (seller_uuid, seller_name, item, material, unit_price, quantity) VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, seller.toString());
            ps.setString(2, sellerName);
            ps.setBytes(3, template.serializeAsBytes());
            ps.setString(4, material);
            ps.setDouble(5, unitPrice);
            ps.setInt(6, quantity);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (insertListing)", e);
        }
        return -1;
    }

    /** Toutes les offres, les plus récentes en premier. */
    public List<ShopListing> getAllListings() {
        return queryListings("SELECT * FROM shop_listings ORDER BY id DESC", null);
    }

    /** Offres d'un vendeur, les plus récentes en premier. */
    public List<ShopListing> getListingsBySeller(UUID seller) {
        return queryListings("SELECT * FROM shop_listings WHERE seller_uuid = ? ORDER BY id DESC", seller.toString());
    }

    public ShopListing getListing(int id) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM shop_listings WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapListing(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (getListing)", e);
        }
        return null;
    }

    public int countListings(UUID seller) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM shop_listings WHERE seller_uuid = ?")) {
            ps.setString(1, seller.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (countListings)", e);
        }
        return 0;
    }

    public boolean updateQuantity(int id, int quantity) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE shop_listings SET quantity = ? WHERE id = ?")) {
            ps.setInt(1, quantity);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (updateQuantity)", e);
            return false;
        }
    }

    public boolean deleteListing(int id) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM shop_listings WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (deleteListing)", e);
            return false;
        }
    }

    public void logSale(int listingId, UUID buyer, UUID seller, String material, int quantity,
                        double unitPrice, double totalPrice) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO shop_sales (listing_id, buyer_uuid, seller_uuid, material, quantity, unit_price, total_price) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, listingId);
            ps.setString(2, buyer.toString());
            ps.setString(3, seller.toString());
            ps.setString(4, material);
            ps.setInt(5, quantity);
            ps.setDouble(6, unitPrice);
            ps.setDouble(7, totalPrice);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (logSale)", e);
        }
    }

    private List<ShopListing> queryListings(String sql, String param) {
        List<ShopListing> listings = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ShopListing listing = mapListing(rs);
                if (listing != null) {
                    listings.add(listing);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Erreur SQL (queryListings)", e);
        }
        return listings;
    }

    private ShopListing mapListing(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        try {
            ItemStack item = ItemStack.deserializeBytes(rs.getBytes("item"));
            return new ShopListing(
                    id,
                    UUID.fromString(rs.getString("seller_uuid")),
                    rs.getString("seller_name"),
                    item,
                    rs.getDouble("unit_price"),
                    rs.getInt("quantity")
            );
        } catch (RuntimeException e) {
            // Item illisible (ex : format changé après une mise à jour du serveur)
            logger.log(Level.WARNING, "Offre shop #" + id + " illisible, ignorée", e);
            return null;
        }
    }
}
