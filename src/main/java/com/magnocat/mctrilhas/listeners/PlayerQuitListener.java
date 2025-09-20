package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener que lida com a saída de um jogador do servidor.
 */
public class PlayerQuitListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public PlayerQuitListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Limpa o HUD do jogador para evitar memory leaks.
        plugin.getHudManager().cleanupOnQuit(player);

        // Verifica se este é o último jogador a sair do servidor.
        // Se for, para as tarefas de atualização de cache para economizar recursos.
        if (plugin.getServer().getOnlinePlayers().size() == 1) {
            plugin.stopCacheUpdateTasks();
        }

        // Salva os dados do jogador do cache para o arquivo e o remove da memória.
        plugin.getPlayerDataManager().unloadPlayerData(player.getUniqueId());
    }
}