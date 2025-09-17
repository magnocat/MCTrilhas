package com.magnocat.mctrilhas.ctf;

/**
 * Armazena as estatísticas de um jogador durante uma única partida de CTF.
 */
public class CTFPlayerStats {

    private int kills = 0;
    private int deaths = 0;
    private int flagCaptures = 0;

    public void incrementKills() {
        this.kills++;
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    public void incrementFlagCaptures() {
        this.flagCaptures++;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getFlagCaptures() {
        return flagCaptures;
    }
}