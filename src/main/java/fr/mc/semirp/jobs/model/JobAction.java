package fr.mc.semirp.jobs.model;

/**
 * Une ligne d'une table de récompenses JSON.
 *
 * @param id       identifiant unique (usage interne)
 * @param label    texte lisible
 * @param target   clé Minecraft visée (bloc, item ou entité), en majuscules
 * @param type     événement qui déclenche la récompense
 * @param baseXp   XP de base au niveau 1
 * @param baseMoney argent de base au niveau 1
 */
public record JobAction(String id, String label, String target, ActionType type, double baseXp, double baseMoney) {
}
