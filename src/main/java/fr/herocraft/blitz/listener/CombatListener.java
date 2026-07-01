package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import fr.herocraft.blitz.storage.PlayerStats;
import fr.herocraft.blitz.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;

public class CombatListener implements Listener {

    private final BlitzPlugin plugin;

    public CombatListener(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Arena arena = plugin.findArenaOf(victim);
        if (arena == null || arena.getState() != ArenaState.PLAYING) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;
        if (attacker.equals(victim)) return;

        Team victimTeam = arena.getTeam(victim);
        Team attackerTeam = arena.getTeam(attacker);
        if (victimTeam != null && victimTeam == attackerTeam) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Arena arena = plugin.findArenaOf(victim);
        if (arena == null || arena.getState() != ArenaState.PLAYING) return;

        event.getDrops().clear();
        event.setDroppedExp(0);

        PlayerStats victimStats = plugin.getStatsManager().get(victim.getUniqueId(), victim.getName());
        victimStats.addDeath();

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            PlayerStats killerStats = plugin.getStatsManager().get(killer.getUniqueId(), killer.getName());
            killerStats.addKill();
        }
        plugin.getStatsManager().save();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);
        if (arena == null || arena.getState() != ArenaState.PLAYING) return;

        Team team = arena.getTeam(player);
        if (team == null) return;

        event.setRespawnLocation(team == Team.RED ? arena.getRedSpawn() : arena.getBlueSpawn());
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            arena.giveKit(player, team);
            plugin.getSidebarManager().update(arena);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().hasItemMeta()
                && event.getItemDrop().getItemStack().getItemMeta().getPersistentDataContainer()
                .has(plugin.getKitKey(), PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }
}
