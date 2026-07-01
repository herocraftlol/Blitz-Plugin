package fr.herocraft.blitz.gui;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.team.Team;
import fr.herocraft.blitz.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArenaSelectorGUI {

    public static final String TITLE = ChatColor.DARK_GRAY + "Blitz " + ChatColor.GOLD + "- Choisir une arène";

    private final BlitzPlugin plugin;

    public ArenaSelectorGUI(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getAll());
        arenas.sort(Comparator.comparing(Arena::getName));

        int size = Math.max(9, ((arenas.size() + 2) / 9 + 1) * 9);
        size = Math.min(size, 54);
        Inventory inv = org.bukkit.Bukkit.createInventory(null, size, TITLE);

        ItemStack random = new ItemBuilder(Material.NETHER_STAR)
                .name(ChatColor.GOLD + "" + ChatColor.BOLD + "Arène aléatoire")
                .lore(List.of(ChatColor.GRAY + "Rejoindre l'arène la plus active",
                        ChatColor.GRAY + "ayant de la place disponible."))
                .tag(plugin.getGuiRandomKey(), "random")
                .build();
        inv.setItem(0, random);

        int slot = 1;
        for (Arena arena : arenas) {
            if (slot >= size) break;
            inv.setItem(slot++, buildArenaItem(arena));
        }

        player.openInventory(inv);
    }

    private ItemStack buildArenaItem(Arena arena) {
        Material material = switch (arena.getState()) {
            case WAITING -> Material.LIME_DYE;
            case STARTING -> Material.YELLOW_DYE;
            case PLAYING -> Material.RED_DYE;
            case RESTARTING -> Material.GRAY_DYE;
        };

        String stateLabel = switch (arena.getState()) {
            case WAITING -> ChatColor.GREEN + "En attente de joueurs";
            case STARTING -> ChatColor.YELLOW + "Démarrage imminent";
            case PLAYING -> ChatColor.RED + "Partie en cours";
            case RESTARTING -> ChatColor.GRAY + "Redémarrage...";
        };

        List<String> lore = new ArrayList<>();
        lore.add(stateLabel);
        lore.add(ChatColor.GRAY + "Joueurs: " + ChatColor.WHITE + arena.getPlayerCount() + "/" + (arena.getMaxPerTeam() * 2));
        lore.add(Team.RED.getChatColor() + "Rouges: " + arena.getTeamCount(Team.RED) + "  "
                + Team.BLUE.getChatColor() + "Bleus: " + arena.getTeamCount(Team.BLUE));
        lore.add(ChatColor.GRAY + "Score pour gagner: " + ChatColor.WHITE + arena.getScoreLimit());
        lore.add("");
        if (arena.canJoin() && arena.isReady()) {
            lore.add(ChatColor.GREEN + "Cliquez pour rejoindre !");
        } else if (!arena.isReady()) {
            lore.add(ChatColor.RED + "Arène non configurée");
        } else {
            lore.add(ChatColor.RED + "Indisponible pour le moment");
        }

        return new ItemBuilder(material)
                .name(ChatColor.GOLD + "" + ChatColor.BOLD + arena.getName())
                .lore(lore)
                .tag(plugin.getGuiArenaKey(), arena.getName())
                .build();
    }

    public static boolean isRandomItem(BlitzPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(plugin.getGuiRandomKey(), PersistentDataType.STRING);
    }

    public static String getArenaName(BlitzPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().get(plugin.getGuiArenaKey(), PersistentDataType.STRING);
    }
}
