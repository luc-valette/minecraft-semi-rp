package fr.mc.semirp.jobs.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.mc.semirp.jobs.model.ActionType;
import fr.mc.semirp.jobs.model.Job;
import fr.mc.semirp.jobs.model.JobAction;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Charge les tables de récompenses depuis plugins/SemiRP/jobs/*.json
 * et les associe aux courbes d'XP définies dans jobs.yml.
 */
public class JobLoader {

    /** Fichiers embarqués dans le .jar et copiés au premier lancement s'ils n'existent pas. */
    private static final List<String> DEFAULT_FILES = List.of(
            "mineur", "bucheron", "fermier", "chasseur", "pecheur", "alchimiste", "forgeron");

    private final JavaPlugin plugin;
    private final Logger logger;

    public JobLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /** Copie les JSON par défaut dans le dossier du plugin, sans jamais écraser un fichier modifié. */
    public void saveDefaults() {
        for (String name : DEFAULT_FILES) {
            String resource = "jobs/" + name + ".json";
            File target = new File(plugin.getDataFolder(), resource);
            if (!target.exists() && plugin.getResource(resource) != null) {
                plugin.saveResource(resource, false);
            }
        }
    }

    /** Charge tous les métiers valides. Un fichier invalide est ignoré avec un avertissement. */
    public Map<String, Job> loadAll(FileConfiguration config) {
        Map<String, Job> jobs = new LinkedHashMap<>();
        File folder = new File(plugin.getDataFolder(), "jobs");
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));

        if (files == null || files.length == 0) {
            logger.warning("Aucun fichier JSON de métier trouvé dans " + folder.getPath());
            return jobs;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            try {
                Job job = loadFile(file, config);
                if (job != null) {
                    jobs.put(job.id(), job);
                    logger.info("Métier chargé : " + job.displayName() + " (" + job.actionCount() + " actions)");
                }
            } catch (IOException | RuntimeException e) {
                logger.log(Level.SEVERE, "Fichier de métier illisible : " + file.getName(), e);
            }
        }
        return jobs;
    }

    private Job loadFile(File file, FileConfiguration config) throws IOException {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        String fileId = file.getName().substring(0, file.getName().length() - ".json".length());
        String id = root.has("metier") ? root.get("metier").getAsString() : fileId;
        id = id.toLowerCase(Locale.ROOT);

        ConfigurationSection curve = config.getConfigurationSection("jobs." + id);
        if (curve == null) {
            logger.warning("Métier '" + id + "' ignoré : aucune entrée jobs." + id + " dans jobs.yml (floor, a).");
            return null;
        }

        String displayName = curve.getString("display-name", id);
        double floor = curve.getDouble("floor");
        double a = curve.getDouble("a");
        if (floor <= 0 || a < 0) {
            logger.warning("Métier '" + id + "' ignoré : floor doit être > 0 et a >= 0 dans jobs.yml.");
            return null;
        }

        Map<String, List<JobAction>> categories = new LinkedHashMap<>();
        Set<String> seenIds = new HashSet<>();

        for (JsonElement categoryElement : root.getAsJsonArray("actions")) {
            JsonObject category = categoryElement.getAsJsonObject();
            String categoryName = category.has("categorie") ? category.get("categorie").getAsString() : "divers";
            List<JobAction> actions = new ArrayList<>();

            JsonArray items = category.getAsJsonArray("items");
            for (JsonElement itemElement : items) {
                JobAction action = parseAction(itemElement.getAsJsonObject(), id);
                if (action == null) continue;

                if (!seenIds.add(action.id())) {
                    logger.warning("[" + id + "] id en double : " + action.id());
                }
                actions.add(action);
            }
            categories.put(categoryName, List.copyOf(actions));
        }

        return new Job(id, displayName, floor, a, Collections.unmodifiableMap(categories));
    }

    private JobAction parseAction(JsonObject item, String jobId) {
        String actionId = item.has("id") ? item.get("id").getAsString() : "?";

        // La cible est dans "block", "item" ou "entity" selon le métier
        String target = null;
        for (String field : List.of("block", "item", "entity")) {
            if (item.has(field)) {
                target = item.get(field).getAsString().toUpperCase(Locale.ROOT);
                break;
            }
        }
        if (target == null) {
            logger.warning("[" + jobId + "] action " + actionId + " ignorée : ni block, ni item, ni entity.");
            return null;
        }

        ActionType type;
        try {
            type = ActionType.valueOf(item.get("action_type").getAsString().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            logger.warning("[" + jobId + "] action " + actionId + " ignorée : action_type absent ou inconnu.");
            return null;
        }

        String label = item.has("label") ? item.get("label").getAsString() : actionId;
        double xp = item.has("xp") ? item.get("xp").getAsDouble() : 0;
        double money = item.has("money") ? item.get("money").getAsDouble() : 0;

        // L'action est gardée, mais on prévient si la cible n'existe pas dans cette version de Minecraft
        // (ex : identifiant renommé par une mise à jour) : elle ne rapporterait jamais rien.
        String problem = checkTarget(type, target);
        if (problem != null) {
            logger.warning("[" + jobId + "] action " + actionId + " : " + problem);
        }

        return new JobAction(actionId, label, target, type, xp, money);
    }

    /** Renvoie une description du problème, ou null si la cible est valide pour ce type d'action. */
    private static String checkTarget(ActionType type, String target) {
        String lower = target.toLowerCase(Locale.ROOT);

        if (type == ActionType.KILL) {
            return Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(lower)) != null
                    ? null : "entité inconnue '" + target + "'";
        }

        if (type == ActionType.BREW && target.startsWith("POTION_")) {
            String potion = lower.substring("potion_".length());
            return Registry.POTION.get(NamespacedKey.minecraft(potion)) != null
                    ? null : "type de potion inconnu '" + potion.toUpperCase(Locale.ROOT) + "'";
        }

        Material material = Material.matchMaterial(target);
        if (material == null) {
            return "bloc ou item inconnu '" + target + "'";
        }
        if ((type == ActionType.BREAK || type == ActionType.PLANT) && !material.isBlock()) {
            return "'" + target + "' n'est pas un bloc, impossible à casser ou planter";
        }
        return null;
    }
}
