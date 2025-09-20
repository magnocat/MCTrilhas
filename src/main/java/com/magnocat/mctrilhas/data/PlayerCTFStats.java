package com.magnocat.mctrilhas.data;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Armazena as estatísticas permanentes de um jogador no modo CTF.
 */
public class PlayerCTFStats {

    private int kills = 0;
    private int deaths = 0;
    private int wins = 0;
    private int gamesPlayed = 0;
    private int flagCaptures = 0;

    public PlayerCTFStats() {
        // Construtor padrão
    }

    // Novo construtor para criar uma cópia com valores específicos
    public PlayerCTFStats(int wins, int losses, int kills, int deaths, int flagCaptures) {
        this.wins = wins;
        this.gamesPlayed = wins + losses;
        this.kills = kills;
        this.deaths = deaths;
        this.flagCaptures = flagCaptures;
    }

    public void addMatchStats(com.magnocat.mctrilhas.ctf.CTFPlayerStats matchStats, boolean won) {
        this.kills += matchStats.getKills();
        this.deaths += matchStats.getDeaths();
        this.flagCaptures += matchStats.getFlagCaptures();
        this.gamesPlayed++;
        if (won) {
            this.wins++;
        }
    }

    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getWins() { return wins; }
    public int getGamesPlayed() { return gamesPlayed; }
    public int getFlagCaptures() { return flagCaptures; }

    public int getLosses() {
        return gamesPlayed - wins;
    }

    public double getKdRatio() {
        if (deaths == 0) {
            return kills; // Evita divisão por zero
        }
        return (double) kills / deaths;
    }

    /**
     * Carrega as estatísticas de uma ConfigurationSection do arquivo YAML do jogador.
     * @param section A seção 'ctf-stats'.
     * @return Uma instância de PlayerCTFStats.
     */
    public static PlayerCTFStats fromConfig(ConfigurationSection section) {
        PlayerCTFStats stats = new PlayerCTFStats();
        if (section != null) {
            stats.kills = section.getInt("kills", 0);
            stats.deaths = section.getInt("deaths", 0);
            stats.wins = section.getInt("wins", 0);
            stats.gamesPlayed = section.getInt("games-played", 0);
            stats.flagCaptures = section.getInt("flag-captures", 0);
        }
        return stats;
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("kills", kills);
        section.set("deaths", deaths);
        section.set("wins", wins);
        section.set("games-played", gamesPlayed);
        section.set("flag-captures", flagCaptures);
    }
}