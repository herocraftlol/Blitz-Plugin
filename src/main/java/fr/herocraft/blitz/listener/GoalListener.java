package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import fr.herocraft.blitz.team.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class GoalListener implements Listener {

    private final BlitzPlugin plugin;

    public GoalListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // On vérifie à chaque mouvement (pas seulement au changement de bloc)
        // car le joueur peut "tomber" dans le trou sans changer de bloc X/Z
        if (event.getTo() == null) return;

        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);
        if (arena == null || arena.getState() != ArenaState.PLAYING) return;

        Team team = arena.getTeam(player);
        if (team == null) return;

        // Éviter un double-score : si le joueur vient d'être téléporté (même tick),
        // on ne redéclenche pas de score.
        // Le contains() utilise la position EXACTE du joueur (double), ce qui
        // permet une zone de but même horizontale (un seul bloc de haut).
        if (team == Team.RED && arena.getBlueGoal() != null
                && arena.getBlueGoal().contains(event.getTo())) {
            arena.scorePoint(Team.RED, player);
        } else if (team == Team.BLUE && arena.getRedGoal() != null
                && arena.getRedGoal().contains(event.getTo())) {
            arena.scorePoint(Team.BLUE, player);
        }
    }
}
