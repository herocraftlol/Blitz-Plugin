package fr.herocraft.blitz.storage;

import fr.herocraft.blitz.BlitzPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager {

    private final BlitzPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();

    public StatsManager(BlitzPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        if (cfg.getConfigurationSection("players") == null) return;
        for (String key : cfg.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String path = "players." + key;
                String name = cfg.getString(path + ".name", "Unknown");
                PlayerStats stats = new PlayerStats(uuid, name);
                stats.setWins(cfg.getInt(path + ".wins", 0));
                stats.setPlayed(cfg.getInt(path + ".played", 0));
                stats.setKills(cfg.getInt(path + ".kills", 0));
                stats.setDeaths(cfg.getInt(path + ".deaths", 0));
                cache.put(uuid, stats);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (PlayerStats stats : cache.values()) {
            String path = "players." + stats.getUuid();
            cfg.set(path + ".name", stats.getName());
            cfg.set(path + ".wins", stats.getWins());
            cfg.set(path + ".played", stats.getPlayed());
            cfg.set(path + ".kills", stats.getKills());
            cfg.set(path + ".deaths", stats.getDeaths());
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder stats.yml: " + e.getMessage());
        }
    }

    public PlayerStats get(UUID uuid, String name) {
        return cache.computeIfAbsent(uuid, u -> new PlayerStats(u, name));
    }

    public Optional<PlayerStats> getIfPresent(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public List<PlayerStats> topWins(int limit) {
        return sortAndLimit(Comparator.comparingInt(PlayerStats::getWins).reversed(), limit);
    }

    public List<PlayerStats> topPlayed(int limit) {
        return sortAndLimit(Comparator.comparingInt(PlayerStats::getPlayed).reversed(), limit);
    }

    public List<PlayerStats> topKills(int limit) {
        return sortAndLimit(Comparator.comparingInt(PlayerStats::getKills).reversed(), limit);
    }

    public List<PlayerStats> topKd(int limit) {
        return sortAndLimit(Comparator.comparingDouble(PlayerStats::getKd).reversed(), limit);
    }

    private List<PlayerStats> sortAndLimit(Comparator<PlayerStats> comparator, int limit) {
        List<PlayerStats> list = new ArrayList<>(cache.values());
        list.sort(comparator);
        if (list.size() > limit) return list.subList(0, limit);
        return list;
    }
}
