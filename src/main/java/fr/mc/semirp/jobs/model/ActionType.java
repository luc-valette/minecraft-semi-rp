package fr.mc.semirp.jobs.model;

/**
 * Types d'actions récompensées, tels qu'écrits dans le champ "action_type" des JSON.
 */
public enum ActionType {
    /** Casser un bloc (mineur, bûcheron, fermier, alchimiste). Clé : type du bloc cassé. */
    BREAK,
    /** Poser un bloc (torches du mineur). Clé : type de l'item posé. */
    PLACE,
    /** Planter une culture (fermier). Clé : type du bloc planté (WHEAT, CARROTS...). */
    PLANT,
    /** Tuer une entité (chasseur). Clé : type de l'entité. */
    KILL,
    /** Pêcher un item (pêcheur). Clé : type de l'item pêché. */
    FISH,
    /** Brasser au brewing stand (alchimiste). Clé : ingrédient d'amélioration ou POTION_<TYPE>. */
    BREW,
    /** Fabriquer à la table de craft (alchimiste, forgeron). Clé : type de l'item fabriqué. */
    CRAFT,
    /** Récupérer un item cuit au four (forgeron). Clé : type de l'item sorti du four. */
    SMELT,
    /** Améliorer à la table de forgeron (forgeron). Clé : type de l'item obtenu. */
    SMITH
}
