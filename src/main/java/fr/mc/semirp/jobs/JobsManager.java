package fr.mc.semirp.jobs;

import fr.mc.semirp.economy.EconomyManager;
import fr.mc.semirp.jobs.database.JobsDatabase;
import fr.mc.semirp.jobs.loader.JobLoader;
import fr.mc.semirp.jobs.model.ActionType;
import fr.mc.semirp.jobs.model.Job;
import fr.mc.semirp.jobs.model.JobAction;
import fr.mc.semirp.jobs.model.PlayerJob;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.Normalizer;
import java.util.*;

/**
 * Cœur du module Métiers : formules de progression, cache des joueurs connectés,
 * versement des gains via l'EconomyManager.
 *
 * Formules (modèle C, décidées dans le cahier des charges) :
 *   XP requise pour passer de N à N+1 : floor + A x N^exposant   (floor et A propres à chaque métier)
 *   XP gagnée par action               : xp_base x (1 + kXP x ln(N+1))
 *   Argent gagné par action            : money_base x (1 + kMoney x ln(N+1))
 * Le multiplicateur d'affichage ne s'applique qu'aux chiffres d'XP montrés au joueur.
 */
public class JobsManager {

    public enum JoinResult { SUCCESS, REJOINED, UNKNOWN_JOB, ALREADY_JOINED, LIMIT_REACHED }

    /** Ce qu'une action a rapporté dans un métier, pour l'affichage. */
    public record Gain(Job job, double xpShown, double money, int level, boolean leveledUp) {
    }

    private record Match(Job job, JobAction action) {
    }

    private final JobsDatabase database;
    private final EconomyManager economy;
    private final JobLoader loader;
    private final FileConfiguration config;

    // Constantes relues à chaque reload
    private int maxSimultaneous;
    private int maxLevel;
    private double displayMultiplier;
    private double curveExponent;
    private double xpGainK;
    private double moneyGainK;

    private Map<String, Job> jobs = Map.of();
    private Map<ActionType, Map<String, List<Match>>> index = Map.of();

    /** Progression des joueurs connectés : uuid -> (id métier -> progression). */
    private final Map<UUID, Map<String, PlayerJob>> cache = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    /** Argent gagné mais pas encore versé : uuid -> (id métier -> montant). */
    private final Map<UUID, Map<String, Double>> pendingMoney = new HashMap<>();

    public JobsManager(JobsDatabase database, EconomyManager economy, JobLoader loader, FileConfiguration config) {
        this.database = database;
        this.economy = economy;
        this.loader = loader;
        this.config = config;
        reload();
    }

    // ── Chargement ────────────────────────────────────────────────

    /** Relit les constantes de jobs.yml et les JSON, puis reconstruit l'index des actions. */
    public void reload() {
        maxSimultaneous = config.getInt("max-simultaneous", 2);
        maxLevel = config.getInt("max-level", 200);
        displayMultiplier = config.getDouble("display-multiplier", 1.0);
        curveExponent = config.getDouble("xp-curve-exponent", 1.9);
        xpGainK = config.getDouble("xp-gain-k", 0.3);
        moneyGainK = config.getDouble("money-gain-k", 0.18);

        jobs = loader.loadAll(config);

        Map<ActionType, Map<String, List<Match>>> newIndex = new EnumMap<>(ActionType.class);
        for (Job job : jobs.values()) {
            for (List<JobAction> actions : job.categories().values()) {
                for (JobAction action : actions) {
                    newIndex.computeIfAbsent(action.type(), t -> new HashMap<>())
                            .computeIfAbsent(action.target(), t -> new ArrayList<>())
                            .add(new Match(job, action));
                }
            }
        }
        index = newIndex;
    }

    public void loadPlayer(UUID uuid, String name) {
        Map<String, PlayerJob> playerJobs = new HashMap<>();
        for (PlayerJob job : database.loadPlayer(uuid)) {
            playerJobs.put(job.getJobId(), job);
        }
        cache.put(uuid, playerJobs);
        names.put(uuid, name);
    }

    /** Verse les gains en attente, sauvegarde et libère la mémoire (déconnexion). */
    public void unloadPlayer(UUID uuid) {
        payout(uuid);
        save(uuid);
        cache.remove(uuid);
        names.remove(uuid);
    }

    // ── Actions ───────────────────────────────────────────────────

    /** Indique si au moins un métier récompense cette action (évite du travail inutile dans les listeners). */
    public boolean isRewarded(ActionType type, String target) {
        Map<String, List<Match>> byTarget = index.get(type);
        return byTarget != null && byTarget.containsKey(target);
    }

    /**
     * Applique les récompenses d'une action à tous les métiers actifs du joueur qui la couvrent.
     *
     * @param count nombre de fois où l'action a été réalisée (ex : craft en shift-clic, 3 potions brassées)
     */
    public List<Gain> handleAction(Player player, ActionType type, String target, int count) {
        if (count <= 0) return List.of();

        Map<String, List<Match>> byTarget = index.get(type);
        if (byTarget == null) return List.of();
        List<Match> matches = byTarget.get(target);
        if (matches == null) return List.of();

        Map<String, PlayerJob> playerJobs = cache.get(player.getUniqueId());
        if (playerJobs == null) return List.of();

        List<Gain> gains = new ArrayList<>();
        for (Match match : matches) {
            PlayerJob progress = playerJobs.get(match.job().id());
            if (progress == null || !progress.isActive()) continue;

            int level = progress.getLevel();
            double money = moneyGain(match.action().baseMoney(), level) * count;
            double xp = level >= maxLevel ? 0 : xpGain(match.action().baseXp(), level) * count;

            boolean leveledUp = false;
            double currentXp = progress.getXp() + xp;
            while (level < maxLevel && currentXp >= xpRequired(match.job(), level)) {
                currentXp -= xpRequired(match.job(), level);
                level++;
                leveledUp = true;
            }
            if (level >= maxLevel) {
                currentXp = 0;
            }
            progress.setLevel(level);
            progress.setXp(currentXp);

            if (money > 0) {
                pendingMoney.computeIfAbsent(player.getUniqueId(), u -> new HashMap<>())
                        .merge(match.job().id(), money, Double::sum);
            }

            gains.add(new Gain(match.job(), xp * displayMultiplier, money, level, leveledUp));
        }
        return gains;
    }

    // ── Rejoindre / quitter ───────────────────────────────────────

    public JoinResult join(Player player, String jobId) {
        Job job = jobs.get(jobId);
        if (job == null) return JoinResult.UNKNOWN_JOB;

        Map<String, PlayerJob> playerJobs = cache.computeIfAbsent(player.getUniqueId(), u -> new HashMap<>());
        PlayerJob existing = playerJobs.get(jobId);
        if (existing != null && existing.isActive()) return JoinResult.ALREADY_JOINED;
        if (getActiveJobs(player.getUniqueId()).size() >= maxSimultaneous) return JoinResult.LIMIT_REACHED;

        JoinResult result;
        if (existing != null) {
            // Métier déjà pratiqué puis quitté : on reprend au niveau conservé
            existing.setActive(true);
            result = JoinResult.REJOINED;
        } else {
            PlayerJob created = new PlayerJob(jobId, 1, 0, true);
            created.markDirty();
            playerJobs.put(jobId, created);
            result = JoinResult.SUCCESS;
        }
        save(player.getUniqueId());
        return result;
    }

    /**
     * Quitte un métier : le niveau est divisé par deux, arrondi au supérieur
     * (100 -> 50, 99 -> 50, 101 -> 51) et l'XP du niveau en cours est remise à 0.
     *
     * @return le nouveau niveau conservé, ou -1 si le joueur n'exerce pas ce métier
     */
    public int leave(Player player, String jobId) {
        Map<String, PlayerJob> playerJobs = cache.get(player.getUniqueId());
        if (playerJobs == null) return -1;
        PlayerJob progress = playerJobs.get(jobId);
        if (progress == null || !progress.isActive()) return -1;

        payout(player.getUniqueId());

        int newLevel = halvedLevel(progress.getLevel());
        progress.setLevel(newLevel);
        progress.setXp(0);
        progress.setActive(false);
        save(player.getUniqueId());
        return newLevel;
    }

    public static int halvedLevel(int level) {
        return Math.max(1, (level + 1) / 2);
    }

    // ── Formules ──────────────────────────────────────────────────

    /** XP réelle nécessaire pour passer du niveau N au niveau N+1. */
    public double xpRequired(Job job, int level) {
        return job.floor() + job.a() * Math.pow(level, curveExponent);
    }

    public double xpGain(double baseXp, int level) {
        return baseXp * (1 + xpGainK * Math.log(level + 1));
    }

    public double moneyGain(double baseMoney, int level) {
        return baseMoney * (1 + moneyGainK * Math.log(level + 1));
    }

    // ── Versement et sauvegarde ───────────────────────────────────

    /** Verse les gains en attente de tous les joueurs (appelé périodiquement). */
    public void payoutAll() {
        for (UUID uuid : new ArrayList<>(pendingMoney.keySet())) {
            payout(uuid);
        }
    }

    /**
     * Verse les gains en attente d'un joueur, une transaction par métier.
     * Les fractions de centime restent en attente pour le prochain versement.
     */
    public void payout(UUID uuid) {
        Map<String, Double> pending = pendingMoney.get(uuid);
        if (pending == null) return;

        Iterator<Map.Entry<String, Double>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Double> entry = it.next();
            double rounded = Math.floor(entry.getValue() * 100.0) / 100.0;
            if (rounded <= 0) continue;

            Job job = jobs.get(entry.getKey());
            String label = job != null ? job.displayName() : entry.getKey();
            if (economy.deposit(uuid, rounded, "Métier " + label)) {
                double rest = entry.getValue() - rounded;
                if (rest > 0.000001) {
                    entry.setValue(rest);
                } else {
                    it.remove();
                }
            }
        }
        if (pending.isEmpty()) {
            pendingMoney.remove(uuid);
        }
    }

    public void saveAll() {
        for (UUID uuid : cache.keySet()) {
            save(uuid);
        }
    }

    public void save(UUID uuid) {
        Map<String, PlayerJob> playerJobs = cache.get(uuid);
        if (playerJobs == null) return;
        String name = names.getOrDefault(uuid, uuid.toString());
        for (PlayerJob job : playerJobs.values()) {
            if (job.isDirty() && database.save(uuid, name, job)) {
                job.markClean();
            }
        }
    }

    // ── Lecture ───────────────────────────────────────────────────

    public Collection<Job> getJobs() {
        return jobs.values();
    }

    /** Retrouve un métier par son id ou son nom affiché, sans tenir compte des accents ni de la casse. */
    public Job findJob(String input) {
        String wanted = normalize(input);
        for (Job job : jobs.values()) {
            if (normalize(job.id()).equals(wanted) || normalize(job.displayName()).equals(wanted)) {
                return job;
            }
        }
        return null;
    }

    public Job getJob(String id) {
        return jobs.get(id);
    }

    public List<PlayerJob> getActiveJobs(UUID uuid) {
        Map<String, PlayerJob> playerJobs = cache.get(uuid);
        if (playerJobs == null) return List.of();
        return playerJobs.values().stream().filter(PlayerJob::isActive).toList();
    }

    public PlayerJob getProgress(UUID uuid, String jobId) {
        Map<String, PlayerJob> playerJobs = cache.get(uuid);
        return playerJobs == null ? null : playerJobs.get(jobId);
    }

    public List<JobsDatabase.TopEntry> getTop(String jobId, int limit) {
        saveAll(); // le classement inclut la progression pas encore sauvegardée
        return database.getTop(jobId, limit);
    }

    public int getMaxSimultaneous() {
        return maxSimultaneous;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public double getDisplayMultiplier() {
        return displayMultiplier;
    }

    public String formatMoney(double amount) {
        return economy.formatMoney(amount);
    }

    private static String normalize(String value) {
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toLowerCase(Locale.ROOT).trim();
    }
}
