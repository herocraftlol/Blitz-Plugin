package fr.herocraft.blitz;

import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaManager;
import fr.herocraft.blitz.command.BlitzCommand;
import fr.herocraft.blitz.gui.ArenaSelectorGUI;
import fr.herocraft.blitz.hologram.HologramManager;
import fr.herocraft.blitz.listener.*;
import fr.herocraft.blitz.scoreboard.SidebarManager;
import fr.herocraft.blitz.storage.StatsManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlitzPlugin extends JavaPlugin {

    private ArenaManager arenaManager;
    private StatsManager statsManager;
    private SidebarManager sidebarManager;
    private HologramManager hologramManager;
    private ArenaSelectorGUI arenaSelectorGUI;

    private NamespacedKey kitKey;
    private NamespacedKey wandKey;
    private NamespacedKey guiArenaKey;
    private NamespacedKey guiRandomKey;

    private final Map<UUID, Location[]> selections = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        kitKey = new NamespacedKey(this, "blitz_kit");
        wandKey = new NamespacedKey(this, "blitz_wand");
        guiArenaKey = new NamespacedKey(this, "blitz_gui_arena");
        guiRandomKey = new NamespacedKey(this, "blitz_gui_random");

        statsManager = new StatsManager(this);
        arenaManager = new ArenaManager(this);
        sidebarManager = new SidebarManager(this);
        hologramManager = new HologramManager(this);
        arenaSelectorGUI = new ArenaSelectorGUI(this);

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GoalListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);

        BlitzCommand executor = new BlitzCommand(this);
        getCommand("blitz").setExecutor(executor);
        getCommand("blitz").setTabCompleter(executor);

        getLogger().info("Blitz activé - HeroCraft");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            for (Arena arena : arenaManager.getAll()) {
                arena.shutdown();
            }
            arenaManager.save();
        }
        if (statsManager != null) statsManager.save();
        if (hologramManager != null) {
            hologramManager.save();
            hologramManager.shutdownDespawnAll();
        }
    }

    public Arena findArenaOf(Player player) {
        for (Arena arena : arenaManager.getAll()) {
            if (arena.getPlayers().containsKey(player.getUniqueId())) return arena;
        }
        return null;
    }

    public void joinArena(Player player, Arena arena) {
        Arena current = findArenaOf(player);
        if (current != null) {
            player.sendMessage(ChatColor.RED + "Vous êtes déjà dans une arène. Utilisez /blitz leave d'abord.");
            return;
        }
        if (!arena.isReady()) {
            player.sendMessage(ChatColor.RED + "Cette arène n'est pas encore configurée.");
            return;
        }
        if (!arena.canJoin()) {
            player.sendMessage(ChatColor.RED + "Cette arène n'est pas disponible actuellement (partie en cours).");
            return;
        }
        var team = arena.addPlayer(player);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Impossible de rejoindre cette arène (complète ?).");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "Vous avez rejoint l'arène " + ChatColor.GOLD + arena.getName()
                + ChatColor.GREEN + " dans l'équipe " + team.getChatColor() + team.getDisplayName() + ChatColor.GREEN + " !");
    }

    public void leaveArena(Player player) {
        Arena arena = findArenaOf(player);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Vous n'êtes dans aucune arène.");
            return;
        }
        arena.removePlayer(player);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        if (arenaManager.getLobbySpawn() != null) {
            player.teleport(arenaManager.getLobbySpawn());
        }
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.sendMessage(ChatColor.YELLOW + "Vous avez quitté l'arène.");
    }

    public ArenaManager getArenaManager() { return arenaManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public SidebarManager getSidebarManager() { return sidebarManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public ArenaSelectorGUI getArenaSelectorGUI() { return arenaSelectorGUI; }

    public NamespacedKey getKitKey() { return kitKey; }
    public NamespacedKey getWandKey() { return wandKey; }
    public NamespacedKey getGuiArenaKey() { return guiArenaKey; }
    public NamespacedKey getGuiRandomKey() { return guiRandomKey; }

    public Map<UUID, Location[]> getSelections() { return selections; }
}
