package com.magnocat.mctrilhas.duels;

import org.bukkit.entity.Player;

/**
 * Representa um desafio de duelo pendente entre dois jogadores.
 */
public class Challenge {
    private final Player challenger;
    private final Player target;
    private final String kitId;
    private final long timestamp;

    public Challenge(Player challenger, Player target, String kitId) {
        this.challenger = challenger;
        this.target = target;
        this.kitId = kitId;
        this.timestamp = System.currentTimeMillis();
    }

    public Player getChallenger() {
        return challenger;
    }

    public Player getTarget() {
        return target;
    }

    public String getKitId() {
        return kitId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Verifica se o desafio expirou com base em um timeout.
     * @param timeoutMillis O tempo de expiração em milissegundos.
     * @return true se o desafio expirou, false caso contrário.
     */
    public boolean isExpired(long timeoutMillis) {
        return (System.currentTimeMillis() - timestamp) > timeoutMillis;
    }
}