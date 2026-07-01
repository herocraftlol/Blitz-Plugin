package fr.herocraft.blitz.scoreboard;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import fr.herocraft.blitz.storage.PlayerStats;
import fr.herocraft.blitz.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SidebarManager {

    private final BlitzPlugin plugin;
    private final Map<UUID, Arena> tracked = new HashMap<>();

    public SidebarManager(BlitzPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void update(Arena arena) {
        for (UUID uuid : arena.getPlayers().keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                tracked.put(uuid, arena);
                render(player, arena);
            }
        }
    }

    public void clear(Player player) {
        tracked.remove(player.getUniqueId());
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
    }

    private void tick() {
        for (Map.Entry<UUID, Arena> entry : new HashMap<>(tracked).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            render(player, entry.getValue());
        }
    }

    private void render(Player player, Arena arena) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard board = manager.getNewScoreboard();
        Objective objective = board.registerNewObjective("blitz", "dummy",
                ChatColor.GOLD + "" + ChatColor.BOLD + plugin.getConfig().getString("server-name", "HeroCraft"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;
        objective.getScore(ChatColor.DARK_GRAY + "" + ChatColor.STRIKETHROUGH + "------------------").setScore(line--);
        objective.getScore(ChatColor.YELLOW + plugin.getConfig().getString("game-name", "Blitz")).setScore(line--);
        objective.getScore(ChatColor.GRAY + "Map: " + ChatColor.WHITE + arena.getName()).setScore(line--);
        objective.getScore(ChatColor.RESET.toString() + ChatColor.BLACK + repeatInvisible(1)).setScore(line--);

        if (arena.getState() == ArenaState.PLAYING) {
            objective.getScore(ChatColor.GRAY + "Temps: " + ChatColor.WHITE + formatTime(arena.getElapsedSeconds())).setScore(line--);
        } else if (arena.getState() == ArenaState.STARTING) {
            objective.getScore(ChatColor.GRAY + "Début dans: " + ChatColor.WHITE + Math.max(arena.getCountdownSecondsLeft(), 0) + "s").setScore(line--);
        } else {
            objective.getScore(ChatColor.GRAY + "En attente de joueurs...").setScore(line--);
        }

        objective.getScore(ChatColor.RESET.toString() + ChatColor.BLACK + repeatInvisible(2)).setScore(line--);
        objective.getScore(Team.RED.getChatColor() + "Rouges: " + ChatColor.WHITE + arena.getScore(Team.RED)
                + ChatColor.GRAY + " (" + arena.getTeamCount(Team.RED) + ")").setScore(line--);
        objective.getScore(Team.BLUE.getChatColor() + "Bleus: " + ChatColor.WHITE + arena.getScore(Team.BLUE)
                + ChatColor.GRAY + " (" + arena.getTeamCount(Team.BLUE) + ")").setScore(line--);

        objective.getScore(ChatColor.RESET.toString() + ChatColor.BLACK + repeatInvisible(3)).setScore(line--);
        PlayerStats stats = plugin.getStatsManager().get(player.getUniqueId(), player.getName());
        objective.getScore(ChatColor.GRAY + "K/D: " + ChatColor.WHITE + stats.getKd()).setScore(line--);

        player.setScoreboard(board);
    }

    private String repeatInvisible(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(ChatColor.RESET);
        return sb.toString();
    }

    private String formatTime(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
