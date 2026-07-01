package fr.herocraft.blitz.team;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;

public enum Team {

    RED("Rouge", ChatColor.RED, Color.RED, Material.RED_TERRACOTTA, Material.RED_WOOL),
    BLUE("Bleu", ChatColor.BLUE, Color.BLUE, Material.BLUE_TERRACOTTA, Material.BLUE_WOOL);

    private final String displayName;
    private final ChatColor chatColor;
    private final Color armorColor;
    private final Material terracotta;
    private final Material wool;

    Team(String displayName, ChatColor chatColor, Color armorColor, Material terracotta, Material wool) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.armorColor = armorColor;
        this.terracotta = terracotta;
        this.wool = wool;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public Color getArmorColor() {
        return armorColor;
    }

    public Material getTerracotta() {
        return terracotta;
    }

    public Material getWool() {
        return wool;
    }

    public Team opposite() {
        return this == RED ? BLUE : RED;
    }
}
