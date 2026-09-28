package fr.mc.semirp.tp;

import fr.mc.semirp.tp.database.TpDatabase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class TpManager {

    private final TpDatabase database;
    private final int maxHomes;
    private final int tpaExpirationSeconds;

    // TPA : demandeur → destinataire
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();
    // TPA : timestamps pour l'expiration
    private final Map<UUID, Long> tpaTimestamps = new HashMap<>();
    // /back : dernière position par joueur
    private final Map<UUID, Location> backLocations = new HashMap<>();

    public TpManager(TpDatabase database, FileConfiguration config) {
        this.database = database;
        this.maxHomes = config.getInt("max-homes", 20);
        this.tpaExpirationSeconds = config.getInt("tpa-expiration-seconds", 180);
    }

    // === Homes ===

    public boolean setHome(UUID uuid, String name, Location location) {
        if (database.homeExists(uuid, name)) {
            return false;
        }
        if (database.countHomes(uuid) >= maxHomes) {
            return false;
        }
        database.setHome(uuid, name, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        return true;
    }

    public Location getHome(UUID uuid, String name) {
        Map<String, Object> data = database.getHome(uuid, name);
        return dataToLocation(data);
    }

    public boolean deleteHome(UUID uuid, String name) {
        return database.deleteHome(uuid, name);
    }

    public List<String> listHomes(UUID uuid) {
        return database.listHomes(uuid);
    }

    public int countHomes(UUID uuid) {
        return database.countHomes(uuid);
    }

    public boolean homeExists(UUID uuid, String name) {
        return database.homeExists(uuid, name);
    }

    public int getMaxHomes() {
        return maxHomes;
    }

    // === Warps ===

    public boolean setWarp(String name, Location location, String createdBy) {
        if (database.warpExists(name)) {
            return false;
        }
        database.setWarp(name, location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch(), createdBy);
        return true;
    }

    public Location getWarp(String name) {
        Map<String, Object> data = database.getWarp(name);
        return dataToLocation(data);
    }

    public boolean deleteWarp(String name) {
        return database.deleteWarp(name);
    }

    public List<String> listWarps() {
        return database.listWarps();
    }

    // === TPA ===

    public boolean sendTpaRequest(UUID from, UUID to) {
        if (tpaRequests.containsKey(from) && tpaRequests.get(from).equals(to)) {
            return false;
        }
        tpaRequests.put(from, to);
        tpaTimestamps.put(from, System.currentTimeMillis());
        return true;
    }

    public UUID getPendingRequestFrom(UUID to) {
        for (Map.Entry<UUID, UUID> entry : tpaRequests.entrySet()) {
            if (entry.getValue().equals(to)) {
                UUID from = entry.getKey();
                long timestamp = tpaTimestamps.getOrDefault(from, 0L);
                long elapsed = (System.currentTimeMillis() - timestamp) / 1000;
                if (elapsed <= tpaExpirationSeconds) {
                    return from;
                } else {
                    tpaRequests.remove(from);
                    tpaTimestamps.remove(from);
                    return null;
                }
            }
        }
        return null;
    }

    public void acceptTpa(UUID to) {
        UUID from = getPendingRequestFrom(to);
        if (from != null) {
            Player fromPlayer = Bukkit.getPlayer(from);
            Player toPlayer = Bukkit.getPlayer(to);
            if (fromPlayer != null && toPlayer != null) {
                saveBackLocation(from, fromPlayer.getLocation());
                fromPlayer.teleport(toPlayer.getLocation());
            }
            tpaRequests.remove(from);
            tpaTimestamps.remove(from);
        }
    }

    public void denyTpa(UUID to) {
        UUID from = getPendingRequestFrom(to);
        if (from != null) {
            tpaRequests.remove(from);
            tpaTimestamps.remove(from);
        }
    }

    public int getTpaExpirationSeconds() {
        return tpaExpirationSeconds;
    }

    // === Back ===

    public void saveBackLocation(UUID uuid, Location location) {
        backLocations.put(uuid, location.clone());
    }

    public Location getBackLocation(UUID uuid) {
        return backLocations.get(uuid);
    }

    public void clearBackLocation(UUID uuid) {
        backLocations.remove(uuid);
    }

    // === Utilitaire ===

    private Location dataToLocation(Map<String, Object> data) {
        if (data == null) return null;
        World world = Bukkit.getWorld((String) data.get("world"));
        if (world == null) return null;
        return new Location(world,
                (double) data.get("x"),
                (double) data.get("y"),
                (double) data.get("z"),
                (float) data.get("yaw"),
                (float) data.get("pitch"));
    }
}