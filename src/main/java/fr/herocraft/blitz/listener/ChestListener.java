package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
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
 * Restreint l'ouverture des coffres d'une arène :
 * - Hors partie : personne ne peut ouvrir les coffres (sauf admins).
 * - En partie : seuls les joueurs de l'équipe propriétaire du coffre peuvent l'ouvrir.
 *
 * La logique de propriété : un coffre appartient à l'équipe dont le spawn
 * est le plus proche. Les coffres enregistrés sont ceux ajoutés via /blitz addchest.
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

        // Trouver l'arène à laquelle appartient ce coffre
        Arena arena = findArenaForChest(chestLoc);
        if (arena == null) return; // Coffre hors arène, pas de restriction

        // Hors partie : admins seulement
        if (arena.getState() != ArenaState.PLAYING) {
            if (!player.hasPermission("blitz.admin")) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Les coffres de l'arène ne sont accessibles qu'en cours de partie.");
            }
            return;
        }

        // En partie : vérifier que le joueur est dans cette arène
        Team playerTeam = arena.getTeam(player);
        if (playerTeam == null) {
            // Spectateur ou joueur extérieur
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Vous ne participez pas à cette partie.");
            return;
        }

        // Déterminer l'équipe propriétaire du coffre
        Team chestTeam = getChestTeam(arena, chestLoc);
        if (chestTeam == null) return; // Pas de spawn configuré, pas de restriction

        if (chestTeam != playerTeam) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Ce coffre appartient à l'équipe "
                    + chestTeam.getChatColor() + chestTeam.getDisplayName()
                    + ChatColor.RED + " !");
        }
    }

    /**
     * Retourne l'arène qui possède ce coffre (l'un de ses chestLocations).
     */
    private Arena findArenaForChest(Location loc) {
        for (Arena arena : plugin.getArenaManager().getAll()) {
            for (Location chestLoc : arena.getChestLocations()) {
                if (isSameBlock(chestLoc, loc)) return arena;
            }
        }
        return null;
    }

    /**
     * Détermine l'équipe propriétaire d'un coffre :
     * l'équipe dont le spawn est le plus proche du coffre.
     */
    private Team getChestTeam(Arena arena, Location chestLoc) {
        if (arena.getRedSpawn() == null || arena.getBlueSpawn() == null) return null;
        if (!arena.getRedSpawn().getWorld().equals(chestLoc.getWorld())) return null;

        double distRed  = chestLoc.distanceSquared(arena.getRedSpawn());
        double distBlue = chestLoc.distanceSquared(arena.getBlueSpawn());

        return distRed <= distBlue ? Team.RED : Team.BLUE;
    }

    private boolean isSameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return false;
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
