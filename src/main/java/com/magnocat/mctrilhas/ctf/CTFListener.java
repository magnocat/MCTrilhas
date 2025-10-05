package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class CTFListener implements Listener {

    private final MCTrilhasPlugin plugin;
    private final CTFManager ctfManager;

    public CTFListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.ctfManager = plugin.getCtfManager();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // O manager já lida com a remoção do jogador da fila ou de uma partida em andamento.
        ctfManager.handlePlayerLeave(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        CTFGame game = ctfManager.getGameForPlayer(player);

        if (game != null) {
            // Este jogador está em uma partida de CTF.

            // Impede que o jogador drope seus itens de kit.
            event.getDrops().clear();

            // Deixa a instância do jogo lidar com a lógica de morte (respawn, drop de bandeira, etc.).
            game.handlePlayerDeath(player, event);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        CTFGame game = ctfManager.getGameForPlayer(player);
        if (game != null) {
            // Deixa o jogo lidar com o teleporte para o spawn correto.
            game.handlePlayerRespawn(player, event);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Otimização: só executa a lógica se o jogador se moveu para um novo bloco.
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        CTFGame game = ctfManager.getGameForPlayer(player);
        if (game != null) {
            // Impede o jogador de se mover durante a contagem regressiva
            if (game.getGameState() == GameState.STARTING) {
                // Para evitar jitter, só cancela se o jogador se mover para um bloco diferente (X ou Z)
                if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                    event.setTo(event.getFrom());
                }
                return;
            }

            // Deixa o jogo lidar com as interações de movimento (pegar bandeira, pontuar, etc.).
            game.handlePlayerMove(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Verifica se o jogador está em um jogo e se a mensagem é para o time
        if (message.startsWith("!")) {
            CTFGame game = ctfManager.getGameForPlayer(player);
            if (game != null) {
                // Cancela o evento para que a mensagem não vá para o chat global
                event.setCancelled(true);

                String teamMessage = message.substring(1).trim();
                if (teamMessage.isEmpty()) {
                    return; // Não envia mensagens vazias
                }

                // Envia a mensagem para o time. Como a operação é rápida, podemos fazer de forma síncrona.
                game.sendTeamMessage(player, teamMessage);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        CTFGame game = ctfManager.getGameForPlayer(player);
        if (game != null) {
            // Impede a quebra de blocos, a menos que seja um admin em modo criativo.
            if (player.getGameMode() != GameMode.CREATIVE || !player.isOp()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não pode quebrar blocos durante uma partida de CTF.");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        CTFGame game = ctfManager.getGameForPlayer(player);
        if (game != null) {
            // Impede a colocação de blocos, a menos que seja um admin em modo criativo.
            if (player.getGameMode() != GameMode.CREATIVE || !player.isOp()) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não pode construir durante uma partida de CTF.");
            }
        }
    }

}