package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Tue les joueurs qui sortent de la zone de jeu (resetRegion) pendant une partie.
 */
public class OutOfBoundsListener implements Listener {

    private final BlitzPlugin plugin;

    public OutOfBoundsListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Optimisation : ignorer si seule la direction du regard a changé
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);
        if (arena == null || arena.getState() != ArenaState.PLAYING) return;

        // Si le joueur est hors de la zone de jeu, il meurt
        if (!arena.isInResetRegion(event.getTo())) {
            player.sendMessage(ChatColor.RED + "Vous êtes sorti de la zone de jeu !");
            player.setHealth(0.0);
        }
    }
}
