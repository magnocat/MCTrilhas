package com.magnocat.godmode.listeners;

import com.magnocat.godmode.data.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PlayerJoinListener implements Listener {

    private final PlayerDataManager playerDataManager;

    public PlayerJoinListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Verifica se o jogador já jogou no servidor antes
        if (player.hasPlayedBefore()) {
            long lastPlayedTimestamp = player.getLastPlayed(); // Pega o timestamp em milissegundos
            Date lastPlayedDate = new Date(lastPlayedTimestamp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss");
            String formattedDate = dateFormat.format(lastPlayedDate);

            player.sendMessage(ChatColor.GOLD + "Bem-vindo de volta, " + player.getName() + "!");
            player.sendMessage(ChatColor.GRAY + "Seu último login foi em: " + ChatColor.AQUA + formattedDate);
        } else {
            player.sendMessage(ChatColor.GREEN + "Seja bem-vindo(a) ao MC Trilhas, " + player.getName() + "!");
        }
    }
}