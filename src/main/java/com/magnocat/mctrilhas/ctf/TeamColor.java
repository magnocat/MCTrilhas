package com.magnocat.mctrilhas.ctf;

import org.bukkit.ChatColor;
import org.bukkit.Color;

import org.bukkit.Material;
import java.util.*;

public enum TeamColor {
    RED("Vermelho", ChatColor.RED, Color.RED, Material.RED_BANNER),
    BLUE("Azul", ChatColor.BLUE, Color.BLUE, Material.BLUE_BANNER),
    GREEN("Verde", ChatColor.GREEN, Color.GREEN, Material.GREEN_BANNER),
    YELLOW("Amarelo", ChatColor.YELLOW, Color.YELLOW, Material.YELLOW_BANNER),
    AQUA("Ciano", ChatColor.AQUA, Color.AQUA, Material.CYAN_BANNER),
    PURPLE("Roxo", ChatColor.LIGHT_PURPLE, Color.PURPLE, Material.PURPLE_BANNER),
    ORANGE("Laranja", ChatColor.GOLD, Color.ORANGE, Material.ORANGE_BANNER);

    private final String displayName;
    private final ChatColor chatColor;
    private final Color armorColor;
    private final Material bannerMaterial;

    private static final List<TeamColor> VALUES = Collections.unmodifiableList(Arrays.asList(values()));
    private static final int SIZE = VALUES.size();
    private static final Random RANDOM = new Random();

    TeamColor(String displayName, ChatColor chatColor, Color armorColor, Material bannerMaterial) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.armorColor = armorColor;
        this.bannerMaterial = bannerMaterial;
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

    public Material getBannerMaterial() {
        return bannerMaterial;
    }

    /**
     * Retorna uma lista com duas cores de time aleatórias e distintas.
     * @return Uma lista contendo dois enums TeamColor diferentes.
     */
    public static List<TeamColor> getTwoRandomTeams() {
        if (SIZE < 2) {
            throw new IllegalStateException("Não é possível escolher dois times pois há menos de duas cores definidas.");
        }
        List<TeamColor> allColors = new ArrayList<>(VALUES);
        Collections.shuffle(allColors, RANDOM);
        return allColors.subList(0, 2);
    }
}