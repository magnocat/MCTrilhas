package com.magnocat.mctrilhas.ranks;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Gerencia a lógica de promoção de ranques para os jogadores.
 */
public class RankManager {

    private final MCTrilhasPlugin plugin;

    /**
     * Construtor do gerenciador de ranques.
     * @param plugin A instância principal do plugin.
     */
    public RankManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Verifica se um jogador atende aos requisitos para o próximo ranque e o promove se for o caso.
     * <p>
     * Este método é projetado para ser chamado após ações significativas do jogador (ex: ganhar uma insígnia).
     * Ele opera em um loop para permitir múltiplas promoções em uma única verificação, caso o jogador
     * atenda aos requisitos de vários ranques de uma vez.
     *
     * @param player O jogador a ser verificado.
     */
    public void checkAndPromote(Player player) {
        final PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        boolean promotedInThisCheck;
        do {
            promotedInThisCheck = false;
            Rank currentRank = playerData.getRank();

            // PIONEIRO é o ranque máximo alcançável automaticamente. CHEFE é manual.
            if (currentRank == Rank.PIONEIRO || currentRank == Rank.CHEFE) {
                break; // Sai do loop se o jogador atingiu o ranque máximo.
            }

            Rank nextRank = currentRank.getNext();
            FileConfiguration config = plugin.getConfig();
            String path = "ranks." + nextRank.name();

            // Se não há requisitos configurados para o próximo ranque, não há como promover.
            if (!config.isConfigurationSection(path)) {
                break; // Sai do loop se não houver mais ranques configurados.
            }

            // Obtém os requisitos do config.yml
            int requiredBadges = config.getInt(path + ".required-badges", Integer.MAX_VALUE);
            long requiredPlaytimeHours = config.getLong(path + ".required-playtime-hours", Long.MAX_VALUE);
            long requiredAccountAgeDays = config.getLong(path + ".required-account-age-days", 0);

            // Converte horas para ticks (1 hora = 72000 ticks)
            long requiredPlaytimeTicks = requiredPlaytimeHours * 72000;
            long requiredAccountAgeMillis = TimeUnit.DAYS.toMillis(requiredAccountAgeDays);

            // Obtém as estatísticas do jogador
            int currentBadges = playerData.getEarnedBadgesMap().size();
            long currentPlaytimeTicks = playerData.getActivePlaytimeTicks();
            long accountAgeMillis = System.currentTimeMillis() - player.getFirstPlayed();

            // Verifica se os requisitos foram atendidos
            if (currentBadges >= requiredBadges && currentPlaytimeTicks >= requiredPlaytimeTicks && accountAgeMillis >= requiredAccountAgeMillis) {
                // Promove o jogador!
                playerData.setRank(nextRank);
                promotedInThisCheck = true; // Marca que houve promoção para continuar o loop.

                // Busca as mensagens de promoção do config.yml
                String personalMessage = plugin.getConfig().getString("messages.rank-promotion.personal", "&6Parabéns! Você foi promovido para o ranque: {rank_color}{rank_name}");
                String titleMessage = plugin.getConfig().getString("messages.rank-promotion.title", "&6PROMOÇÃO!");
                String subtitleMessage = plugin.getConfig().getString("messages.rank-promotion.subtitle", "{rank_color}Você alcançou o ranque {rank_name}");
                String broadcastMessage = plugin.getConfig().getString("messages.rank-promotion.broadcast", "&e{player_name} demonstrou seu valor e foi promovido para {rank_color}{rank_name}&e!");

                // Substitui os placeholders
                personalMessage = personalMessage.replace("{rank_color}", nextRank.getColor().toString()).replace("{rank_name}", nextRank.getDisplayName());
                subtitleMessage = subtitleMessage.replace("{rank_color}", nextRank.getColor().toString()).replace("{rank_name}", nextRank.getDisplayName());
                broadcastMessage = broadcastMessage.replace("{player_name}", player.getName()).replace("{rank_color}", nextRank.getColor().toString()).replace("{rank_name}", nextRank.getDisplayName());

                // Anuncia a promoção para o jogador e para o servidor
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', personalMessage));
                player.sendTitle(ChatColor.translateAlternateColorCodes('&', titleMessage), ChatColor.translateAlternateColorCodes('&', subtitleMessage), 10, 70, 20);
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', broadcastMessage));

                // --- LÓGICA DE INSÍGNIA DE RANQUE ---
                // Se o novo ranque for ESCOTEIRO, concede a insígnia comemorativa.
                if (nextRank == Rank.ESCOTEIRO) {
                    plugin.getPlayerDataManager().grantBadgeAndReward(player, "SCOUT");
                }

                // --- LÓGICA DE RECOMPENSA PARA O PADRINHO ---
                if (playerData.getGodfatherUUID() != null) {
                    UUID godfatherUUID = playerData.getGodfatherUUID();
                    org.bukkit.OfflinePlayer godfatherOffline = Bukkit.getOfflinePlayer(godfatherUUID);

                    double rewardAmount = 10.0; // Valor da recompensa
                    if (plugin.getEconomy() != null) {
                        plugin.getEconomy().depositPlayer(godfatherOffline, rewardAmount);
                    }

                    // Notifica o padrinho se ele estiver online.
                    if (godfatherOffline.isOnline()) {
                        Player godfatherOnline = godfatherOffline.getPlayer();
                        if (godfatherOnline != null) {
                            godfatherOnline.sendMessage(ChatColor.GOLD + "Seu afilhado, " + player.getName() + ", foi promovido para " + nextRank.getDisplayName() + "! Você recebeu " + (int) rewardAmount + " Totens como recompensa.");
                        }
                    }
                }
            }
        } while (promotedInThisCheck);
    }
}