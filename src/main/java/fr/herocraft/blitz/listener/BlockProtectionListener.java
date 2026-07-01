package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockProtectionListener implements Listener {

    private final BlitzPlugin plugin;

    public BlockProtectionListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);

        if (arena == null) {
            if (isInsideAnyArenaRegion(event.getBlockPlaced().getLocation()) && !player.hasPermission("blitz.admin")) {
                event.setCancelled(true);
            }
            return;
        }

        if (arena.getState() != ArenaState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        if (!arena.isInResetRegion(event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Vous ne pouvez pas construire en dehors de la zone de jeu.");
            return;
        }

        arena.markPlaced(event.getBlockPlaced());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);

        if (arena == null) {
            if (isInsideAnyArenaRegion(event.getBlock().getLocation()) && !player.hasPermission("blitz.admin")) {
                event.setCancelled(true);
            }
            return;
        }

        if (arena.getState() != ArenaState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        if (!arena.isInResetRegion(event.getBlock().getLocation()) || !arena.isPlacedByPlayer(event.getBlock())) {
            event.setCancelled(true);
            return;
        }

        arena.unmarkPlaced(event.getBlock());
    }

    private boolean isInsideAnyArenaRegion(org.bukkit.Location loc) {
        for (Arena arena : plugin.getArenaManager().getAll()) {
            if (arena.getResetRegion() != null && arena.getResetRegion().contains(loc)) return true;
        }
        return false;
    }
}
