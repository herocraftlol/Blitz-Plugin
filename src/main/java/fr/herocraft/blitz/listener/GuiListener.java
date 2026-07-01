package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.gui.ArenaSelectorGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

    private final BlitzPlugin plugin;

    public GuiListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!ChatColor.stripColor(event.getView().getTitle())
                .equals(ChatColor.stripColor(ArenaSelectorGUI.TITLE))) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        if (ArenaSelectorGUI.isRandomItem(plugin, event.getCurrentItem())) {
            player.closeInventory();
            Arena best = plugin.getArenaManager().findBestJoinable();
            if (best != null) {
                plugin.joinArena(player, best);
            } else {
                player.sendMessage(ChatColor.RED + "Aucune arène disponible pour le moment.");
            }
            return;
        }

        String arenaName = ArenaSelectorGUI.getArenaName(plugin, event.getCurrentItem());
        if (arenaName != null) {
            Arena arena = plugin.getArenaManager().get(arenaName);
            if (arena != null) {
                player.closeInventory();
                plugin.joinArena(player, arena);
            }
        }
    }
}
