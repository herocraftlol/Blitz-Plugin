package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import fr.herocraft.blitz.team.Team;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Gère les items du lobby d'arène :
 * - Quitter (slot 8) : retour à la position pré-join
 * - Sélection d'équipe (slot 2) : changer d'équipe en lobby
 * - Force-start admin (slot 0) : démarrage immédiat
 *
 * Empêche également de dropper/déplacer ces items hors de l'inventaire.
 */
public class LobbyItemListener implements Listener {

    private final BlitzPlugin plugin;

    public LobbyItemListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    // ---- Clic sur les items de lobby ----

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        Arena arena = plugin.findArenaOf(player);
        if (arena == null) return;

        // Item quitter
        if (Arena.isLeaveItem(plugin, item)) {
            event.setCancelled(true);
            if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) {
                player.sendMessage(ChatColor.RED + "Vous ne pouvez pas quitter pendant la partie !");
                return;
            }
            plugin.leaveArena(player);
            return;
        }

        // Force-start admin
        if (Arena.isForceStartItem(plugin, item)) {
            event.setCancelled(true);
            if (!player.hasPermission("blitz.admin")) return;
            if (!arena.isReady()) {
                player.sendMessage(ChatColor.RED + "L'arène n'est pas configurée.");
                return;
            }
            if (!arena.forceStart()) {
                player.sendMessage(ChatColor.RED + "Impossible de démarrer : il faut au moins 1 joueur par équipe !");
                return;
            }
            player.sendMessage(ChatColor.GREEN + "Partie forcée pour " + arena.getName() + ".");
            return;
        }

        // Sélection d'équipe
        if (Arena.isTeamSelectorItem(plugin, item)) {
            event.setCancelled(true);
            if (arena.getState() != ArenaState.WAITING) {
                player.sendMessage(ChatColor.RED + "Vous ne pouvez pas changer d'équipe maintenant !");
                return;
            }
            Team current = arena.getTeam(player);
            if (current == null) return;
            Team target = current.opposite();
            if (arena.changeTeam(player, target)) {
                player.sendMessage(ChatColor.GREEN + "Vous avez rejoint l'équipe "
                        + target.getChatColor() + target.getDisplayName() + ChatColor.GREEN + " !");
            }
            return;
        }
    }

    // ---- Protection des items (drop, déplacement, swap) ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);
        if (arena == null) return;
        ItemStack item = event.getItemDrop().getItemStack();
        if (Arena.isLeaveItem(plugin, item) || Arena.isForceStartItem(plugin, item) || Arena.isTeamSelectorItem(plugin, item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Arena arena = plugin.findArenaOf(player);
        if (arena == null) return;
        if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) return;
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (Arena.isLeaveItem(plugin, item) || Arena.isForceStartItem(plugin, item) || Arena.isTeamSelectorItem(plugin, item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);
        if (arena != null) event.setCancelled(true);
    }
}
