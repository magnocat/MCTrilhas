package com.magnocat.mctrilhas.integrations;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerDataManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
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

        // Lida com os placeholders de contagem de insígnias.
        // Ex: %mctrilhas_badges_daily%, %mctrilhas_badges_monthly%, %mctrilhas_badges_alltime%
        int count;
        switch (params.toLowerCase()) {
            case "badges_daily":
                // NOTA: Este método precisa existir no seu PlayerDataManager e retornar a contagem diária.
                count = dataManager.getDailyBadgeCount(player.getUniqueId());
                break;
            case "badges_monthly":
                // NOTA: Este método precisa existir no seu PlayerDataManager e retornar a contagem mensal.
                count = dataManager.getMonthlyBadgeCount(player.getUniqueId());
                break;
            case "badges_alltime":
                // NOTA: Este método precisa existir no seu PlayerDataManager e retornar a contagem total.
                count = dataManager.getAllTimeBadgeCount(player.getUniqueId());
                break;
            default:
                // Se o placeholder não for reconhecido, retorna null.
                return null;
        }

        return String.valueOf(count);
    }
}