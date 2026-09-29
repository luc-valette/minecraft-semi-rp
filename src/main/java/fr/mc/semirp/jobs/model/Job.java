package fr.mc.semirp.jobs.model;

import java.util.List;
import java.util.Map;

/**
 * Un métier chargé depuis son fichier JSON, avec sa courbe d'XP lue dans jobs.yml.
 *
 * @param id          identifiant (champ "metier" du JSON, ex : "mineur")
 * @param displayName nom affiché (jobs.yml)
 * @param floor       constante "floor" de la courbe XP_requise(N) = floor + A x N^p
 * @param a           constante "A" de la même courbe
 * @param categories  actions regroupées par catégorie, dans l'ordre du fichier
 */
public record Job(String id, String displayName, double floor, double a, Map<String, List<JobAction>> categories) {

    public int actionCount() {
        return categories.values().stream().mapToInt(List::size).sum();
    }
}
