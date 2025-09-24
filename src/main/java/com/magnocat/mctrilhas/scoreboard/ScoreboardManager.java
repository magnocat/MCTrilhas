package com.magnocat.mctrilhas.scoreboard;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.Badge;
import com.magnocat.mctrilhas.badges.BadgeType;
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
import org.bukkit.scoreboard.RenderType;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Alterna a visibilidade do painel para um jogador e envia uma mensagem.
     *
     * @param player O jogador.
     */
    public void toggleBoard(Player player) {
        toggleBoard(player, false);
    }

    /**
     * Alterna a visibilidade do painel para um jogador.
     *
     * @param player O jogador.
     * @param silent Se true, não envia mensagem de feedback para o jogador.
     */
    public void toggleBoard(Player player, boolean silent) {
        UUID uuid = player.getUniqueId();
        if (activeBoards.contains(uuid)) {
            activeBoards.remove(uuid);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            if (!silent) {
                player.sendMessage(ChatColor.RED + "Painel de estatísticas desativado.");
            }
        } else {
            activeBoards.add(uuid);
            updateScoreboard(player);
            if (!silent) {
                player.sendMessage(ChatColor.GREEN + "Painel de estatísticas ativado.");
            }
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
        // Esta é a linha mágica! Ela diz ao placar para não renderizar os números de pontuação.
        // Requer API 1.20.3+ (o que já temos).
        objective.setRenderType(RenderType.HEARTS);

        int score = 16; // Começa do topo (aumentado para caber mais linhas)

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

        // --- Seção de Próxima Insígnia ---
        Badge nextBadge = findNextClosestBadge(playerData);
        if (nextBadge != null) {
            objective.getScore(ChatColor.AQUA + "§lPróxima Insígnia:").setScore(score--);
            objective.getScore(" " + nextBadge.name()).setScore(score--);

            double progress = playerData.getProgressMap().getOrDefault(nextBadge.type(), 0.0);
            objective.getScore("§bProgresso: §f" + (int) progress + "/" + nextBadge.requirement()).setScore(score--);
        } else {
            objective.getScore(ChatColor.GREEN + "Todas as insígnias").setScore(score--);
            objective.getScore(ChatColor.GREEN + "conquistadas!").setScore(score--);
        }
        objective.getScore("   ").setScore(score--); // Espaçador 3

        // --- Seção de Duelos ---
        PlayerDuelStats duelStats = plugin.getPlayerDataManager().getPlayerDuelStats(player.getUniqueId());
        if (duelStats != null) {
            objective.getScore(ChatColor.RED + "§lDuelos:").setScore(score--);
            objective.getScore("§bELO: §f" + duelStats.getElo()).setScore(score--);
            objective.getScore("§bVitórias: §f" + duelStats.getWins()).setScore(score--);
        }

        objective.getScore("    ").setScore(score--); // Espaçador 4

        // --- Seção de Informações Gerais ---
        if (plugin.getEconomy() != null) {
            objective.getScore("§eTotens: §f" + (int) plugin.getEconomy().getBalance(player)).setScore(score--);
        }
        objective.getScore("§7Online: §f" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers()).setScore(score--);

        objective.getScore("     ").setScore(score--); // Espaçador 5
        objective.getScore(ChatColor.DARK_AQUA + "mc.magnocat.net").setScore(score);

        player.setScoreboard(board);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeBoards.remove(event.getPlayer().getUniqueId());
    }

    public boolean isBoardActive(UUID playerUUID) {
        return activeBoards.contains(playerUUID);
    }

    /**
     * Encontra a insígnia que o jogador está mais perto de conquistar.
     *
     * @param playerData Os dados do jogador.
     * @return A insígnia mais próxima, ou null se todas já foram conquistadas.
     */
    @Nullable
    private Badge findNextClosestBadge(PlayerData playerData) {
        Badge closestBadge = null;
        double maxPercentage = -1.0;

        for (Badge badge : plugin.getBadgeManager().getAllBadges()) {
            // Pula insígnias que o jogador já conquistou
            if (playerData.getEarnedBadgesMap().containsKey(badge.id())) {
                continue;
            }

            double progress = playerData.getProgressMap().getOrDefault(badge.type(), 0.0);
            double requirement = badge.requirement();
            double percentage = (requirement > 0) ? (progress / requirement) : 0.0;

            if (percentage > maxPercentage) {
                maxPercentage = percentage;
                closestBadge = badge;
            }
        }
        return closestBadge;
    }
}
