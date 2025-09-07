package com.magnocat.mctrilhas.ranks;

import java.util.Arrays;

/**
 * Enum que representa os diferentes ranques escoteiros no servidor.
 * A ordem dos enums é importante, pois define a hierarquia de progressão.
 */
public enum Rank {
    FILHOTE("Filhote", "&7"),
    LOBINHO("Lobinho", "&f"),
    ESCOTEIRO("Escoteiro", "&a"),
    SENIOR("Sênior", "&e"),
    PIONEIRO("Pioneiro", "&6"),
    CHEFE("Chefe", "&c&l");

    private final String displayName;
    private final String color;

    Rank(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return color;
    }

    /**
     * Retorna o próximo ranque na hierarquia.
     * @return O próximo ranque, ou o próprio ranque se já for o mais alto (CHEFE).
     */
    public Rank getNext() {
        if (this == CHEFE) {
            return this; // Não há ranque após Chefe.
        }
        return values()[this.ordinal() + 1];
    }

    /**
     * Busca um ranque pelo nome, ignorando maiúsculas/minúsculas.
     * @param name O nome do ranque a ser buscado.
     * @return O Enum Rank correspondente, ou ASPIRANTE como padrão se não for encontrado.
     */
    public static Rank fromString(String name) {
        return Arrays.stream(values()).filter(r -> r.name().equalsIgnoreCase(name)).findFirst().orElse(FILHOTE);
    }
}