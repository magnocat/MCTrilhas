package com.magnocat.mctrilhas.ctf;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Representa um time (Vermelho ou Azul) dentro de uma partida de CTF.
 */
public class CTFTeam {

    private final TeamColor color;
    private final Set<UUID> players = new HashSet<>();
    private int score = 0;

    public CTFTeam(TeamColor color) {
        this.color = color;
    }

    public void addPlayer(UUID playerUUID) {
        players.add(playerUUID);
    }

    public void removePlayer(UUID playerUUID) {
        players.remove(playerUUID);
    }

    public void incrementScore() {
        this.score++;
    }

    public void broadcastMessage(String message) {
        for (UUID playerUUID : players) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    // --- Getters ---

    public TeamColor getColor() { return color; }

    public Set<UUID> getPlayers() { return players; }

    public int getScore() { return score; }
}