package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class WandListener implements Listener {

    private final BlitzPlugin plugin;

    public WandListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(plugin.getWandKey(), PersistentDataType.STRING)) return;

        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;
        Location loc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.getSelections().computeIfAbsent(player.getUniqueId(), k -> new Location[2])[0] = loc;
            player.sendMessage(ChatColor.GREEN + "Position 1 définie: " + ChatColor.WHITE
                    + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.getSelections().computeIfAbsent(player.getUniqueId(), k -> new Location[2])[1] = loc;
            player.sendMessage(ChatColor.GREEN + "Position 2 définie: " + ChatColor.WHITE
                    + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        }
    }
}
