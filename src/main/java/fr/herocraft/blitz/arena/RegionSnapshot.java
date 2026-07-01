package fr.herocraft.blitz.arena;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

public class RegionSnapshot {

    private final CuboidRegion region;
    private String[] data; // BlockData as string, indexed
    private boolean captured = false;

    public RegionSnapshot(CuboidRegion region) {
        this.region = region;
    }

    public void capture() {
        if (region == null || region.getWorld() == null) return;
        World world = region.getWorld();
        int sx = region.getMinX(), sy = region.getMinY(), sz = region.getMinZ();
        int ex = region.getMaxX(), ey = region.getMaxY(), ez = region.getMaxZ();
        int sizeX = ex - sx + 1, sizeY = ey - sy + 1, sizeZ = ez - sz + 1;
        data = new String[sizeX * sizeY * sizeZ];
        int i = 0;
        for (int x = sx; x <= ex; x++) {
            for (int y = sy; y <= ey; y++) {
                for (int z = sz; z <= ez; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    data[i++] = block.getBlockData().getAsString();
                }
            }
        }
        captured = true;
    }

    /**
     * Restaure la région de manière progressive (étalée sur plusieurs ticks) pour éviter le lag.
     */
    public void restore(org.bukkit.plugin.Plugin plugin, Runnable onDone) {
        if (!captured || region == null || region.getWorld() == null) {
            if (onDone != null) onDone.run();
            return;
        }
        World world = region.getWorld();
        int sx = region.getMinX(), sy = region.getMinY(), sz = region.getMinZ();
        int ey = region.getMaxY();
        int sizeY = ey - sy + 1, sizeZ = region.getMaxZ() - sz + 1;

        final int[] index = {0};
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int processed = 0;
            while (processed < 5000 && index[0] < data.length) {
                int i = index[0];
                int x = sx + (i / (sizeY * sizeZ));
                int rem = i % (sizeY * sizeZ);
                int y = sy + (rem / sizeZ);
                int z = sz + (rem % sizeZ);
                BlockData bd = Bukkit.createBlockData(data[i]);
                Block block = world.getBlockAt(x, y, z);
                if (!block.getBlockData().equals(bd)) {
                    block.setBlockData(bd, false);
                }
                index[0]++;
                processed++;
            }
            if (index[0] >= data.length) {
                task.cancel();
                if (onDone != null) onDone.run();
            }
        }, 1L, 1L);
    }

    public boolean isCaptured() {
        return captured;
    }

    public CuboidRegion getRegion() {
        return region;
    }
}
