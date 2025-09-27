package com.magnocat.mctrilhas.duels;

import org.bukkit.entity.Player;

/**
 * Representa um desafio de duelo pendente entre dois jogadores.
 * Usamos um 'record' para uma classe de dados imutável e concisa.
 */
public record Challenge(Player challenger, Player target, String kitId, long timestamp) {

    /**
     * Construtor de conveniência que define o timestamp automaticamente.
     */
    public Challenge(Player challenger, Player target, String kitId) {
        this(challenger, target, kitId, System.currentTimeMillis());
    }

    /**
     * Verifica se o desafio expirou com base em um timeout.
     *
     * @param timeoutMillis O tempo de expiração em milissegundos.
     * @return true se o desafio expirou, false caso contrário.
     */
    public boolean isExpired(long timeoutMillis) {
        return (System.currentTimeMillis() - timestamp) > timeoutMillis;
    }
}