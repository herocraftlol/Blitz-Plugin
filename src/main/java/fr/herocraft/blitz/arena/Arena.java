package fr.herocraft.blitz.arena;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.storage.PlayerStats;
import fr.herocraft.blitz.team.Team;
import fr.herocraft.blitz.util.ItemBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class Arena {

    private final BlitzPlugin plugin;
    private String name;

    private Location redSpawn;
    private Location blueSpawn;
    private Location arenaLobbySpawn;
    private CuboidRegion redGoal;
    private CuboidRegion blueGoal;
    private CuboidRegion resetRegion;

    /**
     * Coffres de l'arène : chaque entrée contient la location du coffre,
     * l'équipe propriétaire, un numéro d'index et le contenu sauvegardé.
     */
    private final List<ChestEntry> teamChests = new ArrayList<>();

    private int maxPerTeam = 8;
    private int scoreLimit = 5;

    private ArenaState state = ArenaState.WAITING;
    private final Map<UUID, Team> players = new LinkedHashMap<>();
    private final Map<Team, Integer> scores = new EnumMap<>(Team.class);
    private final Set<String> placedBlocks = new HashSet<>();

    private final Map<UUID, Location> preLobbyLocations = new HashMap<>();

    private RegionSnapshot snapshot;
    private long gameStartMillis;
    private int countdownSecondsLeft = -1;

    private BukkitTask countdownTask;
    private BukkitTask chestRefillTask;
    private BukkitTask restartTask;

    // Spawner de lingots
    private final List<IronSpawner> ironSpawners = new ArrayList<>();
    private BukkitTask ironSpawnerTask;

    // Clés PDC lobby
    private static final String LEAVE_TAG   = "blitz_lobby_leave";
    private static final String FSTART_TAG  = "blitz_lobby_forcestart";
    private static final String TEAM_R_TAG  = "blitz_team_red";
    private static final String TEAM_B_TAG  = "blitz_team_blue";

    // ---- Classe interne pour les coffres d'équipe ----
    public static class ChestEntry {
        public Location location;
        public Team team;
        public int index; // numéro affiché (1, 2, 3…)
        public ItemStack[] savedContents; // contenu enregistré au moment du addchest

        public ChestEntry(Location location, Team team, int index, ItemStack[] savedContents) {
            this.location = location;
            this.team = team;
            this.index = index;
            this.savedContents = savedContents;
        }
    }

    // ---- Classe interne pour les spawners de lingots ----
    public static class IronSpawner {
        public Location location;
        public IronSpawner(Location location) { this.location = location; }
    }

    public Arena(BlitzPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        scores.put(Team.RED, 0);
        scores.put(Team.BLUE, 0);
    }

    // ---------- Getters / setters ----------

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Location getRedSpawn() { return redSpawn; }
    public void setRedSpawn(Location redSpawn) { this.redSpawn = redSpawn; }

    public Location getBlueSpawn() { return blueSpawn; }
    public void setBlueSpawn(Location blueSpawn) { this.blueSpawn = blueSpawn; }

    public Location getArenaLobbySpawn() { return arenaLobbySpawn; }
    public void setArenaLobbySpawn(Location loc) { this.arenaLobbySpawn = loc; }

    public CuboidRegion getRedGoal() { return redGoal; }
    public void setRedGoal(CuboidRegion redGoal) { this.redGoal = redGoal; }

    public CuboidRegion getBlueGoal() { return blueGoal; }
    public void setBlueGoal(CuboidRegion blueGoal) { this.blueGoal = blueGoal; }

    public CuboidRegion getResetRegion() { return resetRegion; }
    public void setResetRegion(CuboidRegion resetRegion) {
        this.resetRegion = resetRegion;
        this.snapshot = new RegionSnapshot(resetRegion);
    }

    public void clearResetRegion() {
        this.resetRegion = null;
        this.snapshot = null;
    }

    /** @deprecated Utiliser {@link #getTeamChests()} */
    public List<Location> getChestLocations() {
        List<Location> locs = new ArrayList<>();
        for (ChestEntry e : teamChests) locs.add(e.location);
        return locs;
    }

    public List<ChestEntry> getTeamChests() { return teamChests; }

    public List<IronSpawner> getIronSpawners() { return ironSpawners; }

    public int getMaxPerTeam() { return maxPerTeam; }
    public void setMaxPerTeam(int maxPerTeam) { this.maxPerTeam = maxPerTeam; }

    public int getScoreLimit() { return scoreLimit; }
    public void setScoreLimit(int scoreLimit) { this.scoreLimit = scoreLimit; }

    public ArenaState getState() { return state; }

    public World getWorld() {
        if (redSpawn != null) return redSpawn.getWorld();
        if (blueSpawn != null) return blueSpawn.getWorld();
        return null;
    }

    public boolean isReady() {
        return redSpawn != null && blueSpawn != null && redGoal != null
                && blueGoal != null && resetRegion != null;
    }

    public int getScore(Team team) { return scores.getOrDefault(team, 0); }

    public Map<UUID, Team> getPlayers() { return players; }

    public int getPlayerCount() { return players.size(); }

    public int getTeamCount(Team team) {
        int c = 0;
        for (Team t : players.values()) if (t == team) c++;
        return c;
    }

    public long getElapsedSeconds() {
        if (state != ArenaState.PLAYING) return 0;
        return (System.currentTimeMillis() - gameStartMillis) / 1000L;
    }

    public int getCountdownSecondsLeft() { return countdownSecondsLeft; }

    // ---------- Gestion des joueurs ----------

    public boolean canJoin() {
        return state == ArenaState.WAITING || state == ArenaState.STARTING;
    }

    public Team addPlayer(Player player) {
        if (!canJoin() || !isReady()) return null;
        if (getTeamCount(Team.RED) >= maxPerTeam && getTeamCount(Team.BLUE) >= maxPerTeam) return null;

        Team team;
        if (getTeamCount(Team.RED) <= getTeamCount(Team.BLUE) && getTeamCount(Team.RED) < maxPerTeam) {
            team = Team.RED;
        } else {
            team = Team.BLUE;
        }
        players.put(player.getUniqueId(), team);
        preLobbyLocations.put(player.getUniqueId(), player.getLocation().clone());

        Location dest = arenaLobbySpawn != null ? arenaLobbySpawn
                : (team == Team.RED ? redSpawn : blueSpawn);
        player.teleport(dest);
        prepareLobbyPlayer(player, team);

        broadcast(ChatColor.GRAY + player.getName() + " a rejoint l'équipe " + team.getChatColor() + team.getDisplayName()
                + ChatColor.GRAY + " (" + getPlayerCount() + "/" + (maxPerTeam * 2) + ")");

        plugin.getSidebarManager().update(this);
        maybeStartCountdown();
        return team;
    }

    public void removePlayer(Player player) {
        Team team = players.remove(player.getUniqueId());
        if (team == null) return;

        plugin.getSidebarManager().clear(player);

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.ADVENTURE);

        Location preLobby = preLobbyLocations.remove(player.getUniqueId());
        if (preLobby != null) {
            player.teleport(preLobby);
        } else if (plugin.getArenaManager().getLobbySpawn() != null) {
            player.teleport(plugin.getArenaManager().getLobbySpawn());
        }

        if (state == ArenaState.PLAYING) {
            if (getTeamCount(team) == 0 && getPlayerCount() > 0) {
                endGame(team.opposite());
            }
        } else if (state == ArenaState.STARTING) {
            if (getTeamCount(Team.RED) == 0 || getTeamCount(Team.BLUE) == 0) {
                cancelCountdown();
            }
        }
        plugin.getSidebarManager().update(this);
    }

    public Team getTeam(Player player) { return players.get(player.getUniqueId()); }

    public boolean changeTeam(Player player, Team newTeam) {
        if (state == ArenaState.PLAYING) return false;
        Team old = players.get(player.getUniqueId());
        if (old == null || old == newTeam) return false;
        if (getTeamCount(newTeam) >= maxPerTeam) {
            player.sendMessage(ChatColor.RED + "Cette équipe est pleine !");
            return false;
        }
        players.put(player.getUniqueId(), newTeam);
        player.getInventory().setItem(4, buildTeamSelectorItem(newTeam));
        plugin.getSidebarManager().update(this);
        return true;
    }

    private void prepareLobbyPlayer(Player player, Team team) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        player.getInventory().setItem(2, buildTeamSelectorItem(team));

        if (player.hasPermission("blitz.admin")) {
            player.getInventory().setItem(0, buildForceStartItem());
        }

        player.getInventory().setItem(8, buildLeaveItem());
    }

    // ---- Items de lobby ----

    private ItemStack buildLeaveItem() {
        return new ItemBuilder(Material.BARRIER)
                .name(ChatColor.RED + "" + ChatColor.BOLD + "Quitter l'arène")
                .lore(List.of(ChatColor.GRAY + "Cliquez pour quitter et retourner",
                        ChatColor.GRAY + "à votre position précédente."))
                .tag(plugin.getLobbyLeaveKey(), LEAVE_TAG)
                .build();
    }

    private ItemStack buildForceStartItem() {
        return new ItemBuilder(Material.DIAMOND)
                .name(ChatColor.AQUA + "" + ChatColor.BOLD + "Forcer le démarrage")
                .lore(List.of(ChatColor.GRAY + "Cliquez pour lancer la partie immédiatement."))
                .tag(plugin.getLobbyForceStartKey(), FSTART_TAG)
                .build();
    }

    private ItemStack buildTeamSelectorItem(Team team) {
        Material mat = team == Team.RED ? Material.RED_TERRACOTTA : Material.BLUE_TERRACOTTA;
        String tag = team == Team.RED ? TEAM_R_TAG : TEAM_B_TAG;
        return new ItemBuilder(mat)
                .name(team.getChatColor() + "" + ChatColor.BOLD + team.getDisplayName()
                        + ChatColor.WHITE + " - Changer d'équipe")
                .lore(List.of(
                        ChatColor.GRAY + "Équipe actuelle : " + team.getChatColor() + team.getDisplayName(),
                        "",
                        ChatColor.YELLOW + "Cliquez pour rejoindre l'autre équipe !"))
                .tag(plugin.getLobbyTeamKey(), tag)
                .build();
    }

    public static boolean isLeaveItem(BlitzPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.getLobbyLeaveKey(), PersistentDataType.STRING);
    }

    public static boolean isForceStartItem(BlitzPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.getLobbyForceStartKey(), PersistentDataType.STRING);
    }

    public static boolean isTeamSelectorItem(BlitzPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.getLobbyTeamKey(), PersistentDataType.STRING);
    }

    public static Team getTeamFromSelectorItem(BlitzPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String val = item.getItemMeta().getPersistentDataContainer().get(plugin.getLobbyTeamKey(), PersistentDataType.STRING);
        if (TEAM_R_TAG.equals(val)) return Team.RED;
        if (TEAM_B_TAG.equals(val)) return Team.BLUE;
        return null;
    }

    private void maybeStartCountdown() {
        if (state != ArenaState.WAITING) return;
        if (getTeamCount(Team.RED) >= 1 && getTeamCount(Team.BLUE) >= 1) {
            startCountdown();
        }
    }

    private void startCountdown() {
        state = ArenaState.STARTING;
        countdownSecondsLeft = plugin.getConfig().getInt("countdown-seconds", 15);
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (countdownSecondsLeft <= 0) {
                cancelCountdown();
                startGame();
                return;
            }
            if (countdownSecondsLeft <= 5 || countdownSecondsLeft % 5 == 0) {
                broadcast(ChatColor.YELLOW + "Début de la partie dans " + ChatColor.GOLD + countdownSecondsLeft
                        + ChatColor.YELLOW + " seconde(s)...");
            }
            countdownSecondsLeft--;
        }, 0L, 20L);
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        countdownSecondsLeft = -1;
        if (state == ArenaState.STARTING) {
            state = ArenaState.WAITING;
            broadcast(ChatColor.RED + "Pas assez de joueurs – le compte à rebours est annulé.");
        }
    }

    // ---------- Cycle de partie ----------

    public void startGame() {
        if (snapshot == null) snapshot = new RegionSnapshot(resetRegion);
        if (!snapshot.isCaptured()) snapshot.capture();

        state = ArenaState.PLAYING;
        scores.put(Team.RED, 0);
        scores.put(Team.BLUE, 0);
        placedBlocks.clear();
        gameStartMillis = System.currentTimeMillis();

        for (UUID uuid : players.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                respawnPlayer(p);
            }
        }

        restoreAllChests();
        chestRefillTask = Bukkit.getScheduler().runTaskTimer(plugin,
                this::restoreAllChests,
                20L * plugin.getConfig().getInt("chest-refill-seconds", 600),
                20L * plugin.getConfig().getInt("chest-refill-seconds", 600));

        startIronSpawners();

        broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "La partie commence ! Premier à " + scoreLimit + " points !");
        plugin.getSidebarManager().update(this);
    }

    public void respawnPlayer(Player player) {
        Team team = getTeam(player);
        if (team == null) return;
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setFireTicks(0);
        player.teleport(team == Team.RED ? redSpawn : blueSpawn);
        giveKit(player, team);
        player.setHealth(20.0);
        player.setFoodLevel(20);
    }

    /**
     * Donne le kit de base. Appelé uniquement à la mort (respawn)
     * et au début de partie. Pas appelé lors d'un but.
     */
    public void giveKit(Player player, Team team) {
        PlayerInventory inv = player.getInventory();
        NamespacedKey key = plugin.getKitKey();

        ItemStack helmet = new ItemBuilder(Material.LEATHER_HELMET).unbreakable(true).leatherColor(team.getArmorColor()).tag(key, "kit").build();
        ItemStack chest  = new ItemBuilder(Material.LEATHER_CHESTPLATE).unbreakable(true).leatherColor(team.getArmorColor()).tag(key, "kit").build();
        ItemStack legs   = new ItemBuilder(Material.LEATHER_LEGGINGS).unbreakable(true).leatherColor(team.getArmorColor()).tag(key, "kit").build();
        ItemStack boots  = new ItemBuilder(Material.LEATHER_BOOTS).unbreakable(true).leatherColor(team.getArmorColor()).tag(key, "kit").build();
        // Épée en bois de base
        ItemStack sword  = new ItemBuilder(Material.WOODEN_SWORD).unbreakable(true).tag(key, "kit").build();

        inv.setHelmet(helmet);
        inv.setChestplate(chest);
        inv.setLeggings(legs);
        inv.setBoots(boots);
        // Ajouter l'épée seulement si pas déjà une épée en pierre dans l'inventaire
        if (!hasStoneOrBetterSword(inv)) {
            inv.addItem(sword);
        }
    }

    /** Vérifie si le joueur possède déjà une épée en pierre (ou mieux) dans l'inventaire. */
    public static boolean hasStoneOrBetterSword(PlayerInventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            Material m = item.getType();
            if (m == Material.STONE_SWORD || m == Material.IRON_SWORD
                    || m == Material.GOLDEN_SWORD || m == Material.DIAMOND_SWORD
                    || m == Material.NETHERITE_SWORD) {
                return true;
            }
        }
        return false;
    }

    /**
     * Appelé lors d'un but. L'inventaire du joueur n'est PAS réinitialisé,
     * seule la téléportation et la vie/faim sont restaurées.
     */
    public void scorePoint(Team scoringTeam, Player scorer) {
        scores.merge(scoringTeam, 1, Integer::sum);
        broadcast(ChatColor.AQUA + "" + ChatColor.BOLD + "BUT ! " + ChatColor.RESET + scoringTeam.getChatColor()
                + scorer.getName() + ChatColor.GRAY + " marque pour l'équipe " + scoringTeam.getChatColor()
                + scoringTeam.getDisplayName() + ChatColor.GRAY + " (" + scores.get(scoringTeam) + "/" + scoreLimit + ")");

        // Téléporter au spawn sans réinitialiser l'inventaire
        Team team = getTeam(scorer);
        if (team != null) {
            scorer.teleport(team == Team.RED ? redSpawn : blueSpawn);
            scorer.setHealth(20.0);
            scorer.setFoodLevel(20);
            scorer.setFireTicks(0);
        }

        plugin.getSidebarManager().update(this);
        if (scores.get(scoringTeam) >= scoreLimit) {
            endGame(scoringTeam);
        }
    }

    public void endGame(Team winner) {
        if (state != ArenaState.PLAYING) return;
        state = ArenaState.RESTARTING;
        if (chestRefillTask != null) { chestRefillTask.cancel(); chestRefillTask = null; }
        stopIronSpawners();
        cancelCountdown();

        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "L'équipe " + winner.getChatColor() + winner.getDisplayName()
                + ChatColor.GOLD + ChatColor.BOLD + " remporte la partie !");

        for (Map.Entry<UUID, Team> entry : players.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            String name2 = p != null ? p.getName() : null;
            PlayerStats stats = plugin.getStatsManager().get(entry.getKey(), name2 == null ? entry.getKey().toString() : name2);
            stats.addPlayed();
            if (entry.getValue() == winner) stats.addWin();
        }
        plugin.getStatsManager().save();

        int restartSeconds = plugin.getConfig().getInt("restart-seconds", 8);
        restartTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : new ArrayList<>(players.keySet())) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    plugin.getSidebarManager().clear(p);
                    p.getInventory().clear();
                    p.getInventory().setArmorContents(null);
                    p.setGameMode(GameMode.ADVENTURE);
                    Location preLobby = preLobbyLocations.remove(uuid);
                    if (preLobby != null) {
                        p.teleport(preLobby);
                    } else if (plugin.getArenaManager().getLobbySpawn() != null) {
                        p.teleport(plugin.getArenaManager().getLobbySpawn());
                    }
                }
            }
            players.clear();
            preLobbyLocations.clear();
            scores.put(Team.RED, 0);
            scores.put(Team.BLUE, 0);
            if (snapshot != null && snapshot.isCaptured()) {
                snapshot.restore(plugin, () -> state = ArenaState.WAITING);
            } else {
                state = ArenaState.WAITING;
            }
        }, restartSeconds * 20L);
    }

    // ---------- Blocs ----------

    public boolean isInResetRegion(Location loc) {
        return resetRegion != null && resetRegion.contains(loc);
    }

    public void markPlaced(Block block) { placedBlocks.add(key(block)); }
    public void unmarkPlaced(Block block) { placedBlocks.remove(key(block)); }
    public boolean isPlacedByPlayer(Block block) { return placedBlocks.contains(key(block)); }

    private String key(Block block) {
        return block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }

    // ---------- Coffres d'équipe ----------

    /**
     * Ajoute un coffre enregistré avec son équipe et son numéro.
     * Le contenu actuel du coffre est sauvegardé comme loot fixe.
     */
    public ChestEntry addTeamChest(Location loc, Team team) {
        // Calculer le prochain index pour cette équipe
        int nextIndex = 1;
        for (ChestEntry e : teamChests) {
            if (e.team == team && e.index >= nextIndex) nextIndex = e.index + 1;
        }
        // Lire le contenu actuel du coffre
        ItemStack[] contents = readChestContents(loc);
        ChestEntry entry = new ChestEntry(loc.clone(), team, nextIndex, contents);
        teamChests.add(entry);
        return entry;
    }

    private ItemStack[] readChestContents(Location loc) {
        if (loc.getWorld() == null) return new ItemStack[0];
        Block block = loc.getBlock();
        if (!(block.getState() instanceof Chest chestState)) return new ItemStack[0];
        Inventory inv = chestState.getBlockInventory();
        ItemStack[] contents = inv.getContents();
        // Copier pour éviter les références
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = (contents[i] != null) ? contents[i].clone() : null;
        }
        return copy;
    }

    /** Restaure tous les coffres avec leur contenu sauvegardé. */
    public void restoreAllChests() {
        for (ChestEntry entry : teamChests) {
            restoreChest(entry);
        }
    }

    private void restoreChest(ChestEntry entry) {
        Location loc = entry.location;
        if (loc.getWorld() == null) return;
        Block block = loc.getBlock();
        if (!(block.getState() instanceof Chest chestState)) return;
        Inventory inv = chestState.getBlockInventory();
        inv.clear();
        if (entry.savedContents != null) {
            for (int i = 0; i < entry.savedContents.length && i < inv.getSize(); i++) {
                if (entry.savedContents[i] != null) {
                    inv.setItem(i, entry.savedContents[i].clone());
                }
            }
        }
    }

    /** Retourne l'équipe propriétaire d'un coffre à cette location (null si pas enregistré). */
    public Team getChestTeam(Location loc) {
        for (ChestEntry e : teamChests) {
            if (isSameBlock(e.location, loc)) return e.team;
        }
        return null;
    }

    /** Retourne le ChestEntry pour une location donnée. */
    public ChestEntry getChestEntry(Location loc) {
        for (ChestEntry e : teamChests) {
            if (isSameBlock(e.location, loc)) return e;
        }
        return null;
    }

    private boolean isSameBlock(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return false;
        return a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    // ---------- Spawners de lingots ----------

    public IronSpawner addIronSpawner(Location loc) {
        IronSpawner spawner = new IronSpawner(loc.clone());
        ironSpawners.add(spawner);
        return spawner;
    }

    private void startIronSpawners() {
        if (ironSpawners.isEmpty()) return;
        // Spawner toutes les 60 secondes (1200 ticks)
        ironSpawnerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (IronSpawner spawner : ironSpawners) {
                if (spawner.location.getWorld() != null) {
                    spawner.location.getWorld().dropItemNaturally(spawner.location, new ItemStack(Material.IRON_INGOT));
                }
            }
        }, 1200L, 1200L);
    }

    private void stopIronSpawners() {
        if (ironSpawnerTask != null) {
            ironSpawnerTask.cancel();
            ironSpawnerTask = null;
        }
    }

    // ---------- Divers ----------

    public void broadcast(String message) {
        for (UUID uuid : players.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Blitz" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET + message);
        }
    }

    public void forceStart() {
        cancelCountdown();
        startGame();
    }

    public void shutdown() {
        if (countdownTask != null) countdownTask.cancel();
        if (chestRefillTask != null) chestRefillTask.cancel();
        if (restartTask != null) restartTask.cancel();
        stopIronSpawners();
    }
}
