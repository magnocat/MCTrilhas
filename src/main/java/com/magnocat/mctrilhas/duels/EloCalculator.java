package com.magnocat.mctrilhas.duels;

/**
 * Classe utilitária para calcular as mudanças de rating ELO.
 */
public final class EloCalculator {

    // O K-factor determina o impacto máximo de uma partida no rating.
    // Valores mais altos significam mudanças mais rápidas. 32 é um valor comum.
    private static final int K_FACTOR = 32;

    private EloCalculator() {
        // Classe utilitária, não deve ser instanciada.
    }

    /**
     * Calcula o novo rating para dois jogadores após uma partida.
     * @param winnerRating O rating ELO atual do vencedor.
     * @param loserRating O rating ELO atual do perdedor.
     * @return Um array de inteiros com [novo rating do vencedor, novo rating do perdedor].
     */
    public static int[] calculateNewRatings(int winnerRating, int loserRating) {
        double expectedWinnerScore = calculateExpectedScore(winnerRating, loserRating);
        double expectedLoserScore = 1.0 - expectedWinnerScore; // O esperado do perdedor é o inverso

        // S_A é o resultado real: 1 para vitória, 0 para derrota.
        int newWinnerRating = (int) Math.round(winnerRating + K_FACTOR * (1.0 - expectedWinnerScore));
        int newLoserRating = (int) Math.round(loserRating + K_FACTOR * (0.0 - expectedLoserScore));

        return new int[]{newWinnerRating, newLoserRating};
    }

    /**
     * Calcula a probabilidade de vitória do jogador A contra o jogador B.
     */
    private static double calculateExpectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10, (double) (ratingB - ratingA) / 400.0));
    }
}