package com.magnocat.mctrilhas.ctf;

public enum GameState {
    WAITING,      // Esperando por jogadores no lobby da arena
    STARTING,     // Contagem regressiva para o início da partida
    IN_PROGRESS,  // Partida em andamento
    ENDING        // Partida finalizada, exibindo resultados
}