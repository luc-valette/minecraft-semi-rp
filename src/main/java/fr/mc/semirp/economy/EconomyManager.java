package fr.mc.semirp.economy;

import fr.mc.semirp.economy.database.EconomyDatabase;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {

    private final EconomyDatabase database;
    private final String currencyName;
    private final double startingBalance;
    private final double minBalance;
    private final int decimalPlaces;
    private final boolean logTransactions;

    public EconomyManager(EconomyDatabase database, FileConfiguration config) {
        this.database = database;
        this.currencyName = config.getString("currency-name", "€");
        this.startingBalance = config.getDouble("starting-balance", 1000.0);
        this.minBalance = config.getDouble("min-balance", 0.0);
        this.decimalPlaces = config.getInt("decimal-places", 2);
        this.logTransactions = config.getBoolean("log-transactions", true);
    }

    public void createPlayerIfNotExists(UUID uuid, String name) {
        if (!database.playerExists(uuid)) {
            database.createPlayer(uuid, name, startingBalance);
            if (logTransactions) {
                database.logTransaction(null, uuid, startingBalance, "INITIAL", "Solde de départ");
            }
        } else {
            database.updatePlayerName(uuid, name);
        }
    }

    public double getBalance(UUID uuid) {
        return database.getBalance(uuid);
    }

    public boolean deposit(UUID uuid, double amount, String reason) {
        if (amount <= 0) return false;

        double newBalance = database.getBalance(uuid) + amount;
        boolean success = database.setBalance(uuid, newBalance);

        if (success && logTransactions) {
            database.logTransaction(null, uuid, amount, "DEPOSIT", reason);
        }
        return success;
    }

    public boolean withdraw(UUID uuid, double amount, String reason) {
        if (amount <= 0) return false;

        double currentBalance = database.getBalance(uuid);
        if (currentBalance - amount < minBalance) {
            return false;
        }

        boolean success = database.setBalance(uuid, currentBalance - amount);

        if (success && logTransactions) {
            database.logTransaction(uuid, null, amount, "WITHDRAW", reason);
        }
        return success;
    }

    public boolean transfer(UUID from, UUID to, double amount, String reason) {
        if (amount <= 0) return false;
        if (from.equals(to)) return false;

        double fromBalance = database.getBalance(from);
        if (fromBalance - amount < minBalance) {
            return false;
        }

        double toBalance = database.getBalance(to);
        boolean success = database.setBalance(from, fromBalance - amount)
                && database.setBalance(to, toBalance + amount);

        if (success && logTransactions) {
            database.logTransaction(from, to, amount, "TRANSFER", reason);
        }
        return success;
    }

    public boolean setBalance(UUID uuid, double amount) {
        if (amount < minBalance) return false;

        boolean success = database.setBalance(uuid, amount);

        if (success && logTransactions) {
            database.logTransaction(null, uuid, amount, "ADMIN_SET", "Solde défini par un admin");
        }
        return success;
    }

    public boolean hasEnough(UUID uuid, double amount) {
        return database.getBalance(uuid) >= amount;
    }

    public List<Map.Entry<String, Double>> getTopBalances(int limit) {
        return database.getTopBalances(limit);
    }

    public String formatMoney(double amount) {
        return String.format("%." + decimalPlaces + "f%s", amount, currencyName);
    }
}