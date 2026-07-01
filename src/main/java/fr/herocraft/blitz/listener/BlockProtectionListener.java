package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Protection des blocs dans les arènes.
 *
 * En jeu :
 * - Les joueurs peuvent casser les blocs qu'ils ont posés.
 * - Ils peuvent aussi casser les blocs présents dans la liste "breakable-blocks"
 *   de la config (ex: COBWEB).
 * - Pose autorisée uniquement dans la zone de jeu.
 */
public class BlockProtectionListener implements Listener {

    private final BlitzPlugin plugin;
    private final Set<Material> breakableBlocks = new HashSet<>();

    public BlockProtectionListener(BlitzPlugin plugin) {
        this.plugin = plugin;
        reloadBreakableBlocks();
    }

    public void reloadBreakableBlocks() {
        breakableBlocks.clear();
        List<String> list = plugin.getConfig().getStringList("breakable-blocks");
        for (String s : list) {
            try {
                breakableBlocks.add(Material.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("breakable-blocks: matériau inconnu : " + s);
            }
        }
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

        // Autoriser les blocs cassables paramétrés (ex: cobweb)
        if (breakableBlocks.contains(event.getBlock().getType())) {
            // Autoriser sans drop (éviter farm)
            event.setDropItems(false);
            return;
        }

        // Autoriser uniquement les blocs posés par le joueur
        if (!arena.isInResetRegion(event.getBlock().getLocation()) || !arena.isPlacedByPlayer(event.getBlock())) {
            event.setCancelled(true);
            return;
        }

        arena.unmarkPlaced(event.getBlock());
    }

    private boolean isInsideAnyArenaRegion(Location loc) {
        for (Arena arena : plugin.getArenaManager().getAll()) {
            if (arena.getResetRegion() != null && arena.getResetRegion().contains(loc)) return true;
        }
        return false;
    }
}
