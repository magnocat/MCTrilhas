package com.magnocat.mctrilhas.ranks;

import java.util.Arrays;

/**
 * Enum que representa os diferentes ranques escoteiros no servidor.
 * A ordem das constantes é crucial, pois define a hierarquia de progressão,
 * do menor (`FILHOTE`) ao maior (`CHEFE`).
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

    /**
     * Construtor do enum de ranques.
     * @param displayName O nome de exibição formatado do ranque.
     * @param color O código de cor do chat associado ao ranque.
     */
    Rank(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * @return O nome de exibição formatado (ex: "Escoteiro").
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return O código de cor do chat (ex: "&a").
     */
    public String getColor() {
        return color;
    }

    /**
     * Retorna o próximo ranque na hierarquia.
     * A progressão automática para no ranque PIONEIRO.
     * @return O próximo ranque, ou {@code null} se o ranque atual for o último da linha de progressão automática.
     */
    public Rank getNext() {
        // A progressão automática para em PIONEIRO. CHEFE é um ranque especial.
        if (this == PIONEIRO || this == CHEFE) {
            return null;
        }
        return values()[this.ordinal() + 1];
    }

    /**
     * Busca um ranque pelo nome, ignorando maiúsculas/minúsculas.
     *
     * @param name O nome do ranque a ser buscado.
     * @return O Enum Rank correspondente, ou {@code FILHOTE} como padrão se não for encontrado.
     */
    public static Rank fromString(String name) {
        return Arrays.stream(values()).filter(r -> r.name().equalsIgnoreCase(name)).findFirst().orElse(FILHOTE);
    }
}