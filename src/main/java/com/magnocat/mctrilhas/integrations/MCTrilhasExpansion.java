package com.magnocat.mctrilhas.integrations;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerDataManager;
import com.magnocat.mctrilhas.ranks.Rank;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import com.magnocat.mctrilhas.data.PlayerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MCTrilhasExpansion extends PlaceholderExpansion {

    private final MCTrilhasPlugin plugin;
    private final PlayerDataManager dataManager;

    public MCTrilhasExpansion(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mctrilhas"; // O prefixo: %mctrilhas_...%
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
        return true; // Mantém a expansão carregada
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        String lowerParams = params.toLowerCase();

        // Placeholder para posição no ranking: %mctrilhas_rank_pos_daily%, %mctrilhas_rank_pos_monthly%, etc.
        if (lowerParams.startsWith("rank_pos_")) {
            String type = lowerParams.substring("rank_pos_".length());
            int position = dataManager.getRankPosition(player.getUniqueId(), type);
            return (position > 0) ? "#" + position : "N/A";
        }

        // Lida com os placeholders de contagem de insígnias.
        // Ex: %mctrilhas_badges_daily%, %mctrilhas_badges_monthly%, %mctrilhas_badges_alltime%
        switch (lowerParams) {
            case "rank":
                // O método getRank foi adicionado ao PlayerDataManager para buscar o ranque
                // de jogadores online (do cache) e offline (do arquivo).
                // Isso retornará o nome do ranque em maiúsculas (ex: ESCOTEIRO).
                return dataManager.getRank(player.getUniqueId()).name();
            case "rank_formatted":
                // Retorna o nome do ranque formatado (ex: Escoteiro).
                return dataManager.getRank(player.getUniqueId()).getDisplayName();
            case "badges_daily":
                // NOTA: Este método precisa existir no seu PlayerDataManager e retornar a contagem diária.
                return String.valueOf(dataManager.getDailyBadgeCount(player.getUniqueId()));
            case "badges_monthly":
                // NOTA: Este método precisa existir no seu PlayerDataManager e retornar a contagem mensal.
                return String.valueOf(dataManager.getMonthlyBadgeCount(player.getUniqueId()));
            case "badges_alltime":
                // NOTA: Este método precisa existir no seu PlayerDataManager e retornar a contagem total.
                return String.valueOf(dataManager.getAllTimeBadgeCount(player.getUniqueId()));
            case "rank_progress":
                Rank currentRank = dataManager.getRank(player.getUniqueId());
                Rank nextRank = PlayerDataManager.getNextRank(currentRank);

                if (nextRank == null) {
                    return "Ranque Máximo";
                }

                PlayerData playerData = dataManager.getPlayerData(player.getUniqueId());
                if (playerData == null) { // Carrega dados offline se necessário
                    playerData = dataManager.loadOfflinePlayerData(player.getUniqueId());
                }
                if (playerData == null) {
                    return "N/A";
                }

                FileConfiguration config = plugin.getConfig();
                String rankPath = "ranks." + nextRank.name();

                double reqPlaytime = config.getDouble(rankPath + ".required-playtime-hours", -1);
                int reqBadges = config.getInt(rankPath + ".required-badges", -1);
                int reqAge = config.getInt(rankPath + ".required-account-age-days", -1);

                double currentPlaytime = (double) playerData.getActivePlaytimeTicks() / 72000.0;
                int currentBadges = playerData.getEarnedBadgesMap().size();
                long currentAge = (System.currentTimeMillis() - player.getFirstPlayed()) / (1000L * 60 * 60 * 24);

                Map<String, Double> progressPercentages = new HashMap<>();
                Map<String, String> progressStrings = new HashMap<>();

                if (reqPlaytime > 0 && currentPlaytime < reqPlaytime) {
                    progressPercentages.put("playtime", (currentPlaytime / reqPlaytime) * 100.0);
                    progressStrings.put("playtime", String.format("%d/%d Horas", (int)currentPlaytime, (int)reqPlaytime));
                }
                if (reqBadges > 0 && currentBadges < reqBadges) {
                    progressPercentages.put("badges", ((double)currentBadges / reqBadges) * 100.0);
                    progressStrings.put("badges", String.format("%d/%d Insígnias", currentBadges, reqBadges));
                }
                if (reqAge > 0 && currentAge < reqAge) {
                    progressPercentages.put("age", ((double)currentAge / reqAge) * 100.0);
                    progressStrings.put("age", String.format("%d/%d Dias de conta", (int)currentAge, reqAge));
                }

                if (progressPercentages.isEmpty()) {
                    return "Pronto para promover!";
                }

                // Encontra o requisito com a maior porcentagem de conclusão (o mais próximo de ser finalizado).
                // Em caso de empate, "playtime" é priorizado.
                return progressPercentages.entrySet().stream()
                        .max((e1, e2) -> e1.getValue().equals(e2.getValue()) ? (e1.getKey().equals("playtime") ? 1 : -1) : e1.getValue().compareTo(e2.getValue()))
                        .map(entry -> progressStrings.get(entry.getKey()))
                        .orElse(""); // Retorna string vazia se não houver progresso a ser mostrado.
            default:
                // Se o placeholder não for reconhecido, retorna null.
                return null;
        }
    }
}