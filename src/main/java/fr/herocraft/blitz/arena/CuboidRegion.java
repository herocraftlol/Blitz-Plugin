package fr.herocraft.blitz.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class CuboidRegion {

    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public CuboidRegion(Location pos1, Location pos2) {
        this.world = pos1.getWorld();
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public CuboidRegion(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.world = world;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public boolean contains(Location loc) {
        if (loc.getWorld() == null || world == null || !loc.getWorld().equals(world)) return false;
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return x >= minX && x <= maxX + 1
                && y >= minY && y <= maxY + 1
                && z >= minZ && z <= maxZ + 1;
    }

    public World getWorld() {
        return world;
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    public long volume() {
        return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public void save(ConfigurationSection section, String path) {
        if (world == null) return;
        section.set(path + ".world", world.getName());
        section.set(path + ".minX", minX);
        section.set(path + ".minY", minY);
        section.set(path + ".minZ", minZ);
        section.set(path + ".maxX", maxX);
        section.set(path + ".maxY", maxY);
        section.set(path + ".maxZ", maxZ);
    }

    public static CuboidRegion load(ConfigurationSection section, String path) {
        if (section == null || !section.isSet(path + ".world")) return null;
        World world = org.bukkit.Bukkit.getWorld(section.getString(path + ".world"));
        if (world == null) return null;
        return new CuboidRegion(world,
                section.getInt(path + ".minX"),
                section.getInt(path + ".minY"),
                section.getInt(path + ".minZ"),
                section.getInt(path + ".maxX"),
                section.getInt(path + ".maxY"),
                section.getInt(path + ".maxZ"));
    }
}
