package com.magnocat.mctrilhas.integrations;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerDataManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import com.magnocat.mctrilhas.data.PlayerData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        // Lida com os placeholders de contagem de insígnias.
        // Ex: %mctrilhas_badges_daily%, %mctrilhas_badges_monthly%, %mctrilhas_badges_alltime%
        switch (lowerParams) {
            case "rank":
                // O método getRank foi adicionado ao PlayerDataManager para buscar o ranque
                // de jogadores online (do cache) e offline (do arquivo).
                // Isso retornará o nome do ranque em maiúsculas (ex: ESCOTEIRO).
                return dataManager.getRank(player.getUniqueId()).name();
            case "badges_daily":
                // NOTA: Este método precisa existir no seu PlayerDataManager e retornar a contagem diária.
                return String.valueOf(dataManager.getDailyBadgeCount(player.getUniqueId()));
            case "badges_monthly":
                // NOTA: Este método precisa existir no seu PlayerDataManager e retornar a contagem mensal.
                return String.valueOf(dataManager.getMonthlyBadgeCount(player.getUniqueId()));
            case "badges_alltime":
                // NOTA: Este método precisa existir no seu PlayerDataManager e retornar a contagem total.
                return String.valueOf(dataManager.getAllTimeBadgeCount(player.getUniqueId()));
            default:
                // Se o placeholder não for reconhecido, retorna null.
                return null;
        }
    }
}