package com.magnocat.mctrilhas.duels;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Armazena e gerencia as estatísticas de duelo de um jogador (vitórias/derrotas).
 */
public class PlayerDuelStats {
    private int wins;
    private int losses;
    private int elo;

    public static final int DEFAULT_ELO = 1200;

    public PlayerDuelStats(int wins, int losses, int elo) {
        this.wins = wins;
        this.losses = losses;
        this.elo = elo;
    }

    public PlayerDuelStats() {
        this(0, 0, DEFAULT_ELO);
    }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getElo() { return elo; }

    public void incrementWins() { this.wins++; }
    public void incrementLosses() { this.losses++; }
    public void setElo(int elo) { this.elo = elo; }
    public void addElo(int amount) { this.elo += amount; }

    public static PlayerDuelStats fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new PlayerDuelStats();
        }
        int wins = section.getInt("wins", 0);
        int losses = section.getInt("losses", 0);
        int elo = section.getInt("elo", DEFAULT_ELO);
        return new PlayerDuelStats(wins, losses, elo);
    }

    public void saveToConfig(ConfigurationSection section) {
        if (section == null) return;
        section.set("wins", this.wins);
        section.set("losses", this.losses);
        section.set("elo", this.elo);
    }
}