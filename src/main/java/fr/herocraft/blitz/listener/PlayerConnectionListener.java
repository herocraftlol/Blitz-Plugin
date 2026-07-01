package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final BlitzPlugin plugin;

    public PlayerConnectionListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getStatsManager().get(player.getUniqueId(), player.getName());
        if (plugin.getArenaManager().getLobbySpawn() != null) {
            player.teleport(plugin.getArenaManager().getLobbySpawn());
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);
        if (arena != null) {
            arena.removePlayer(player);
        }
        plugin.getStatsManager().save();
    }
}
