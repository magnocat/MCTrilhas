package com.magnocat.godmode.badges;

/**
 * Enum que define os tipos de progresso para as insígnias.
 * Cada tipo corresponde a uma ação que o jogador pode realizar.
 */
public enum BadgeType {
    LUMBERJACK("Lenhador"),
    MINING("Mineração"),
    COOKING("Culinária"),
    BUILDER("Construção"),
    FISHING("Pesca");

    private final String name;

    BadgeType(String name) {
        this.name = name;
    }

    public String getName() { return name; }
}