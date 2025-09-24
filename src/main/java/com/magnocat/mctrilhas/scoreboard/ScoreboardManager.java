package com.magnocat.mctrilhas.scoreboard;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.duels.PlayerDuelStats;
import com.magnocat.mctrilhas.data.PlayerDataManager;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia a criação e atualização do painel lateral (scoreboard) de
 * estatísticas.
 */
public class ScoreboardManager implements Listener {

    private final MCTrilhasPlugin plugin;
    private final Set<UUID> activeBoards = ConcurrentHashMap.newKeySet();

    public ScoreboardManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startUpdater();
    }

    public void toggleBoard(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeBoards.contains(uuid)) {
            activeBoards.remove(uuid);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            player.sendMessage(ChatColor.RED + "Painel de estatísticas desativado.");
        } else {
            activeBoards.add(uuid);
            updateScoreboard(player);
            player.sendMessage(ChatColor.GREEN + "Painel de estatísticas ativado.");
        }
    }

    private void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activeBoards) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        updateScoreboard(player);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L * 5); // Atualiza a cada 5 segundos
    }

    private void updateScoreboard(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("mctrilhas_sb", "dummy", ChatColor.GOLD + "⚜ " + ChatColor.BOLD + "MC Trilhas" + ChatColor.GOLD + " ⚜");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = 15; // Começa do topo

        objective.getScore(" ").setScore(score--); // Espaçador

        Rank currentRank = playerData.getRank();
        Rank nextRank = PlayerDataManager.getNextRank(currentRank);

        if (nextRank != null) {
            objective.getScore(ChatColor.YELLOW + "§lPróximo Ranque:").setScore(score--);
            objective.getScore(nextRank.getColor() + " " + nextRank.getDisplayName()).setScore(score--);

            long reqPlaytime = plugin.getConfig().getLong("ranks." + nextRank.name() + ".required-playtime-hours", -1);
            int reqBadges = plugin.getConfig().getInt("ranks." + nextRank.name() + ".required-badges", -1);

            if (reqPlaytime > 0) {
                objective.getScore("§bHoras: §f" + (playerData.getActivePlaytimeTicks() / 72000) + "/" + reqPlaytime).setScore(score--);
            }
            if (reqBadges > 0) {
                objective.getScore("§bInsígnias: §f" + playerData.getEarnedBadgesMap().size() + "/" + reqBadges).setScore(score--);
            }
        } else {
            objective.getScore(ChatColor.GREEN + "Ranque Máximo!").setScore(score--);
        }

        objective.getScore("  ").setScore(score--); // Espaçador 2

        // --- Seção de Duelos ---
        PlayerDuelStats duelStats = plugin.getPlayerDataManager().getPlayerDuelStats(player.getUniqueId());
        if (duelStats != null) {
            objective.getScore(ChatColor.RED + "§lDuelos:").setScore(score--);
            objective.getScore("§bELO: §f" + duelStats.getElo()).setScore(score--);
            objective.getScore("§bVitórias: §f" + duelStats.getWins()).setScore(score--);
        }

        objective.getScore("   ").setScore(score--); // Espaçador 3
        objective.getScore(ChatColor.DARK_AQUA + "mc.magnocat.net").setScore(score);

        player.setScoreboard(board);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeBoards.remove(event.getPlayer().getUniqueId());
    }
}
