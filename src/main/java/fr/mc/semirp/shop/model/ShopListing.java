package fr.mc.semirp.shop.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Une offre en vente dans le shop.
 * L'item est stocké en consigne : il a quitté l'inventaire du vendeur.
 *
 * @param id         identifiant unique de l'offre
 * @param sellerUuid UUID du vendeur
 * @param sellerName pseudo du vendeur au moment de la mise en vente
 * @param item       modèle de l'item (quantité 1, avec enchantements, nom, etc.)
 * @param unitPrice  prix d'un exemplaire
 * @param quantity   nombre d'exemplaires encore disponibles
 */
public record ShopListing(int id, UUID sellerUuid, String sellerName, ItemStack item, double unitPrice, int quantity) {
}
