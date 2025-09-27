package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class GameListener implements Listener {

    private final MCTrilhasPlugin plugin;
    private final DuelManager duelManager;

    public GameListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.duelManager = plugin.getDuelManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player loser = event.getEntity();
        DuelGame game = duelManager.getGameForPlayer(loser.getUniqueId());

        if (game != null && game.isParticipant(loser) && game.getState() == DuelGame.GameState.FIGHTING) {
            event.getDrops().clear(); // Não dropar itens do kit
            event.setDeathMessage(null); // Mensagem customizada será enviada

            Player winner = game.getOpponent(loser);
            if (winner != null) {
                // Adia a chamada para o próximo tick para evitar problemas com o evento de morte
                plugin.getServer().getScheduler().runTask(plugin, () -> game.end(winner, loser));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DuelGame game = duelManager.getGameForPlayer(player.getUniqueId());

        if (game != null && game.isParticipant(player)) {
            Player opponent = game.getOpponent(player);
            if (opponent != null) {
                String forfeitMessage = plugin.getConfig().getString("duel-settings.messages.forfeit", "&e{player} &7desistiu do duelo. A partida terminou em empate.");
                // Adia a chamada para o próximo tick
                plugin.getServer().getScheduler().runTask(plugin, () -> game.endAsDraw(forfeitMessage, player));
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        DuelGame game = duelManager.getGameForPlayer(player.getUniqueId());

        if (game != null && game.isParticipant(player)) {
            // Permite apenas o comando de desistir
            if (!event.getMessage().toLowerCase().startsWith("/duelo desistir")) {
                player.sendMessage(org.bukkit.ChatColor.RED + "Você não pode usar comandos durante um duelo, exceto /duelo desistir.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        DuelGame game = duelManager.getGameForPlayer(player.getUniqueId());

        if (game != null && game.isParticipant(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        DuelGame game = duelManager.getGameForPlayer(player.getUniqueId());

        if (game != null && game.isParticipant(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        DuelGame game = duelManager.getGameForPlayer(player.getUniqueId());

        if (game != null && game.isParticipant(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        DuelGame victimGame = duelManager.getGameForPlayer(victim.getUniqueId());

        if (event.getDamager() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            DuelGame attackerGame = duelManager.getGameForPlayer(attacker.getUniqueId());

            // Se o atacante está em um duelo e a vítima não está (ou está em outro duelo), cancela.
            if (attackerGame != null && victimGame != attackerGame) {
                event.setCancelled(true);
            }
            // Se a vítima está em um duelo e o atacante não está, cancela.
            if (victimGame != null && attackerGame == null) {
                event.setCancelled(true);
            }
        }
    }
}