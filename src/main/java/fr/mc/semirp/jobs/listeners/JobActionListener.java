package fr.mc.semirp.jobs.listeners;

import fr.mc.semirp.common.InventoryUtil;
import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.jobs.JobsManager;
import fr.mc.semirp.jobs.model.ActionType;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.*;

/**
 * Écoute les événements Paper et les traduit en actions de métier.
 * Priorité MONITOR + ignoreCancelled : on ne récompense que les actions réellement effectuées.
 */
public class JobActionListener implements Listener {

    private final JobsManager jobs;
    private final MessageHelper messages;
    private final boolean showActionBar;

    /** Dernier joueur ayant manipulé chaque alambic : le BrewEvent ne fournit pas de joueur. */
    private final Map<Location, UUID> lastBrewer = new HashMap<>();

    public JobActionListener(JobsManager jobs, MessageHelper messages, boolean showActionBar) {
        this.jobs = jobs;
        this.messages = messages;
        this.showActionBar = showActionBar;
    }

    // ── BREAK : casser un bloc ────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        reward(event.getPlayer(), ActionType.BREAK, key(event.getBlock().getType()), 1);
    }

    // ── PLACE / PLANT : poser un bloc ─────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        // PLACE se base sur l'item tenu (une torche posée au mur devient le bloc WALL_TORCH)
        reward(player, ActionType.PLACE, key(event.getItemInHand().getType()), 1);
        // PLANT se base sur le bloc obtenu (graines de blé -> bloc WHEAT)
        reward(player, ActionType.PLANT, key(event.getBlockPlaced().getType()), 1);
    }

    // ── KILL : tuer une entité ────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;
        reward(killer, ActionType.KILL, key(entity.getType()), 1);
    }

    // ── FISH : pêcher un item ─────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item caught)) return;
        ItemStack stack = caught.getItemStack();
        reward(event.getPlayer(), ActionType.FISH, key(stack.getType()), 1);
    }

    // ── CRAFT : table de craft ────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe().getResult();
        String target = key(result.getType());
        if (!jobs.isRewarded(ActionType.CRAFT, target)) return;

        int crafts = 1;
        if (event.isShiftClick()) {
            // En shift-clic, le jeu fabrique autant que possible : limité par l'ingrédient le plus rare
            // et par la place libre dans l'inventaire
            int minIngredient = Integer.MAX_VALUE;
            for (ItemStack ingredient : event.getInventory().getMatrix()) {
                if (ingredient != null && !ingredient.getType().isAir()) {
                    minIngredient = Math.min(minIngredient, ingredient.getAmount());
                }
            }
            if (minIngredient == Integer.MAX_VALUE) minIngredient = 1;

            int perCraft = Math.max(1, result.getAmount());
            int bySpace = InventoryUtil.capacityFor(player.getInventory(), result) / perCraft;
            crafts = Math.min(minIngredient, bySpace);
        }
        reward(player, ActionType.CRAFT, target, crafts);
    }

    // ── SMELT : sortie du four ────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSmelt(FurnaceExtractEvent event) {
        reward(event.getPlayer(), ActionType.SMELT, key(event.getItemType()), event.getItemAmount());
    }

    // ── SMITH : table de forgeron ─────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmith(SmithItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getInventory().getResult();
        if (result == null || result.getType().isAir()) return;
        reward(player, ActionType.SMITH, key(result.getType()), 1);
    }

    // ── BREW : alambic ────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewerClick(InventoryClickEvent event) {
        rememberBrewer(event.getView().getTopInventory(), event.getWhoClicked().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrewerDrag(InventoryDragEvent event) {
        rememberBrewer(event.getView().getTopInventory(), event.getWhoClicked().getUniqueId());
    }

    private void rememberBrewer(Inventory top, UUID playerId) {
        if (top instanceof BrewerInventory && top.getLocation() != null) {
            lastBrewer.put(top.getLocation().toBlockLocation(), playerId);
        }
    }

    /**
     * Récompense par potion individuelle (un brassage de 3 potions = 3 fois la récompense).
     * Si l'ingrédient est une amélioration présente dans le JSON (GUNPOWDER, REDSTONE, GLOWSTONE_DUST,
     * DRAGON_BREATH), c'est elle qui est récompensée. Sinon, on récompense la potion obtenue
     * via la clé POTION_<TYPE> (ex : POTION_HEALING), sans les préfixes LONG_ / STRONG_.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        UUID brewerId = lastBrewer.get(event.getBlock().getLocation().toBlockLocation());
        if (brewerId == null) return;
        Player player = Bukkit.getPlayer(brewerId);
        if (player == null) return;

        BrewerInventory contents = event.getContents();
        ItemStack ingredient = contents.getIngredient();
        if (ingredient == null || ingredient.getType().isAir()) return;

        List<ItemStack> results = event.getResults();
        List<ItemStack> brewed = new ArrayList<>();
        for (int slot = 0; slot < Math.min(3, results.size()); slot++) {
            ItemStack input = contents.getItem(slot);
            ItemStack output = results.get(slot);
            if (input != null && !input.getType().isAir() && output != null && isPotion(output.getType())) {
                brewed.add(output);
            }
        }
        if (brewed.isEmpty()) return;

        String ingredientKey = key(ingredient.getType());
        if (jobs.isRewarded(ActionType.BREW, ingredientKey)) {
            reward(player, ActionType.BREW, ingredientKey, brewed.size());
            return;
        }

        Map<String, Integer> byPotion = new HashMap<>();
        for (ItemStack potion : brewed) {
            if (!(potion.getItemMeta() instanceof PotionMeta meta)) continue;
            PotionType type = meta.getBasePotionType();
            if (type == null) continue;
            String base = key(type).replaceFirst("^(LONG_|STRONG_)", "");
            byPotion.merge("POTION_" + base, 1, Integer::sum);
        }
        byPotion.forEach((potionKey, count) -> reward(player, ActionType.BREW, potionKey, count));
    }

    // ── Commun ────────────────────────────────────────────────────

    private void reward(Player player, ActionType type, String target, int count) {
        if (!jobs.isRewarded(type, target)) return;

        for (JobsManager.Gain gain : jobs.handleAction(player, type, target, count)) {
            if (showActionBar) {
                messages.sendActionBar(player, "action-gain", "info", Map.of(
                        "job", gain.job().displayName(),
                        "xp", String.format(Locale.ROOT, "%.1f", gain.xpShown()),
                        "money", jobs.formatMoney(gain.money())));
            }
            if (gain.leveledUp()) {
                messages.send(player, "level-up", "success", Map.of(
                        "job", gain.job().displayName(),
                        "level", String.valueOf(gain.level())));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
        }
    }

    private static boolean isPotion(Material material) {
        return material == Material.POTION || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    /** Clé en majuscules alignée sur les JSON (ex : minecraft:diamond_ore -> DIAMOND_ORE). */
    private static String key(Keyed keyed) {
        return keyed.getKey().getKey().toUpperCase(Locale.ROOT);
    }
}
