package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import fr.herocraft.blitz.team.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class GoalListener implements Listener {

    private final BlitzPlugin plugin;

    public GoalListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);
        if (arena == null || arena.getState() != ArenaState.PLAYING) return;

        Team team = arena.getTeam(player);
        if (team == null) return;

        // Le joueur marque en entrant dans le trou de l'equipe adverse
        if (team == Team.RED && arena.getBlueGoal() != null && arena.getBlueGoal().contains(event.getTo())) {
            arena.scorePoint(Team.RED, player);
        } else if (team == Team.BLUE && arena.getRedGoal() != null && arena.getRedGoal().contains(event.getTo())) {
            arena.scorePoint(Team.BLUE, player);
        }
    }
}
