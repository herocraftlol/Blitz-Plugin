package fr.herocraft.blitz.listener;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.ArenaState;
import fr.herocraft.blitz.storage.PlayerStats;
import fr.herocraft.blitz.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

/**
 * Gestion du combat :
 * - Pas de dégâts équipiers.
 * - À la mort : clear inventaire, donner le kit de base.
 * - Épée en pierre dans l'inventaire : remplace l'épée en bois de kit.
 * - Si on jette l'épée en pierre (ou toute épée ≥ pierre) et qu'il n'en reste plus,
 *   l'épée en bois du kit est redonnée automatiquement.
 */
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
            // À la mort : reset complet de l'inventaire + kit de base
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            arena.giveKit(player, team);
            plugin.getSidebarManager().update(arena);
        });
    }

    /**
     * Lorsque le joueur jette un item :
     * - Si c'est un item de kit (épée en bois), on bloque le drop.
     * - Si c'est une épée en pierre (ramassée dans un coffre), on autorise le drop,
     *   mais on vérifie ensuite si le joueur n'a plus d'épée ≥ pierre
     *   → dans ce cas on redonne l'épée en bois.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.findArenaOf(player);
        if (arena == null || arena.getState() != ArenaState.PLAYING) return;

        ItemStack dropped = event.getItemDrop().getItemStack();

        // Bloquer le drop des items de kit
        if (isKitItem(dropped)) {
            event.setCancelled(true);
            return;
        }

        // Si le joueur jette une épée de coffre (pierre ou mieux), vérifier
        // s'il faut redonner l'épée en bois.
        if (isStoneSword(dropped)) {
            // Vérifier après le drop (tick suivant)
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!arena.equals(plugin.findArenaOf(player))) return;
                PlayerInventory inv = player.getInventory();
                if (!Arena.hasStoneOrBetterSword(inv)) {
                    // Plus d'épée en pierre : redonner l'épée en bois
                    Team team = arena.getTeam(player);
                    if (team != null) {
                        ItemStack woodSword = new org.bukkit.inventory.ItemStack(Material.WOODEN_SWORD);
                        // Marquer comme kit
                        org.bukkit.inventory.meta.ItemMeta meta = woodSword.getItemMeta();
                        if (meta != null) {
                            meta.getPersistentDataContainer().set(plugin.getKitKey(), PersistentDataType.STRING, "kit");
                            meta.setUnbreakable(true);
                            woodSword.setItemMeta(meta);
                        }
                        inv.addItem(woodSword);
                    }
                }
            });
        }
    }

    private boolean isKitItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getKitKey(), PersistentDataType.STRING);
    }

    private boolean isStoneSword(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m == Material.STONE_SWORD || m == Material.IRON_SWORD
                || m == Material.GOLDEN_SWORD || m == Material.DIAMOND_SWORD
                || m == Material.NETHERITE_SWORD;
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }
}
