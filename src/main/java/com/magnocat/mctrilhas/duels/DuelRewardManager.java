package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Gerencia a distribuição de recompensas semanais para o ranking de ELO de duelos.
 */
public class DuelRewardManager {

    private final MCTrilhasPlugin plugin;
    private long lastRewardCheckTimestamp = 0;
    private boolean duelActivityThisWeek = false;

    public DuelRewardManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        scheduleRewardCheck();
    }

    private void scheduleRewardCheck() {
        // Roda a cada hora para verificar se é o momento da recompensa.
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndDistributeRewards();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60 * 60); // Atraso de 1 min, repete a cada hora
    }

    private void checkAndDistributeRewards() {
        if (!plugin.getConfig().getBoolean("duel-settings.weekly-rewards.enabled", false)) {
            return;
        }

        Calendar now = Calendar.getInstance();
        int currentDay = now.get(Calendar.DAY_OF_WEEK);
        int currentHour = now.get(Calendar.HOUR_OF_DAY);

        int rewardDay = plugin.getConfig().getInt("duel-settings.weekly-rewards.reward-day-of-week", 1);
        int rewardHour = plugin.getConfig().getInt("duel-settings.weekly-rewards.reward-hour", 20);

        // Verifica se é o dia e a hora corretos.
        if (currentDay == rewardDay && currentHour == rewardHour) {
            // Verifica se a recompensa para esta semana já foi distribuída.
            // Compara o timestamp atual com o último, se a diferença for menor que 23h, já foi dado.
            if (System.currentTimeMillis() - lastRewardCheckTimestamp < 23 * 60 * 60 * 1000) {
                return;
            }

            // Verifica se houve alguma atividade de duelo na semana.
            if (!duelActivityThisWeek) {
                plugin.logInfo("Nenhuma atividade de duelo registrada esta semana. Recompensas de ranking não serão distribuídas.");
                lastRewardCheckTimestamp = System.currentTimeMillis(); // Atualiza o timestamp para não checar de novo nesta hora.
                return;
            }

            plugin.logInfo("Iniciando distribuição de recompensas semanais do ranking de Duelos...");
            lastRewardCheckTimestamp = System.currentTimeMillis();
            distributeRewards();
        }
    }

    private void distributeRewards() {
        plugin.getPlayerDataManager().getAllPlayerDuelStatsAsync().thenAcceptAsync(allStats -> {
            Map<UUID, PlayerDuelStats> sortedStats = allStats.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(PlayerDuelStats::getElo).reversed()))
                    .limit(3) // Pega apenas o Top 3
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            // A distribuição de recompensas (economia) deve ser na thread principal.
            new BukkitRunnable() {
                @Override
                public void run() {
                    int rank = 1;
                    for (Map.Entry<UUID, PlayerDuelStats> entry : sortedStats.entrySet()) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                        double rewardAmount = plugin.getConfig().getDouble("duel-settings.weekly-rewards.rewards." + rank, 0);

                        if (rewardAmount > 0 && plugin.getEconomy() != null) {
                            plugin.getEconomy().depositPlayer(player, rewardAmount);

                            String broadcastMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("duel-settings.weekly-rewards.broadcast-message", "&6&l[DUELOS] &e{player} &7ficou em &e{position}º &7no ranking ELO e recebeu &b{reward} Totens&7!")).replace("{player}", player.getName()).replace("{position}", String.valueOf(rank)).replace("{reward}", String.valueOf((int) rewardAmount));
                            Bukkit.broadcastMessage(broadcastMessage);

                            if (player.isOnline()) {
                                player.getPlayer().sendMessage(ChatColor.GREEN + "Você recebeu " + rewardAmount + " Totens por sua colocação no ranking de duelos!");
                            }
                        }
                        rank++;
                    }
                    plugin.logInfo("Distribuição de recompensas semanais do ranking de Duelos concluída.");

                    // Reseta a flag de atividade para a próxima semana.
                    duelActivityThisWeek = false;
                }
            }.runTask(plugin);
        });
    }

    /**
     * Registra que houve uma partida de duelo válida (com vencedor) nesta semana.
     */
    public void recordDuelActivity() {
        this.duelActivityThisWeek = true;
    }
}