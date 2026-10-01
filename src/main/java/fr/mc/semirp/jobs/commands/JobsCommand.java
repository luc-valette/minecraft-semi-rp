package fr.mc.semirp.jobs.commands;

import fr.mc.semirp.common.MessageHelper;
import fr.mc.semirp.jobs.JobsManager;
import fr.mc.semirp.jobs.database.JobsDatabase;
import fr.mc.semirp.jobs.model.Job;
import fr.mc.semirp.jobs.model.JobAction;
import fr.mc.semirp.jobs.model.PlayerJob;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

/**
 * /jobs                          : liste des métiers
 * /jobs join <métier>            : rejoindre un métier
 * /jobs leave <métier> [confirm] : quitter un métier (perte de 50 % du niveau, confirmation demandée)
 * /jobs info <métier>            : détail des récompenses d'un métier
 * /jobs stats                    : ses métiers, niveaux et XP
 * /jobs top [métier]             : classement
 * /jobs reload                   : recharge jobs.yml et les JSON (admin)
 */
public class JobsCommand implements TabExecutor {

    private static final int TOP_SIZE = 10;
    private static final int BAR_LENGTH = 20;

    private final JobsManager jobs;
    private final MessageHelper messages;
    private final Runnable reloadAction;

    public JobsCommand(JobsManager jobs, MessageHelper messages, Runnable reloadAction) {
        this.jobs = jobs;
        this.messages = messages;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "list" -> handleList(sender);
            case "join" -> requirePlayer(sender, player -> handleJoin(player, args));
            case "leave" -> requirePlayer(sender, player -> handleLeave(player, args));
            case "info" -> handleInfo(sender, args);
            case "stats" -> requirePlayer(sender, this::handleStats);
            case "top" -> handleTop(sender, args);
            case "reload" -> handleReload(sender);
            default -> messages.send(sender, "usage-jobs", "error");
        }
        return true;
    }

    // ── /jobs ─────────────────────────────────────────────────────

    private void handleList(CommandSender sender) {
        Collection<Job> all = jobs.getJobs();
        if (all.isEmpty()) {
            messages.send(sender, "no-jobs-loaded", "error");
            return;
        }

        messages.send(sender, "list-header", "primary", Map.of("count", String.valueOf(all.size())));
        for (Job job : all) {
            boolean joined = sender instanceof Player player
                    && isActive(jobs.getProgress(player.getUniqueId(), job.id()));
            messages.send(sender, joined ? "list-entry-joined" : "list-entry", joined ? "success" : "info", Map.of(
                    "job", job.displayName(),
                    "id", job.id(),
                    "actions", String.valueOf(job.actionCount())));
        }
    }

    // ── /jobs join ────────────────────────────────────────────────

    private void handleJoin(Player player, String[] args) {
        if (args.length != 2) {
            messages.send(player, "usage-join", "error");
            return;
        }
        Job job = jobs.findJob(args[1]);
        if (job == null) {
            messages.send(player, "job-not-found", "error", Map.of("value", args[1]));
            return;
        }

        switch (jobs.join(player, job.id())) {
            case SUCCESS -> messages.send(player, "join-success", "success", Map.of("job", job.displayName()));
            case REJOINED -> messages.send(player, "join-rejoined", "success", Map.of(
                    "job", job.displayName(),
                    "level", String.valueOf(jobs.getProgress(player.getUniqueId(), job.id()).getLevel())));
            case ALREADY_JOINED -> messages.send(player, "join-already", "error", Map.of("job", job.displayName()));
            case LIMIT_REACHED -> messages.send(player, "join-limit", "error",
                    Map.of("max", String.valueOf(jobs.getMaxSimultaneous())));
            case UNKNOWN_JOB -> messages.send(player, "job-not-found", "error", Map.of("value", args[1]));
        }
    }

    // ── /jobs leave ───────────────────────────────────────────────

    private void handleLeave(Player player, String[] args) {
        if (args.length < 2 || args.length > 3) {
            messages.send(player, "usage-leave", "error");
            return;
        }
        Job job = jobs.findJob(args[1]);
        if (job == null) {
            messages.send(player, "job-not-found", "error", Map.of("value", args[1]));
            return;
        }

        PlayerJob progress = jobs.getProgress(player.getUniqueId(), job.id());
        if (!isActive(progress)) {
            messages.send(player, "leave-not-joined", "error", Map.of("job", job.displayName()));
            return;
        }

        // La perte de niveau est définitive : on demande une confirmation explicite
        boolean confirmed = args.length == 3 && args[2].equalsIgnoreCase("confirm");
        if (!confirmed) {
            messages.send(player, "leave-confirm", "info", Map.of(
                    "job", job.displayName(),
                    "id", job.id(),
                    "level", String.valueOf(progress.getLevel()),
                    "new-level", String.valueOf(JobsManager.halvedLevel(progress.getLevel()))));
            return;
        }

        int newLevel = jobs.leave(player, job.id());
        messages.send(player, "leave-success", "success", Map.of(
                "job", job.displayName(),
                "level", String.valueOf(newLevel)));
    }

    // ── /jobs info ────────────────────────────────────────────────

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length != 2) {
            messages.send(sender, "usage-info", "error");
            return;
        }
        Job job = jobs.findJob(args[1]);
        if (job == null) {
            messages.send(sender, "job-not-found", "error", Map.of("value", args[1]));
            return;
        }

        // Les gains sont affichés au niveau actuel du joueur (niveau 1 s'il n'exerce pas ce métier)
        int level = 1;
        if (sender instanceof Player player) {
            PlayerJob progress = jobs.getProgress(player.getUniqueId(), job.id());
            if (progress != null) level = progress.getLevel();
        }

        messages.send(sender, "info-header", "primary", Map.of(
                "job", job.displayName(),
                "level", String.valueOf(level)));

        for (Map.Entry<String, List<JobAction>> category : job.categories().entrySet()) {
            List<JobAction> actions = category.getValue();
            if (actions.isEmpty()) continue;

            double xpMin = Double.MAX_VALUE, xpMax = 0, moneyMin = Double.MAX_VALUE, moneyMax = 0;
            for (JobAction action : actions) {
                double xp = jobs.xpGain(action.baseXp(), level) * jobs.getDisplayMultiplier();
                double money = jobs.moneyGain(action.baseMoney(), level);
                xpMin = Math.min(xpMin, xp);
                xpMax = Math.max(xpMax, xp);
                moneyMin = Math.min(moneyMin, money);
                moneyMax = Math.max(moneyMax, money);
            }

            messages.send(sender, "info-category", "info", Map.of(
                    "category", category.getKey().replace('_', ' '),
                    "count", String.valueOf(actions.size()),
                    "xp", range(xpMin, xpMax, false),
                    "money", range(moneyMin, moneyMax, true)));

            // Détail : chaque item du métier qui rapporte de l'argent, à ce niveau
            for (JobAction action : actions) {
                double money = jobs.moneyGain(action.baseMoney(), level);
                if (money <= 0) continue;
                double xp = jobs.xpGain(action.baseXp(), level) * jobs.getDisplayMultiplier();
                messages.send(sender, "info-item", "info", Map.of(
                        "label", action.label(),
                        "money", range(money, money, true),
                        "xp", range(xp, xp, false)));
            }
        }
    }

    // ── /jobs stats ───────────────────────────────────────────────

    private void handleStats(Player player) {
        List<PlayerJob> active = jobs.getActiveJobs(player.getUniqueId());
        if (active.isEmpty()) {
            messages.send(player, "stats-empty", "info");
            return;
        }

        messages.send(player, "stats-header", "primary", Map.of(
                "count", String.valueOf(active.size()),
                "max", String.valueOf(jobs.getMaxSimultaneous())));

        for (PlayerJob progress : active) {
            Job job = jobs.getJob(progress.getJobId());
            if (job == null) continue; // métier retiré des JSON depuis

            if (progress.getLevel() >= jobs.getMaxLevel()) {
                messages.send(player, "stats-entry-max", "success", Map.of(
                        "job", job.displayName(),
                        "level", String.valueOf(progress.getLevel())));
                continue;
            }

            double required = jobs.xpRequired(job, progress.getLevel());
            double ratio = Math.min(1.0, progress.getXp() / required);
            double multiplier = jobs.getDisplayMultiplier();

            messages.send(player, "stats-entry", "info", Map.of(
                    "job", job.displayName(),
                    "level", String.valueOf(progress.getLevel()),
                    "max", String.valueOf(jobs.getMaxLevel()),
                    "xp", String.format(Locale.ROOT, "%,.0f", progress.getXp() * multiplier),
                    "required", String.format(Locale.ROOT, "%,.0f", required * multiplier),
                    "bar", progressBar(ratio),
                    "percent", String.valueOf((int) Math.floor(ratio * 100))));
        }
    }

    // ── /jobs top ─────────────────────────────────────────────────

    private void handleTop(CommandSender sender, String[] args) {
        Job job = null;
        if (args.length >= 2) {
            job = jobs.findJob(args[1]);
            if (job == null) {
                messages.send(sender, "job-not-found", "error", Map.of("value", args[1]));
                return;
            }
        }

        List<JobsDatabase.TopEntry> top = jobs.getTop(job != null ? job.id() : null, TOP_SIZE);
        String scope = job != null ? job.displayName() : "tous métiers";
        messages.send(sender, "top-header", "primary", Map.of("scope", scope));

        if (top.isEmpty()) {
            messages.send(sender, "top-empty", "info");
            return;
        }

        int rank = 1;
        for (JobsDatabase.TopEntry entry : top) {
            Job entryJob = jobs.getJob(entry.jobId());
            messages.send(sender, "top-entry", "info", Map.of(
                    "rank", String.valueOf(rank++),
                    "player", entry.playerName(),
                    "job", entryJob != null ? entryJob.displayName() : entry.jobId(),
                    "level", String.valueOf(entry.level())));
        }
    }

    // ── /jobs reload ──────────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("semirp.jobs.admin")) {
            messages.send(sender, "no-permission", "error");
            return;
        }
        reloadAction.run();
        messages.send(sender, "reload-success", "success", Map.of("count", String.valueOf(jobs.getJobs().size())));
    }

    // ── Outils ────────────────────────────────────────────────────

    private void requirePlayer(CommandSender sender, Consumer<Player> action) {
        if (sender instanceof Player player) {
            action.accept(player);
        } else {
            messages.send(sender, "player-only", "error");
        }
    }

    private static boolean isActive(PlayerJob progress) {
        return progress != null && progress.isActive();
    }

    private String range(double min, double max, boolean money) {
        String low = money ? jobs.formatMoney(min) : String.format(Locale.ROOT, "%.1f", min);
        String high = money ? jobs.formatMoney(max) : String.format(Locale.ROOT, "%.1f", max);
        return Math.abs(max - min) < 0.005 ? low : low + " à " + high;
    }

    private static String progressBar(double ratio) {
        int filled = (int) Math.round(ratio * BAR_LENGTH);
        return "[" + "|".repeat(filled) + ".".repeat(BAR_LENGTH - filled) + "]";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of("join", "leave", "info", "stats", "top"));
            if (sender.hasPermission("semirp.jobs.admin")) subs.add("reload");
            return filter(subs, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("join") || sub.equals("info") || sub.equals("top")) {
                return filter(jobs.getJobs().stream().map(Job::id).toList(), args[1]);
            }
            if (sub.equals("leave") && sender instanceof Player player) {
                return filter(jobs.getActiveJobs(player.getUniqueId()).stream().map(PlayerJob::getJobId).toList(), args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("leave")) {
            return filter(List.of("confirm"), args[2]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
