package fr.herocraft.blitz.arena;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ArenaManager {

    private final BlitzPlugin plugin;
    private final File file;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private Location lobbySpawn;

    public ArenaManager(BlitzPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        load();
    }

    public Arena create(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (arenas.containsKey(key)) return null;
        Arena arena = new Arena(plugin, name);
        arenas.put(key, arena);
        save();
        return arena;
    }

    public boolean delete(String name) {
        Arena removed = arenas.remove(name.toLowerCase(Locale.ROOT));
        if (removed != null) { removed.shutdown(); save(); return true; }
        return false;
    }

    public Arena get(String name) {
        if (name == null) return null;
        return arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Arena> getAll() { return arenas.values(); }

    /**
     * Meilleure arène disponible pour joinrandom.
     * Priorité : arènes avec joueurs déjà présents, puis arènes vides.
     */
    public Arena findBestJoinable() {
        List<Arena> joinable = new ArrayList<>();
        for (Arena a : arenas.values()) {
            if (a.canJoin() && a.isReady() && a.getPlayerCount() < a.getMaxPerTeam() * 2) {
                joinable.add(a);
            }
        }
        if (joinable.isEmpty()) return null;

        // Priorité : arène la plus peuplée déjà en attente
        joinable.sort(Comparator.comparingInt(Arena::getPlayerCount).reversed());

        // Parmi celles avec le même nombre max de joueurs, choisir aléatoirement
        int maxPop = joinable.get(0).getPlayerCount();
        List<Arena> top = new ArrayList<>();
        for (Arena a : joinable) {
            if (a.getPlayerCount() == maxPop) top.add(a);
        }
        return top.get(ThreadLocalRandom.current().nextInt(top.size()));
    }

    public Location getLobbySpawn() { return lobbySpawn; }

    public void setLobbySpawn(Location lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
        save();
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        lobbySpawn = LocationUtil.load(cfg, "lobby-spawn");

        ConfigurationSection arenasSection = cfg.getConfigurationSection("arenas");
        if (arenasSection == null) return;
        for (String key : arenasSection.getKeys(false)) {
            ConfigurationSection s = arenasSection.getConfigurationSection(key);
            if (s == null) continue;
            Arena arena = new Arena(plugin, s.getString("name", key));
            arena.setRedSpawn(LocationUtil.load(s, "red-spawn"));
            arena.setBlueSpawn(LocationUtil.load(s, "blue-spawn"));
            arena.setArenaLobbySpawn(LocationUtil.load(s, "arena-lobby-spawn"));

            CuboidRegion redGoal = CuboidRegion.load(s, "red-goal");
            CuboidRegion blueGoal = CuboidRegion.load(s, "blue-goal");
            CuboidRegion resetRegion = CuboidRegion.load(s, "reset-region");
            if (redGoal != null) arena.setRedGoal(redGoal);
            if (blueGoal != null) arena.setBlueGoal(blueGoal);
            if (resetRegion != null) arena.setResetRegion(resetRegion);

            arena.setMaxPerTeam(s.getInt("max-per-team", 8));
            arena.setScoreLimit(s.getInt("score-limit", plugin.getConfig().getInt("default-score-limit", 5)));

            List<String> chestList = s.getStringList("chests");
            for (String chestStr : chestList) {
                Location loc = LocationUtil.deserializeSimple(chestStr);
                if (loc != null) arena.getChestLocations().add(loc);
            }
            arenas.put(key.toLowerCase(Locale.ROOT), arena);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        if (lobbySpawn != null) LocationUtil.save(cfg, "lobby-spawn", lobbySpawn);
        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.getName().toLowerCase(Locale.ROOT);
            cfg.set(path + ".name", arena.getName());
            if (arena.getRedSpawn() != null) LocationUtil.save(cfg, path + ".red-spawn", arena.getRedSpawn());
            if (arena.getBlueSpawn() != null) LocationUtil.save(cfg, path + ".blue-spawn", arena.getBlueSpawn());
            if (arena.getArenaLobbySpawn() != null) LocationUtil.save(cfg, path + ".arena-lobby-spawn", arena.getArenaLobbySpawn());
            if (arena.getRedGoal() != null) arena.getRedGoal().save(cfg, path + ".red-goal");
            if (arena.getBlueGoal() != null) arena.getBlueGoal().save(cfg, path + ".blue-goal");
            if (arena.getResetRegion() != null) arena.getResetRegion().save(cfg, path + ".reset-region");
            cfg.set(path + ".max-per-team", arena.getMaxPerTeam());
            cfg.set(path + ".score-limit", arena.getScoreLimit());
            List<String> chestList = new ArrayList<>();
            for (Location loc : arena.getChestLocations()) chestList.add(LocationUtil.serializeSimple(loc));
            cfg.set(path + ".chests", chestList);
        }
        try { cfg.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Impossible de sauvegarder arenas.yml: " + e.getMessage()); }
    }
}
