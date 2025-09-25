package com.magnocat.mctrilhas.pet;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Armazena os dados persistentes de um pet de um jogador.
 */
public class PetData {

    public static final int MAX_LEVEL = 50;

    private String type;
    private String name;
    private int level;
    private double experience;
    private boolean hasCustomName;
    private boolean isOwned; // Novo campo para indicar se o pet foi adquirido

    public PetData(String type, String name, int level, double experience, boolean hasCustomName, boolean isOwned) {
        this.type = type;
        this.name = name;
        this.level = level;
        this.experience = experience;
        this.hasCustomName = hasCustomName;
        this.isOwned = isOwned;
    }

    // Getters
    public String getType() { return type; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public double getExperience() { return experience; }
    public boolean hasCustomName() { return hasCustomName; }
    public boolean isOwned() { return isOwned; }

    // Setters
    public void setType(String type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setLevel(int level) { this.level = level; }
    public void setExperience(double experience) { this.experience = experience; }
    public void setHasCustomName(boolean hasCustomName) { this.hasCustomName = hasCustomName; }
    public void setOwned(boolean owned) { isOwned = owned; }

    /**
     * Calcula a quantidade de experiência necessária para o próximo nível.
     * @return A experiência necessária.
     */
    public int getExperienceToNextLevel() {
        return 100 * level;
    }

    /**
     * Cria um objeto PetData a partir de uma seção de configuração.
     * @param section A ConfigurationSection contendo os dados do pet.
     * @return Um novo objeto PetData, ou null se a seção for inválida.
     */
    public static PetData fromConfig(ConfigurationSection section) {
        if (section == null) return null;
        String type = section.getString("type");
        if (type == null || type.isEmpty()) return null;

        return new PetData(
                type,
                section.getString("name", type), // Usa o tipo como nome padrão
                section.getInt("level", 1),
                section.getDouble("experience", 0.0),
                section.getBoolean("has-custom-name", false),
                section.getBoolean("is-owned", false)
        );
    }
}