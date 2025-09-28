package com.magnocat.mctrilhas.ranks;

import org.bukkit.ChatColor;

public enum Rank {
    VISITANTE("Visitante", ChatColor.GRAY),
    FILHOTE("Filhote", ChatColor.WHITE),
    LOBINHO("Lobinho", ChatColor.YELLOW),
    ESCOTEIRO("Escoteiro", ChatColor.GREEN),
    SENIOR("Sênior", ChatColor.BLUE),
    PIONEIRO("Pioneiro", ChatColor.DARK_PURPLE),
    CHEFE("Chefe", ChatColor.RED);

    private final String displayName;
    private final ChatColor color;

    Rank(String displayName, ChatColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public Rank getNext() {
        // O ranque CHEFE não faz parte da progressão linear
        if (this == CHEFE || this == PIONEIRO) {
            return null;
        }
        return values()[this.ordinal() + 1];
    }

    public static Rank fromString(String text) {
        if (text != null) {
            for (Rank r : Rank.values()) {
                if (text.equalsIgnoreCase(r.name())) {
                    return r;
                }
            }
        }
        // Retorna VISITANTE como padrão se o ranque for nulo ou inválido
        return VISITANTE;
    }
}