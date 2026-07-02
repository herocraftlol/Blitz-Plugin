package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.Arena.ChestEntry;
import fr.herocraft.blitz.arena.ArenaState;
import fr.herocraft.blitz.team.Team;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Restreint l'ouverture des coffres d'une arène par équipe.
 *
 * Chaque coffre est explicitement assigné à une équipe via /blitz addchest.
 * La restriction ne se base plus sur la distance au spawn.
 */
public class ChestListener implements Listener {

    private final BlitzPlugin plugin;

    public ChestListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getInventory().getType() != InventoryType.CHEST) return;

        Block block = null;
        if (event.getInventory().getHolder() instanceof Chest chest) {
            block = chest.getBlock();
        }
        if (block == null) return;

        Location chestLoc = block.getLocation();

        // Trouver l'arène et l'entrée de coffre correspondantes
        Arena arena = findArenaForChest(chestLoc);
        if (arena == null) return;

        ChestEntry entry = arena.getChestEntry(chestLoc);
        if (entry == null) return;

        // Hors partie : admins seulement
        if (arena.getState() != ArenaState.PLAYING) {
            if (!player.hasPermission("blitz.admin")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Les coffres de l'arène ne sont accessibles qu'en cours de partie.");
            }
            return;
        }

        // En partie : vérifier que le joueur participe
        Team playerTeam = arena.getTeam(player);
        if (playerTeam == null) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Vous ne participez pas à cette partie.");
            return;
        }

        // Vérifier l'équipe du coffre
        if (entry.team != playerTeam) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Ce coffre appartient à l'équipe "
                    + entry.team.getChatColor() + entry.team.getDisplayName()
                    + ChatColor.RED + " !");
        }
    }

    private Arena findArenaForChest(Location loc) {
        for (Arena arena : plugin.getArenaManager().getAll()) {
            if (arena.getChestEntry(loc) != null) return arena;
        }
        return null;
    }
}
