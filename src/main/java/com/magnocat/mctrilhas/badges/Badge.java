package com.magnocat.mctrilhas.badges;

/**
 * Representa uma insígnia com todas as suas propriedades carregadas da configuração.
 * Esta classe é imutável.
 */
public class Badge {

    private final String id;
    private final String name;
    private final String description;
    private final BadgeType type;
    private final double requirement;
    private final String icon;

    public Badge(String id, String name, String description, BadgeType type, double requirement, String icon) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.requirement = requirement;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BadgeType getType() { return type; }

    public double getRequirement() { return requirement; }

    public String getIcon() { return icon; }
}