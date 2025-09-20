package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@SuppressWarnings("deprecation") // Para AsyncPlayerChatEvent
public class GameChatListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public GameChatListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        plugin.getHttpApiManager().addChatMessage(player, message);
    }
}