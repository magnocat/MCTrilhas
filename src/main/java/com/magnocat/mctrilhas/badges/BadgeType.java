package com.magnocat.mctrilhas.badges;

/**
 * Define as categorias de progresso rastreáveis para as insígnias.
 * <p>
 * Cada constante neste enum representa um tipo de ação que pode ser monitorada
 * (ex: quebrar blocos, pescar, etc.). O nome da constante (ex: {@code MINING})
 * é usado como o identificador interno e deve corresponder à chave da insígnia
 * no arquivo de configuração.
 */
public enum BadgeType {
    LUMBERJACK("Lenhador"),
    MINING("Mineração"),
    COOKING("Culinária"),
    BUILDER("Construção"),
    FISHING("Pesca"),
    FARMING("Agricultura"),
    CRAFTING("Artesão"),
    EXPLORER("Explorador");

    private final String displayName;

    /**
     * Construtor do enum.
     * @param displayName O nome amigável da categoria, usado para exibição.
     */
    BadgeType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Obtém o nome de exibição amigável para este tipo de insígnia.
     * @return O nome de exibição (ex: "Mineração").
     */
    public String getDisplayName() { return displayName; }
}