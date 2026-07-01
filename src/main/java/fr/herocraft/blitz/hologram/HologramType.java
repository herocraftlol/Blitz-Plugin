package fr.herocraft.blitz.hologram;

public enum HologramType {
    WINS("Victoires"),
    PLAYED("Parties jouées"),
    KD("K/D"),
    KILLS("Kills");

    private final String label;

    HologramType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
