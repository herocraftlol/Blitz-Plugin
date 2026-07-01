package fr.herocraft.blitz.arena;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.storage.PlayerStats;
import fr.herocraft.blitz.team.Team;
import fr.herocraft.blitz.util.ItemBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Arena {

    private final BlitzPlugin plugin;
    private String name;

    private Location redSpawn;
    private Location blueSpawn;
    private CuboidRegion redGoal;
    private CuboidRegion blueGoal;
    private CuboidRegion resetRegion;
    private final List<Location> chestLocations = new ArrayList<>();

    private int maxPerTeam = 8;
    private int scoreLimit = 5;

    private ArenaState state = ArenaState.WAITING;
    private final Map<UUID, Team> players = new LinkedHashMap<>();
    private final Map<Team, Integer> scores = new EnumMap<>(Team.class);
    private final Set<String> placedBlocks = new HashSet<>(); // "world;x;y;z" des blocs poses par des joueurs

    private RegionSnapshot snapshot;
    private long gameStartMillis;
    private int countdownSecondsLeft = -1;

    private BukkitTask countdownTask;
    private BukkitTask chestRefillTask;
    private BukkitTask restartTask;

    public Arena(BlitzPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        scores.put(Team.RED, 0);
        scores.put(Team.BLUE, 0);
    }

    // ---------- Getters / setters de configuration ----------

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Location getRedSpawn() { return redSpawn; }
    public void setRedSpawn(Location redSpawn) { this.redSpawn = redSpawn; }

    public Location getBlueSpawn() { return blueSpawn; }
    public void setBlueSpawn(Location blueSpawn) { this.blueSpawn = blueSpawn; }

    public CuboidRegion getRedGoal() { return redGoal; }
    public void setRedGoal(CuboidRegion redGoal) { this.redGoal = redGoal; }

    public CuboidRegion getBlueGoal() { return blueGoal; }
    public void setBlueGoal(CuboidRegion blueGoal) { this.blueGoal = blueGoal; }

    public CuboidRegion getResetRegion() { return resetRegion; }
    public void setResetRegion(CuboidRegion resetRegion) {
        this.resetRegion = resetRegion;
        this.snapshot = new RegionSnapshot(resetRegion);
    }

    public List<Location> getChestLocations() { return chestLocations; }

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
        player.teleport(team == Team.RED ? redSpawn : blueSpawn);
        prepareLobbyPlayer(player);

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

        if (state == ArenaState.PLAYING) {
            // Si une equipe se retrouve vide en pleine partie, l'autre gagne par forfait
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

    private void prepareLobbyPlayer(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);
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
        if (state == ArenaState.STARTING) state = ArenaState.WAITING;
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

        fillAllChests();
        chestRefillTask = Bukkit.getScheduler().runTaskTimer(plugin,
                this::fillAllChests,
                20L * plugin.getConfig().getInt("chest-refill-seconds", 600),
                20L * plugin.getConfig().getInt("chest-refill-seconds", 600));

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

    public void giveKit(Player player, Team team) {
        PlayerInventory inv = player.getInventory();
        NamespacedKey key = plugin.getKitKey();

        ItemStack helmet = new ItemBuilder(Material.LEATHER_HELMET).unbreakable(true).leatherColor(team.getArmorColor()).tag(key, "kit").build();
        ItemStack chest = new ItemBuilder(Material.LEATHER_CHESTPLATE).unbreakable(true).leatherColor(team.getArmorColor()).tag(key, "kit").build();
        ItemStack legs = new ItemBuilder(Material.LEATHER_LEGGINGS).unbreakable(true).leatherColor(team.getArmorColor()).tag(key, "kit").build();
        ItemStack boots = new ItemBuilder(Material.LEATHER_BOOTS).unbreakable(true).leatherColor(team.getArmorColor()).tag(key, "kit").build();
        ItemStack sword = new ItemBuilder(Material.WOODEN_SWORD).unbreakable(true).tag(key, "kit").build();

        inv.setHelmet(helmet);
        inv.setChestplate(chest);
        inv.setLeggings(legs);
        inv.setBoots(boots);
        inv.addItem(sword);
    }

    public void endGame(Team winner) {
        if (state != ArenaState.PLAYING) return;
        state = ArenaState.RESTARTING;
        if (chestRefillTask != null) { chestRefillTask.cancel(); chestRefillTask = null; }
        cancelCountdown();

        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "L'équipe " + winner.getChatColor() + winner.getDisplayName()
                + ChatColor.GOLD + ChatColor.BOLD + " remporte la partie !");

        for (Map.Entry<UUID, Team> entry : players.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            String name = p != null ? p.getName() : null;
            PlayerStats stats = plugin.getStatsManager().get(entry.getKey(), name == null ? entry.getKey().toString() : name);
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
                    p.teleport(plugin.getArenaManager().getLobbySpawn());
                    p.setGameMode(GameMode.ADVENTURE);
                    p.getInventory().clear();
                    p.getInventory().setArmorContents(null);
                }
            }
            players.clear();
            scores.put(Team.RED, 0);
            scores.put(Team.BLUE, 0);
            if (snapshot != null && snapshot.isCaptured()) {
                snapshot.restore(plugin, () -> state = ArenaState.WAITING);
            } else {
                state = ArenaState.WAITING;
            }
        }, restartSeconds * 20L);
    }

    public void scorePoint(Team scoringTeam, Player scorer) {
        scores.merge(scoringTeam, 1, Integer::sum);
        broadcast(ChatColor.AQUA + "" + ChatColor.BOLD + "BUT ! " + ChatColor.RESET + scoringTeam.getChatColor()
                + scorer.getName() + ChatColor.GRAY + " marque pour l'équipe " + scoringTeam.getChatColor()
                + scoringTeam.getDisplayName() + ChatColor.GRAY + " (" + scores.get(scoringTeam) + "/" + scoreLimit + ")");
        respawnPlayer(scorer);
        plugin.getSidebarManager().update(this);
        if (scores.get(scoringTeam) >= scoreLimit) {
            endGame(scoringTeam);
        }
    }

    // ---------- Blocs ----------

    public boolean isInResetRegion(Location loc) {
        return resetRegion != null && resetRegion.contains(loc);
    }

    public void markPlaced(Block block) {
        placedBlocks.add(key(block));
    }

    public void unmarkPlaced(Block block) {
        placedBlocks.remove(key(block));
    }

    public boolean isPlacedByPlayer(Block block) {
        return placedBlocks.contains(key(block));
    }

    private String key(Block block) {
        return block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();
    }

    // ---------- Coffres ----------

    public void fillAllChests() {
        for (Location loc : chestLocations) {
            fillChest(loc);
        }
    }

    private void fillChest(Location loc) {
        if (loc.getWorld() == null) return;
        Block block = loc.getBlock();
        if (!(block.getState() instanceof Chest chestState)) return;
        Inventory inv = chestState.getBlockInventory();
        inv.clear();

        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getSize(); i++) slots.add(i);
        Collections.shuffle(slots);

        List<ItemStack> loot = generateLoot();
        for (int i = 0; i < loot.size() && i < slots.size(); i++) {
            inv.setItem(slots.get(i), loot.get(i));
        }
    }

    private List<ItemStack> generateLoot() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<ItemStack> loot = new ArrayList<>();

        loot.add(new ItemStack(Material.STONE_SWORD));
        loot.add(new ItemStack(Material.RED_WOOL, rnd.nextInt(2, 6)));
        loot.add(new ItemStack(Material.BLUE_WOOL, rnd.nextInt(2, 6)));
        loot.add(new ItemStack(Material.RED_TERRACOTTA, rnd.nextInt(4, 9)));
        loot.add(new ItemStack(Material.BLUE_TERRACOTTA, rnd.nextInt(4, 9)));
        loot.add(new ItemStack(Material.BOW));
        loot.add(new ItemStack(Material.ARROW, rnd.nextInt(4, 12)));
        loot.add(new ItemStack(Material.GOLDEN_APPLE, rnd.nextInt(1, 3)));
        loot.add(new ItemStack(Material.COOKED_BEEF, rnd.nextInt(2, 6)));

        if (rnd.nextBoolean()) {
            loot.add(potion(PotionType.SWIFTNESS, false));
        }
        if (rnd.nextBoolean()) {
            loot.add(potion(PotionType.STRENGTH, false));
        }
        if (rnd.nextInt(4) == 0) {
            loot.add(potion(PotionType.HEALING, true));
        }

        Collections.shuffle(loot);
        return loot;
    }

    private ItemStack potion(PotionType type, boolean splash) {
        ItemStack item = new ItemStack(splash ? Material.SPLASH_POTION : Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(type);
            item.setItemMeta(meta);
        }
        return item;
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
    }
}
