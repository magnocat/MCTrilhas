package com.magnocat.mctrilhas.duels;

import org.bukkit.entity.Player;

/**
 * Representa um par de jogadores e um kit aguardando na fila por uma arena livre.
 * Usamos um 'record' para uma classe de dados imutável e concisa.
 */
public record QueuedDuel(Player player1, Player player2, DuelKit kit) {

    public boolean involves(Player player) {
        return player.equals(player1) || player.equals(player2);
    }
}