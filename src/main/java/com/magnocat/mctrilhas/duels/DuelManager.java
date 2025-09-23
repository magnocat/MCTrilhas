package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia todo o sistema de duelos 1v1.
 * <p>
 * Responsabilidades:
 * <ul>
 *     <li>Carregar e gerenciar as arenas de duelo.</li>
 *     <li>Processar desafios entre jogadores.</li>
 *     <li>Iniciar e finalizar partidas de duelo.</li>
 *     <li>Manter uma lista de jogos ativos.</li>
 * </ul>
 */
public class DuelManager {
    private final MCTrilhasPlugin plugin;

    public DuelManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadArenas() {
        plugin.logInfo("Carregando arenas de duelo do arquivo duel_arenas.yml...");
    }
}