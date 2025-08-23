package com.magnocat.godmode.badges;

import com.magnocat.godmode.GodModePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BadgeManager {

    private final GodModePlugin plugin;

    public BadgeManager(GodModePlugin plugin) {
        this.plugin = plugin;
    }

    public void awardBadge(Player player, String badgeId) {
        List<String> earnedBadges = plugin.getPlayerDataManager().getEarnedBadges(player);
        if (earnedBadges.contains(badgeId)) {
            return; // Player already has this badge, do nothing.
        }

        ConfigurationSection badgeSection = plugin.getConfig().getConfigurationSection("badges." + badgeId);
        if (badgeSection == null) {
            plugin.getLogger().warning("Attempted to award a non-existent badge: " + badgeId);
            return;
        }

        // 1. Add badge to player data
        plugin.getPlayerDataManager().addBadge(player, badgeId);

        // 2. Announce and give rewards
        String badgeName = badgeSection.getString("name", badgeId);
        player.sendMessage(ChatColor.GOLD + "---------------------------------");
        player.sendMessage(ChatColor.GREEN + "Insígnia Conquistada: " + ChatColor.BOLD + badgeName);
        player.sendMessage(ChatColor.GOLD + "---------------------------------");

        // 3. Reward with Totems (Vault)
        int totems = badgeSection.getInt("reward-totems", 0);
        if (totems > 0 && plugin.getEconomy() != null) {
            Economy economy = plugin.getEconomy();
            economy.depositPlayer(player, totems);
            player.sendMessage(ChatColor.YELLOW + "+ " + totems + " Totens!");
        }

        // 4. Reward with Item (Note: This is a simplified parser for material names)
        String itemString = badgeSection.getString("reward-item", "");
        int amount = badgeSection.getInt("reward-amount", 1);
        if (!itemString.isEmpty()) {
            // This command can be used to give complex items with NBT data.
            String command = "give " + player.getName() + " " + itemString + " " + amount;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    public void incrementProgress(Player player, String badgeId, int amount) {
        if (plugin.getPlayerDataManager().getEarnedBadges(player).contains(badgeId)) {
            return; // Player has already earned this badge.
        }

        ConfigurationSection badgeSection = plugin.getConfig().getConfigurationSection("badges." + badgeId);
        if (badgeSection == null) {
            return; // Badge is not configured.
        }

        int oldProgress = plugin.getPlayerDataManager().getProgress(player, badgeId);
        int newProgress = oldProgress + amount;
        int requiredProgress = badgeSection.getInt("required-progress", Integer.MAX_VALUE);

        // If progress meets or exceeds the requirement, award the badge and stop.
        if (newProgress >= requiredProgress) {
            plugin.getPlayerDataManager().setProgress(player, badgeId, requiredProgress);
            awardBadge(player, badgeId);
            return;
        }

        // Save the new progress value.
        plugin.getPlayerDataManager().setProgress(player, badgeId, newProgress);

        // Check if the player wants to see progress messages.
        if (plugin.getPlayerDataManager().areProgressMessagesEnabled(player) && requiredProgress > 0) {
            // Calculate milestones as increments of 10%
            int oldMilestone = (oldProgress * 10) / requiredProgress;
            int newMilestone = (newProgress * 10) / requiredProgress;

            if (newMilestone > oldMilestone) {
                String messageFormat = plugin.getConfig().getString("progress-message-format", "&e{badgeName}: &a{progress}&8/&7{required}");
                String badgeName = badgeSection.getString("name", badgeId);
                int percentage = (newProgress * 100) / requiredProgress;

                String message = messageFormat.replace("{badgeName}", badgeName).replace("{progress}", String.valueOf(newProgress)).replace("{required}", String.valueOf(requiredProgress)).replace("{percentage}", String.valueOf(percentage));

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }
}