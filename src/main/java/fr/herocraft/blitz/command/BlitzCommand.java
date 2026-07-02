package fr.herocraft.blitz.command;

import fr.herocraft.blitz.BlitzPlugin;
import fr.herocraft.blitz.arena.Arena;
import fr.herocraft.blitz.arena.CuboidRegion;
import fr.herocraft.blitz.hologram.HologramType;
import fr.herocraft.blitz.storage.PlayerStats;
import fr.herocraft.blitz.team.Team;
import fr.herocraft.blitz.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class BlitzCommand implements CommandExecutor, TabCompleter {

    private final BlitzPlugin plugin;

    private static final List<String> PLAYER_SUBS = List.of("join", "joinrandom", "leave", "gui", "stats");
    private static final List<String> ADMIN_SUBS  = List.of(
            "wand", "create", "delete", "list", "setlobby", "setarenalobby",
            "setspawn", "setregion", "setgoal", "setcapture", "setlimit",
            "setteamsize", "addchest", "forcestart", "reload",
            "delregion", "hologram", "delhologram");

    public BlitzCommand(BlitzPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join"          -> handleJoin(sender, args);
            case "joinrandom"    -> handleJoinRandom(sender);
            case "leave"         -> handleLeave(sender);
            case "gui"           -> handleGui(sender);
            case "stats"         -> handleStats(sender, args);
            case "wand"          -> handleWand(sender);
            case "create"        -> handleCreate(sender, args);
            case "delete"        -> handleDelete(sender, args);
            case "list"          -> handleList(sender);
            case "setlobby"      -> handleSetLobby(sender);
            case "setarenalobby" -> handleSetArenaLobby(sender, args);
            case "setspawn"      -> handleSetSpawn(sender, args);
            case "setregion"     -> handleSetRegion(sender, args);
            case "setgoal", "setcapture" -> handleSetGoal(sender, args);
            case "setlimit"      -> handleSetLimit(sender, args);
            case "setteamsize"   -> handleSetTeamSize(sender, args);
            case "addchest"      -> handleAddChest(sender, args);
            case "forcestart"    -> handleForceStart(sender, args);
            case "reload"        -> handleReload(sender);
            case "delregion"     -> handleDelRegion(sender, args);
            case "hologram"      -> handleHologram(sender, args);
            case "delhologram"   -> handleDelHologram(sender);
            default              -> sendHelp(sender);
        }
        return true;
    }

    // ---------- Commandes joueur ----------

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Commande réservée aux joueurs."); return; }
        if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /blitz join <arène>"); return; }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) { player.sendMessage(ChatColor.RED + "Cette arène n'existe pas."); return; }
        plugin.joinArena(player, arena);
    }

    private void handleJoinRandom(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Commande réservée aux joueurs."); return; }
        Arena best = plugin.getArenaManager().findBestJoinable();
        if (best == null) {
            player.sendMessage(ChatColor.RED + "Aucune arène disponible pour le moment.");
            return;
        }
        plugin.joinArena(player, best);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Commande réservée aux joueurs."); return; }
        plugin.leaveArena(player);
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Commande réservée aux joueurs."); return; }
        plugin.getArenaSelectorGUI().open(player);
    }

    private void handleStats(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length >= 2) {
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /blitz stats <joueur>");
            return;
        }
        PlayerStats stats = plugin.getStatsManager().get(target.getUniqueId(),
                target.getName() != null ? target.getName() : "Inconnu");
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Statistiques Blitz de " + stats.getName() + " ===");
        sender.sendMessage(ChatColor.GRAY + "Victoires: " + ChatColor.WHITE + stats.getWins());
        sender.sendMessage(ChatColor.GRAY + "Parties jouées: " + ChatColor.WHITE + stats.getPlayed());
        sender.sendMessage(ChatColor.GRAY + "Kills: " + ChatColor.WHITE + stats.getKills());
        sender.sendMessage(ChatColor.GRAY + "Morts: " + ChatColor.WHITE + stats.getDeaths());
        sender.sendMessage(ChatColor.GRAY + "K/D: " + ChatColor.WHITE + stats.getKd());
    }

    // ---------- Commandes admin ----------

    private boolean checkAdmin(CommandSender sender) {
        if (!sender.hasPermission("blitz.admin")) {
            sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission d'utiliser cette commande.");
            return false;
        }
        return true;
    }

    private void handleWand(CommandSender sender) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        ItemStack wand = new ItemBuilder(Material.BLAZE_ROD)
                .name(ChatColor.GOLD + "Baguette de sélection Blitz")
                .lore(List.of(ChatColor.GRAY + "Clic gauche: Position 1", ChatColor.GRAY + "Clic droit: Position 2"))
                .tag(plugin.getWandKey(), "wand")
                .build();
        player.getInventory().addItem(wand);
        player.sendMessage(ChatColor.GREEN + "Baguette de sélection reçue.");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!checkAdmin(sender)) return;
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /blitz create <nom>"); return; }
        Arena arena = plugin.getArenaManager().create(args[1]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Une arène avec ce nom existe déjà."); return; }
        sender.sendMessage(ChatColor.GREEN + "Arène " + ChatColor.GOLD + args[1] + ChatColor.GREEN + " créée. "
                + "Configurez-la avec /blitz setarenalobby, setspawn, setgoal, setregion, addchest...");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!checkAdmin(sender)) return;
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /blitz delete <nom>"); return; }
        boolean removed = plugin.getArenaManager().delete(args[1]);
        sender.sendMessage(removed ? ChatColor.GREEN + "Arène supprimée." : ChatColor.RED + "Arène introuvable.");
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Arènes Blitz ===");
        for (Arena arena : plugin.getArenaManager().getAll()) {
            boolean hasArenaLobby = arena.getArenaLobbySpawn() != null;
            sender.sendMessage(ChatColor.YELLOW + "- " + arena.getName()
                    + ChatColor.GRAY + " | état: " + arena.getState()
                    + " | joueurs: " + arena.getPlayerCount() + "/" + (arena.getMaxPerTeam() * 2)
                    + " | lobby propre: " + (hasArenaLobby ? ChatColor.GREEN + "oui" : ChatColor.RED + "non")
                    + ChatColor.GRAY + " | prête: " + (arena.isReady() ? ChatColor.GREEN + "oui" : ChatColor.RED + "non"));
        }
    }

    private void handleSetLobby(CommandSender sender) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        plugin.getArenaManager().setLobbySpawn(player.getLocation());
        sender.sendMessage(ChatColor.GREEN + "Spawn du lobby global défini.");
    }

    /**
     * Définit le spawn du lobby propre à une arène (où les joueurs attendent avant le début).
     * Usage : /blitz setarenalobby <arène>
     */
    private void handleSetArenaLobby(CommandSender sender, String[] args) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /blitz setarenalobby <arène>"); return; }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Arène introuvable."); return; }
        arena.setArenaLobbySpawn(player.getLocation());
        plugin.getArenaManager().save();
        sender.sendMessage(ChatColor.GREEN + "Lobby propre à l'arène " + ChatColor.GOLD + arena.getName()
                + ChatColor.GREEN + " défini à votre position.");
        sender.sendMessage(ChatColor.GRAY + "(Les joueurs qui rejoignent cette arène seront téléportés ici en attente de partie.)");
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /blitz setspawn <red|blue> <arène>"); return; }
        Arena arena = plugin.getArenaManager().get(args[2]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Arène introuvable."); return; }
        Location loc = player.getLocation();
        if (args[1].equalsIgnoreCase("red")) {
            arena.setRedSpawn(loc);
            sender.sendMessage(ChatColor.GREEN + "Spawn rouge défini pour " + arena.getName() + ".");
        } else if (args[1].equalsIgnoreCase("blue")) {
            arena.setBlueSpawn(loc);
            sender.sendMessage(ChatColor.GREEN + "Spawn bleu défini pour " + arena.getName() + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Équipe invalide (red/blue).");
            return;
        }
        plugin.getArenaManager().save();
    }

    private void handleSetRegion(CommandSender sender, String[] args) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /blitz setregion <arène>"); return; }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Arène introuvable."); return; }
        Location[] sel = plugin.getSelections().get(player.getUniqueId());
        if (sel == null || sel[0] == null || sel[1] == null) {
            sender.sendMessage(ChatColor.RED + "Sélectionnez d'abord deux points avec /blitz wand."); return;
        }
        arena.setResetRegion(new CuboidRegion(sel[0], sel[1]));
        plugin.getArenaManager().save();
        sender.sendMessage(ChatColor.GREEN + "Zone de reset définie pour " + arena.getName()
                + ChatColor.GRAY + " (" + arena.getResetRegion().volume() + " blocs).");
    }

    private void handleDelRegion(CommandSender sender, String[] args) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blitz delregion <arène>");
            return;
        }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Arène introuvable."); return; }
        if (arena.getResetRegion() == null) {
            sender.sendMessage(ChatColor.RED + "Cette arène n'a pas de zone de reset définie.");
            return;
        }
        arena.setResetRegion(null);
        plugin.getArenaManager().save();
        sender.sendMessage(ChatColor.GREEN + "Zone de reset supprimée pour " + arena.getName() + ".");
    }

    /** setgoal ET setcapture sont des alias l'un de l'autre. */
    private void handleSetGoal(CommandSender sender, String[] args) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /blitz setgoal <red|blue> <arène>");
            sender.sendMessage(ChatColor.GRAY + "Alias : /blitz setcapture <red|blue> <arène>");
            return;
        }
        Arena arena = plugin.getArenaManager().get(args[2]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Arène introuvable."); return; }
        Location[] sel = plugin.getSelections().get(player.getUniqueId());
        if (sel == null || sel[0] == null || sel[1] == null) {
            sender.sendMessage(ChatColor.RED + "Sélectionnez d'abord deux points avec /blitz wand."); return;
        }
        CuboidRegion region = new CuboidRegion(sel[0], sel[1]);
        if (args[1].equalsIgnoreCase("red")) {
            arena.setRedGoal(region);
            sender.sendMessage(ChatColor.GREEN + "Zone de capture rouge définie pour " + arena.getName() + ".");
        } else if (args[1].equalsIgnoreCase("blue")) {
            arena.setBlueGoal(region);
            sender.sendMessage(ChatColor.GREEN + "Zone de capture bleue définie pour " + arena.getName() + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Équipe invalide (red/blue)."); return;
        }
        plugin.getArenaManager().save();
    }

    private void handleSetLimit(CommandSender sender, String[] args) {
        if (!checkAdmin(sender)) return;
        if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /blitz setlimit <arène> <score>"); return; }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Arène introuvable."); return; }
        try {
            int limit = Integer.parseInt(args[2]);
            arena.setScoreLimit(limit);
            plugin.getArenaManager().save();
            sender.sendMessage(ChatColor.GREEN + "Score pour gagner défini à " + limit + " pour " + arena.getName() + ".");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Nombre invalide.");
        }
    }

    private void handleSetTeamSize(CommandSender sender, String[] args) {
        if (!checkAdmin(sender)) return;
        if (args.length < 3) { sender.sendMessage(ChatColor.RED + "Usage: /blitz setteamsize <arène> <1-8>"); return; }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Arène introuvable."); return; }
        try {
            int max = Integer.parseInt(args[2]);
            if (max < 1 || max > 8) { sender.sendMessage(ChatColor.RED + "La taille d'équipe doit être entre 1 et 8."); return; }
            arena.setMaxPerTeam(max);
            plugin.getArenaManager().save();
            sender.sendMessage(ChatColor.GREEN + "Taille d'équipe max définie à " + max + " (" + max + "v" + max + ") pour " + arena.getName() + ".");
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Nombre invalide.");
        }
    }

    private void handleAddChest(CommandSender sender, String[] args) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /blitz addchest <arène> (regardez un coffre)"); return; }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Arène introuvable."); return; }
        Block target = player.getTargetBlockExact(6);
        if (target == null || !(target.getState() instanceof Chest)) {
            sender.sendMessage(ChatColor.RED + "Regardez un coffre pour l'ajouter (distance max 6 blocs)."); return;
        }
        arena.getChestLocations().add(target.getLocation());
        plugin.getArenaManager().save();
        sender.sendMessage(ChatColor.GREEN + "Coffre ajouté à l'arène " + arena.getName()
                + ChatColor.GRAY + " (" + arena.getChestLocations().size() + " au total).");
    }

    private void handleForceStart(CommandSender sender, String[] args) {
        if (!checkAdmin(sender)) return;
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /blitz forcestart <arène>"); return; }
        Arena arena = plugin.getArenaManager().get(args[1]);
        if (arena == null) { sender.sendMessage(ChatColor.RED + "Arène introuvable."); return; }
        if (!arena.isReady()) { sender.sendMessage(ChatColor.RED + "Arène non configurée."); return; }
        if (!arena.forceStart()) {
            sender.sendMessage(ChatColor.RED + "Impossible de démarrer : il faut au moins 1 joueur par équipe !");
            return;
        }
        sender.sendMessage(ChatColor.GREEN + "Partie forcée pour " + arena.getName() + ".");
    }

    private void handleReload(CommandSender sender) {
        if (!checkAdmin(sender)) return;
        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configuration rechargée.");
    }

    private void handleHologram(CommandSender sender, String[] args) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /blitz hologram <wins|played|kd|kills>"); return; }
        HologramType type;
        try {
            type = HologramType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Type invalide (wins/played/kd/kills)."); return;
        }
        plugin.getHologramManager().create(type, player.getLocation());
        sender.sendMessage(ChatColor.GREEN + "Hologramme de classement (" + type.getLabel() + ") créé.");
    }

    private void handleDelHologram(CommandSender sender) {
        if (!checkAdmin(sender) || !(sender instanceof Player player)) return;
        boolean removed = plugin.getHologramManager().removeNearest(player.getLocation(), 5);
        sender.sendMessage(removed ? ChatColor.GREEN + "Hologramme supprimé."
                : ChatColor.RED + "Aucun hologramme trouvé à proximité (5 blocs).");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Blitz - HeroCraft ===");
        sender.sendMessage(ChatColor.YELLOW + "/blitz join <arène>" + ChatColor.GRAY + " - Rejoindre une arène (lobby d'attente)");
        sender.sendMessage(ChatColor.YELLOW + "/blitz joinrandom" + ChatColor.GRAY + " - Rejoindre une arène aléatoire");
        sender.sendMessage(ChatColor.YELLOW + "/blitz leave" + ChatColor.GRAY + " - Quitter et retourner à votre position");
        sender.sendMessage(ChatColor.YELLOW + "/blitz gui" + ChatColor.GRAY + " - Ouvrir le menu des arènes");
        sender.sendMessage(ChatColor.YELLOW + "/blitz stats [joueur]" + ChatColor.GRAY + " - Voir des statistiques");
        if (sender.hasPermission("blitz.admin")) {
            sender.sendMessage(ChatColor.AQUA + "--- Administration ---");
            sender.sendMessage(ChatColor.YELLOW + "/blitz wand" + ChatColor.GRAY + " - Baguette de sélection");
            sender.sendMessage(ChatColor.YELLOW + "/blitz create <nom>" + ChatColor.GRAY + " - Créer une arène");
            sender.sendMessage(ChatColor.YELLOW + "/blitz delete <nom>" + ChatColor.GRAY + " - Supprimer une arène");
            sender.sendMessage(ChatColor.YELLOW + "/blitz list" + ChatColor.GRAY + " - Lister les arènes");
            sender.sendMessage(ChatColor.YELLOW + "/blitz setlobby" + ChatColor.GRAY + " - Spawn du lobby global");
            sender.sendMessage(ChatColor.YELLOW + "/blitz setarenalobby <arène>" + ChatColor.GRAY + " - Lobby propre à l'arène (attente avant partie)");
            sender.sendMessage(ChatColor.YELLOW + "/blitz setspawn <red|blue> <arène>" + ChatColor.GRAY + " - Spawn d'équipe (en partie)");
            sender.sendMessage(ChatColor.YELLOW + "/blitz setregion <arène>" + ChatColor.GRAY + " - Zone qui se réinitialise");
            sender.sendMessage(ChatColor.YELLOW + "/blitz delregion <arène>" + ChatColor.GRAY + " - Supprimer la zone de jeu (sans supprimer l'arène)");
            sender.sendMessage(ChatColor.YELLOW + "/blitz setgoal <red|blue> <arène>" + ChatColor.GRAY + " - Zone de capture (alias: setcapture)");
            sender.sendMessage(ChatColor.YELLOW + "/blitz setlimit <arène> <score>" + ChatColor.GRAY + " - Score pour gagner");
            sender.sendMessage(ChatColor.YELLOW + "/blitz setteamsize <arène> <1-8>" + ChatColor.GRAY + " - Taille d'équipe max");
            sender.sendMessage(ChatColor.YELLOW + "/blitz addchest <arène>" + ChatColor.GRAY + " - Ajouter un coffre (regardez-le)");
            sender.sendMessage(ChatColor.YELLOW + "/blitz forcestart <arène>" + ChatColor.GRAY + " - Forcer le début");
            sender.sendMessage(ChatColor.YELLOW + "/blitz hologram <type>" + ChatColor.GRAY + " - Classement hologramme (wins/played/kd/kills)");
            sender.sendMessage(ChatColor.YELLOW + "/blitz delhologram" + ChatColor.GRAY + " - Supprimer l'hologramme proche");
            sender.sendMessage(ChatColor.YELLOW + "/blitz reload" + ChatColor.GRAY + " - Recharger la config");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(PLAYER_SUBS);
            if (sender.hasPermission("blitz.admin")) options.addAll(ADMIN_SUBS);
            return options.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (List.of("join", "delete", "setregion", "delregion", "addchest", "forcestart", "setlimit", "setteamsize", "setarenalobby").contains(sub)) {
                return arenaNames(args[1]);
            }
            if (List.of("setspawn", "setgoal", "setcapture").contains(sub)) {
                return List.of("red", "blue").stream().filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
            }
            if (sub.equals("hologram")) {
                return Arrays.stream(HologramType.values()).map(t -> t.name().toLowerCase(Locale.ROOT))
                        .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
            }
            if (sub.equals("stats")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                        .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (List.of("setspawn", "setgoal", "setcapture").contains(sub)) return arenaNames(args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> arenaNames(String prefix) {
        return plugin.getArenaManager().getAll().stream()
                .map(Arena::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
    }
}
