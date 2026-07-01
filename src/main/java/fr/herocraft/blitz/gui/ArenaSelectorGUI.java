package fr.herocraft.blitz.gui;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import fr.herocraft.blitz.team.Team;
import fr.herocraft.blitz.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GUI de sélection d'arène, inspiré du style HikaBrain :
 * - Lignes 1–5 : une icône par arène (colorées selon l'état)
 * - Slots vides remplis par du verre noir
 * - Ligne 6 entière : bouton « Rejoindre une arène aléatoire »
 */
public class ArenaSelectorGUI {

    public static final String TITLE = ChatColor.DARK_GRAY + "⚔ " + ChatColor.GOLD + ChatColor.BOLD + "Blitz " + ChatColor.DARK_GRAY + "- Choisir une arène";
    private static final int GUI_SIZE        = 54;
    private static final int RANDOM_ROW_START = 45;

    private final BlitzPlugin plugin;

    public ArenaSelectorGUI(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        player.openInventory(buildInventory());
    }

    public Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, TITLE);

        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getAll());
        arenas.sort(Comparator.comparing(Arena::getName));

        int slot = 0;
        for (Arena arena : arenas) {
            if (slot >= RANDOM_ROW_START) break;
            inv.setItem(slot++, buildArenaItem(arena));
        }

        // Remplir les slots vides (lignes 1-5) avec du verre noir
        ItemStack filler = buildFiller();
        for (int i = slot; i < RANDOM_ROW_START; i++) inv.setItem(i, filler);

        // Ligne 6 entière : bouton aléatoire
        ItemStack randomBtn = buildRandomButton(arenas);
        for (int i = RANDOM_ROW_START; i < GUI_SIZE; i++) inv.setItem(i, randomBtn);

        return inv;
    }

    private ItemStack buildArenaItem(Arena arena) {
        Material mat;
        String stateLine;
        String prefix;

        switch (arena.getState()) {
            case WAITING -> {
                mat = arena.isReady() ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                stateLine = arena.isReady() ? ChatColor.GREEN + "✔ En attente de joueurs" : ChatColor.GRAY + "✖ Non configurée";
                prefix = arena.isReady() ? ChatColor.GREEN + "" + ChatColor.BOLD + "✔ " : ChatColor.GRAY + "" + ChatColor.BOLD + "✖ ";
            }
            case STARTING -> {
                mat = Material.YELLOW_STAINED_GLASS_PANE;
                stateLine = ChatColor.YELLOW + "⌛ Démarrage imminent";
                prefix = ChatColor.YELLOW + "" + ChatColor.BOLD + "⌛ ";
            }
            case PLAYING -> {
                mat = Material.RED_STAINED_GLASS_PANE;
                stateLine = ChatColor.RED + "⚔ Partie en cours";
                prefix = ChatColor.RED + "" + ChatColor.BOLD + "⚔ ";
            }
            default -> {
                mat = Material.GRAY_STAINED_GLASS_PANE;
                stateLine = ChatColor.GRAY + "↻ Redémarrage…";
                prefix = ChatColor.GRAY + "" + ChatColor.BOLD + "↻ ";
            }
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(stateLine);
        lore.add(ChatColor.GRAY + "Joueurs : " + ChatColor.WHITE + arena.getPlayerCount() + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + (arena.getMaxPerTeam() * 2));
        lore.add(Team.RED.getChatColor() + "Rouges : " + arena.getTeamCount(Team.RED)
                + "  " + Team.BLUE.getChatColor() + "Bleus : " + arena.getTeamCount(Team.BLUE));
        lore.add(ChatColor.GRAY + "Score pour gagner : " + ChatColor.WHITE + arena.getScoreLimit());
        lore.add("");

        boolean joinable = arena.isReady() && arena.canJoin() && arena.getPlayerCount() < arena.getMaxPerTeam() * 2;
        lore.add(joinable ? ChatColor.YELLOW + "▶ Cliquez pour rejoindre !" : ChatColor.RED + "✖ Indisponible");

        return new ItemBuilder(mat)
                .name(prefix + capitalize(arena.getName()))
                .lore(lore)
                .tag(plugin.getGuiArenaKey(), arena.getName())
                .build();
    }

    private ItemStack buildRandomButton(List<Arena> arenas) {
        long joinable = arenas.stream()
                .filter(a -> a.isReady() && a.canJoin() && a.getPlayerCount() < a.getMaxPerTeam() * 2)
                .count();

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Vous serez envoyé dans l'arène");
        lore.add(ChatColor.GRAY + "la plus active avec de la place.");
        lore.add("");
        if (joinable > 0) {
            lore.add(ChatColor.GREEN + "" + joinable + " arène(s) disponible(s)");
            lore.add("");
            lore.add(ChatColor.YELLOW + "▶ Cliquez pour jouer !");
        } else {
            lore.add(ChatColor.RED + "Aucune arène disponible.");
        }

        return new ItemBuilder(Material.NETHER_STAR)
                .name(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ Rejoindre une arène aléatoire")
                .lore(lore)
                .tag(plugin.getGuiRandomKey(), "random")
                .build();
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** Retourne le nom d'arène d'un item du GUI (null si filler ou bouton random). */
    public static String getArenaName(BlitzPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(plugin.getGuiArenaKey(), org.bukkit.persistence.PersistentDataType.STRING);
    }

    public static boolean isRandomItem(BlitzPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getGuiRandomKey(), org.bukkit.persistence.PersistentDataType.STRING);
    }
}
