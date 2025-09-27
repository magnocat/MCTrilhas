package com.magnocat.mctrilhas.duels;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Representa uma arena de duelo com seus pontos de spawn.
 * Esta classe é usada tanto para arenas carregadas quanto para arenas em processo de criação.
 */
public class DuelArena {

    private final String name;
    private Location spawn1;
    private Location spawn2;
    private Location spectatorSpawn;

    public DuelArena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Location getSpawn1() {
        return spawn1;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public Location getSpawn2() {
        return spawn2;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    /**
     * Verifica se a arena está completamente configurada (com todos os spawns definidos).
     * @return true se a arena estiver pronta para ser salva, false caso contrário.
     */
    public boolean isComplete() {
        return spawn1 != null && spawn2 != null && spectatorSpawn != null;
    }

    /**
     * Salva os dados da arena em uma seção de configuração do YAML.
     * @param section A ConfigurationSection onde os dados serão salvos.
     */
    public void saveToConfig(ConfigurationSection section) {
        if (spawn1 != null) {
            section.set("spawn1", spawn1);
        }
        if (spawn2 != null) {
            section.set("spawn2", spawn2);
        }
        if (spectatorSpawn != null) {
            section.set("spectatorSpawn", spectatorSpawn);
        }
    }
}