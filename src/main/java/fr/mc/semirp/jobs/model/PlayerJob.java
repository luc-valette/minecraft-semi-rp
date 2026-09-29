package fr.mc.semirp.jobs.model;

/**
 * Progression d'un joueur dans un métier (une ligne de la table player_jobs).
 * Un métier quitté reste en base avec active = false et son niveau réduit de moitié :
 * si le joueur le reprend plus tard, il repart de ce niveau.
 */
public class PlayerJob {

    private final String jobId;
    private int level;
    private double xp;
    private boolean active;
    private boolean dirty;

    public PlayerJob(String jobId, int level, double xp, boolean active) {
        this.jobId = jobId;
        this.level = level;
        this.xp = xp;
        this.active = active;
    }

    public String getJobId() {
        return jobId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
        this.dirty = true;
    }

    /** XP accumulée dans le niveau en cours (valeur réelle, sans multiplicateur d'affichage). */
    public double getXp() {
        return xp;
    }

    public void setXp(double xp) {
        this.xp = xp;
        this.dirty = true;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }
}
