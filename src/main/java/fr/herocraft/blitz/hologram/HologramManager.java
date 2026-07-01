package fr.herocraft.blitz.hologram;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.storage.PlayerStats;
import fr.herocraft.blitz.util.LocationUtil;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HologramManager {

    private static final int LINES = 11; // 1 titre + 10 entrees
    private static final double LINE_SPACING = 0.25;

    private final BlitzPlugin plugin;
    private final File file;
    private final List<HologramEntry> holograms = new ArrayList<>();
    private int nextId = 1;

    public HologramManager(BlitzPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 100L, 20L * 60L); // toutes les minutes
    }

    public void create(HologramType type, Location baseLocation) {
        HologramEntry entry = new HologramEntry(nextId++, type, baseLocation.clone(), new ArrayList<>());
        spawnStands(entry);
        holograms.add(entry);
        refresh(entry);
        save();
    }

    public boolean removeNearest(Location loc, double radius) {
        HologramEntry closest = null;
        double closestDist = Double.MAX_VALUE;
        for (HologramEntry entry : holograms) {
            if (entry.location.getWorld() == null || loc.getWorld() == null) continue;
            if (!entry.location.getWorld().equals(loc.getWorld())) continue;
            double dist = entry.location.distance(loc);
            if (dist <= radius && dist < closestDist) {
                closest = entry;
                closestDist = dist;
            }
        }
        if (closest == null) return false;
        despawnStands(closest);
        holograms.remove(closest);
        save();
        return true;
    }

    public void refreshAll() {
        for (HologramEntry entry : holograms) {
            refresh(entry);
        }
    }

    private void spawnStands(HologramEntry entry) {
        for (int i = 0; i < LINES; i++) {
            Location loc = entry.location.clone().add(0, (LINES - 1 - i) * LINE_SPACING, 0);
            ArmorStand stand = (ArmorStand) entry.location.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setCustomNameVisible(true);
            stand.setCustomName(ChatColor.GRAY + "...");
            stand.setSmall(true);
            stand.setPersistent(true);
            entry.standIds.add(stand.getUniqueId());
        }
    }

    private void despawnStands(HologramEntry entry) {
        for (UUID id : entry.standIds) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        entry.standIds.clear();
    }

    private void refresh(HologramEntry entry) {
        // Verifie que les stands existent toujours, sinon on les respawn
        boolean needsRespawn = entry.standIds.size() != LINES;
        if (!needsRespawn) {
            for (UUID id : entry.standIds) {
                if (Bukkit.getEntity(id) == null) {
                    needsRespawn = true;
                    break;
                }
            }
        }
        if (needsRespawn) {
            despawnStands(entry);
            spawnStands(entry);
        }

        List<String> lines = buildLines(entry.type);
        for (int i = 0; i < entry.standIds.size() && i < lines.size(); i++) {
            Entity e = Bukkit.getEntity(entry.standIds.get(i));
            if (e instanceof ArmorStand stand) {
                stand.setCustomName(lines.get(i));
            }
        }
    }

    private List<String> buildLines(HologramType type) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "" + ChatColor.BOLD + "Top " + type.getLabel());

        List<PlayerStats> top = switch (type) {
            case WINS -> plugin.getStatsManager().topWins(10);
            case PLAYED -> plugin.getStatsManager().topPlayed(10);
            case KD -> plugin.getStatsManager().topKd(10);
            case KILLS -> plugin.getStatsManager().topKills(10);
        };

        for (int i = 0; i < 10; i++) {
            if (i < top.size()) {
                PlayerStats s = top.get(i);
                String value = switch (type) {
                    case WINS -> String.valueOf(s.getWins());
                    case PLAYED -> String.valueOf(s.getPlayed());
                    case KD -> String.valueOf(s.getKd());
                    case KILLS -> String.valueOf(s.getKills());
                };
                lines.add(ChatColor.YELLOW + "" + (i + 1) + ". " + ChatColor.WHITE + s.getName()
                        + ChatColor.GRAY + " - " + ChatColor.AQUA + value);
            } else {
                lines.add(ChatColor.DARK_GRAY + "" + (i + 1) + ". -");
            }
        }
        return lines;
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("holograms");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            try {
                int id = Integer.parseInt(key);
                HologramType type = HologramType.valueOf(s.getString("type", "WINS"));
                Location loc = LocationUtil.load(s, "location");
                if (loc == null) continue;
                HologramEntry entry = new HologramEntry(id, type, loc, new ArrayList<>());
                spawnStands(entry);
                holograms.add(entry);
                nextId = Math.max(nextId, id + 1);
            } catch (Exception ignored) {
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAll, 40L);
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (HologramEntry entry : holograms) {
            String path = "holograms." + entry.id;
            cfg.set(path + ".type", entry.type.name());
            LocationUtil.save(cfg, path + ".location", entry.location);
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible de sauvegarder holograms.yml: " + e.getMessage());
        }
    }

    public void shutdownDespawnAll() {
        for (HologramEntry entry : holograms) {
            despawnStands(entry);
        }
    }

    private static class HologramEntry {
        final int id;
        final HologramType type;
        final Location location;
        final List<UUID> standIds;

        HologramEntry(int id, HologramType type, Location location, List<UUID> standIds) {
            this.id = id;
            this.type = type;
            this.location = location;
            this.standIds = standIds;
        }
    }
}
