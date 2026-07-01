package fr.herocraft.blitz.storage;

import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private String name;
    private int wins;
    private int played;
    private int kills;
    private int deaths;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() { return uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public void addWin() { this.wins++; }

    public int getPlayed() { return played; }
    public void setPlayed(int played) { this.played = played; }
    public void addPlayed() { this.played++; }

    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public void addKill() { this.kills++; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void addDeath() { this.deaths++; }

    public double getKd() {
        if (deaths <= 0) return kills;
        return Math.round((kills / (double) deaths) * 100.0) / 100.0;
    }
}
