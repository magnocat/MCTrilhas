package com.magnocat.mctrilhas.duels;

public class EloCalculator {

    private static final int K_FACTOR = 32;

    /**
     * Calcula a mudança de ELO para o vencedor.
     * @param winnerElo O ELO do vencedor.
     * @param loserElo O ELO do perdedor.
     * @return A quantidade de ELO que o vencedor ganha (e o perdedor perde).
     */
    public static int calculateEloChange(int winnerElo, int loserElo) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (double) (loserElo - winnerElo) / 400.0));
        int eloChange = (int) Math.round(K_FACTOR * (1 - expectedScore));

        // Garante que a mudança seja de no mínimo 1 ponto.
        return Math.max(1, eloChange);
    }
}