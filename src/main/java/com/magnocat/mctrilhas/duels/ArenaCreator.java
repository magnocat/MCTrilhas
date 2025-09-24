package com.magnocat.mctrilhas.duels;

import org.bukkit.Location;

/**
 * Classe auxiliar para armazenar os dados de uma arena em processo de criação.
 */
public class ArenaCreator {
    private final String id;
    private Location pos1;
    private Location pos2;
    private Location spectatorSpawn;

    public ArenaCreator(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public Location getPos1() { return pos1; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }

    public Location getPos2() { return pos2; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }

    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public void setSpectatorSpawn(Location spectatorSpawn) { this.spectatorSpawn = spectatorSpawn; }

    /**
     * Verifica se todos os dados necessários para salvar a arena foram definidos.
     */
    public boolean isReady() {
        return id != null && !id.isEmpty() && pos1 != null && pos2 != null && spectatorSpawn != null;
    }
}