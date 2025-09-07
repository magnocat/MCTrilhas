package com.magnocat.mctrilhas.ranks;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.concurrent.TimeUnit;

/**
 * Gerencia a lógica de promoção de ranques dos jogadores.
 */
public class RankManager {

    private final MCTrilhasPlugin plugin;

    public RankManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Verifica se um jogador atende aos requisitos para o próximo ranque e o promove se for o caso.
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

                // Anuncia a promoção para o jogador e para o servidor
                String promotionMessage = ChatColor.GOLD + "Parabéns! Você foi promovido para o ranque: " + nextRank.getColor() + nextRank.getDisplayName();
                player.sendMessage(promotionMessage);
                player.sendTitle(ChatColor.GOLD + "PROMOÇÃO!", nextRank.getColor() + "Você alcançou o ranque " + nextRank.getDisplayName(), 10, 70, 20);

                Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " demonstrou seu valor e foi promovido para " + nextRank.getColor() + nextRank.getDisplayName() + ChatColor.YELLOW + "!");
            }
        } while (promotedInThisCheck);
    }
}