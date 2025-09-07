package com.magnocat.mctrilhas.utils;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class MessageUtils {

    public static void displayRequirement(CommandSender sender, String name, long current, long required) {
        ChatColor statusColor = (current >= required) ? ChatColor.GREEN : ChatColor.YELLOW;
        String progressBar = createProgressBar(current, required);
        sender.sendMessage(statusColor + "❖ " + name + ": " + ChatColor.AQUA + current + "/" + required);
        sender.sendMessage("   " + progressBar);
    }

    public static String createProgressBar(long current, long required) {
        if (required <= 0) return "";
        double percentage = Math.min(1.0, (double) current / required);

        int totalBars = 20;
        int progressBars = (int) (totalBars * percentage);

        return ChatColor.GREEN + "▌".repeat(progressBars) +
               ChatColor.GRAY + "▌".repeat(totalBars - progressBars) +
               ChatColor.WHITE + " " + String.format("%.0f%%", percentage * 100);
    }

    public static void displayRankProgress(CommandSender sender, Player target, PlayerData playerData, MCTrilhasPlugin plugin) {
        Rank currentRank = playerData.getRank();
        if (currentRank == Rank.PIONEIRO || currentRank == Rank.CHEFE) {
            sender.sendMessage(ChatColor.GREEN + "   Ranque máximo alcançado!");
            return;
        }

        Rank nextRank = currentRank.getNext();
        FileConfiguration config = plugin.getConfig();
        String path = "ranks." + nextRank.name();

        if (!config.isConfigurationSection(path)) {
            sender.sendMessage(ChatColor.GRAY + "   Nenhum ranque superior disponível.");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Próximo ranque: " + nextRank.getColor() + nextRank.getDisplayName());

        long requiredPlaytimeHours = config.getLong(path + ".required-playtime-hours");
        int requiredBadges = config.getInt(path + ".required-badges");
        long requiredAccountAgeDays = config.getLong(path + ".required-account-age-days");

        long currentPlaytimeHours = playerData.getActivePlaytimeTicks() / 72000;
        int currentBadges = playerData.getEarnedBadgesMap().size();
        long currentAccountAgeDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - target.getFirstPlayed());

        displayRequirement(sender, "Horas de Jogo Ativo", currentPlaytimeHours, requiredPlaytimeHours);
        displayRequirement(sender, "Insígnias Conquistadas", (long) currentBadges, requiredBadges);
        displayRequirement(sender, "Dias no Servidor", currentAccountAgeDays, requiredAccountAgeDays);
    }
}