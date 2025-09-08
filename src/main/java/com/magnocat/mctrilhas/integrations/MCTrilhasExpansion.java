package com.magnocat.mctrilhas.integrations;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.TimeUnit;

public class MCTrilhasExpansion extends PlaceholderExpansion {

    private final MCTrilhasPlugin plugin;

    public MCTrilhasExpansion(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mctrilhas";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MagnoCat";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Necessário para não desregistrar a expansão no /papi reload
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Para jogadores online, os dados estão no cache.
        // Nota: Para jogadores offline, seria necessário carregar os dados do arquivo, o que é mais complexo.
        // Esta implementação funcionará perfeitamente para jogadores online.
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return "N/A"; // Dados do jogador não carregados
        }

        final Rank rank = playerData.getRank();

        // Placeholders que não dependem de progresso
        switch (params.toLowerCase()) {
            case "rank_name":
                return rank.getDisplayName();
            case "rank_prefix":
                return ChatColor.translateAlternateColorCodes('&', rank.getColor() + "[" + rank.getDisplayName() + "]");
            case "rank_name_colored":
                return ChatColor.translateAlternateColorCodes('&', rank.getColor() + rank.getDisplayName());
            case "rank_next_name":
                Rank nextRank = rank.getNext();
                return (nextRank == rank) ? "Máximo" : nextRank.getDisplayName();
        }

        // --- Lógica para Placeholders de Progresso ---
        if (params.toLowerCase().startsWith("rank_progress_")) {
            // Se o jogador já está no ranque máximo
            if (rank == Rank.PIONEIRO || rank == Rank.CHEFE) {
                return switch (params.toLowerCase()) {
                    case "rank_progress_percentage" -> "100";
                    case "rank_progress_bar" -> createProgressBar(1.0, 10, '▌', ChatColor.GOLD, '▌', ChatColor.GRAY);
                    default -> "Máximo";
                };
            }

            final Rank nextRank = rank.getNext();
            final FileConfiguration config = plugin.getConfig();
            final String path = "ranks." + nextRank.name();

            if (!config.isConfigurationSection(path)) return ""; // Sem próximo ranque configurado

            // Requisitos
            final long requiredPlaytimeHours = config.getLong(path + ".required-playtime-hours", Long.MAX_VALUE);
            final int requiredBadges = config.getInt(path + ".required-badges", Integer.MAX_VALUE);
            final long requiredAccountAgeDays = config.getLong(path + ".required-account-age-days", 0);

            final long requiredPlaytimeTicks = requiredPlaytimeHours * 72000;
            final long requiredAccountAgeMillis = TimeUnit.DAYS.toMillis(requiredAccountAgeDays);

            // Progresso atual
            final int currentBadges = playerData.getEarnedBadgesMap().size();
            final long currentPlaytimeTicks = playerData.getActivePlaytimeTicks();
            final long currentAccountAgeMillis = System.currentTimeMillis() - player.getFirstPlayed();

            // Porcentagens individuais
            final double badgePercent = (requiredBadges <= 0) ? 1.0 : Math.min(1.0, (double) currentBadges / requiredBadges);
            final double playtimePercent = (requiredPlaytimeTicks <= 0) ? 1.0 : Math.min(1.0, (double) currentPlaytimeTicks / requiredPlaytimeTicks);
            final double agePercent = (requiredAccountAgeMillis <= 0) ? 1.0 : Math.min(1.0, (double) currentAccountAgeMillis / requiredAccountAgeMillis);

            final double overallPercent = (badgePercent + playtimePercent + agePercent) / 3.0;

            return switch (params.toLowerCase()) {
                case "rank_progress_percentage" -> String.format("%.0f", overallPercent * 100);
                case "rank_progress_bar" -> createProgressBar(overallPercent, 10, '▌', ChatColor.GREEN, '▌', ChatColor.GRAY);
                case "rank_progress_badges" -> currentBadges + "/" + requiredBadges;
                case "rank_progress_playtime" -> (currentPlaytimeTicks / 72000) + "/" + requiredPlaytimeHours;
                case "rank_progress_account_age" -> TimeUnit.MILLISECONDS.toDays(currentAccountAgeMillis) + "/" + requiredAccountAgeDays;
                default -> null; // Placeholder de progresso não encontrado
            };
        }

        return null; // Placeholder não encontrado
    }

    private String createProgressBar(double percentage, int totalBars, char symbol, ChatColor completedColor, char pendingSymbol, ChatColor pendingColor) {
        int progressBars = (int) (totalBars * percentage);
        int pendingBars = totalBars - progressBars;

        return completedColor.toString() + String.valueOf(symbol).repeat(progressBars) +
               pendingColor.toString() + String.valueOf(pendingSymbol).repeat(pendingBars);
    }
}