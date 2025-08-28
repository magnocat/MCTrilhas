package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener que lida com a saída de um jogador do servidor.
 */
public class PlayerQuitListener implements Listener {

    private final GodModePlugin plugin;

    public PlayerQuitListener(GodModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Salva os dados do jogador do cache para o arquivo e o remove da memória.
        plugin.getPlayerDataManager().unloadPlayerData(player.getUniqueId());
    }
}