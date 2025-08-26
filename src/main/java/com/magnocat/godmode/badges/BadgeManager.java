package com.magnocat.godmode.badges;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.utils.ItemFactory;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.List;

/**
 * Manages the logic for awarding badges, tracking progress, and giving rewards.
 */
public class BadgeManager {

    private final GodModePlugin plugin;

    // Constants for configuration keys to avoid "magic strings"
    private static final String BADGES_SECTION = "badges.";
    private static final String KEY_REWARD_TOTEMS = "reward-totems";
    private static final String KEY_REWARD_ITEM_DATA = "reward-item-data";
    private static final String KEY_REQUIRED_PROGRESS = "required-progress";
    private static final String KEY_AWARD_MESSAGE = "badge-award-format";
    private static final String KEY_PROGRESS_MESSAGE_FORMAT = "progress-message-format";
    private static final String KEY_TOTEM_REWARD_MESSAGE = "totem-reward-format";

    /**
     * Constructs a new BadgeManager.
     * @param plugin The main plugin instance.
     */
    public BadgeManager(GodModePlugin plugin) {
        this.plugin = plugin;
    }

    public void awardBadge(Player player, String badgeId) {
        List<String> earnedBadges = plugin.getPlayerDataManager().getEarnedBadges(player.getUniqueId());
        if (earnedBadges.contains(badgeId)) {
            return; // Player already has this badge, do nothing.
        }

        ConfigurationSection badgeSection = plugin.getConfig().getConfigurationSection(BADGES_SECTION + badgeId);
        if (badgeSection == null) {
            plugin.getLogger().warning("Attempted to award a non-existent badge: " + badgeId);
            return;
        }

        // 1. Add badge to player data
        plugin.getPlayerDataManager().addBadge(player.getUniqueId(), badgeId);

        // 2. Announce and give rewards
        // Announce the achievement using a configurable message format
        String badgeName = badgeSection.getString("name", badgeId);
        List<String> awardMessages = plugin.getConfig().getStringList(KEY_AWARD_MESSAGE);

        // Provide a default message if the configuration is missing, making it robust
        if (awardMessages.isEmpty()) {
            awardMessages = List.of(
                "&6---------------------------------",
                "&aInsígnia Conquistada: &l{badgeName}",
                "&6---------------------------------"
            );
        }
        awardMessages.forEach(line -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', line.replace("{badgeName}", badgeName))));

        // 3. Reward with Totems (Vault)
        int totems = badgeSection.getInt(KEY_REWARD_TOTEMS, 0);
        if (totems > 0 && plugin.getEconomy() != null) {
            Economy economy = plugin.getEconomy();
            economy.depositPlayer(player, totems);
            // Send a configurable message for the totem reward
            String totemMessageFormat = plugin.getConfig().getString(KEY_TOTEM_REWARD_MESSAGE, "&e+ {amount} Totens!");
            String totemMessage = totemMessageFormat.replace("{amount}", String.valueOf(totems));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', totemMessage));
        }

        // 4. Reward with Item (Robust API-based method)
        ConfigurationSection itemSection = badgeSection.getConfigurationSection(KEY_REWARD_ITEM_DATA);
        if (itemSection != null) {
            ItemStack rewardItem = ItemFactory.createRewardItem(itemSection, badgeId, plugin.getLogger());
            if (rewardItem != null) {
                // Give the item to the player, dropping it if the inventory is full.
                player.getInventory().addItem(rewardItem).forEach((index, item) ->
                        player.getWorld().dropItem(player.getLocation(), item)
                );
            }
        }
    }

    public void incrementProgress(Player player, String badgeId, int amount) {
        if (plugin.getPlayerDataManager().getEarnedBadges(player.getUniqueId()).contains(badgeId)) {
            return; // Player has already earned this badge.
        }

        ConfigurationSection badgeSection = plugin.getConfig().getConfigurationSection(BADGES_SECTION + badgeId);
        if (badgeSection == null) {
            return; // Badge is not configured.
        }

        // The PlayerDataManager should handle returning 0 if no progress exists.
        int oldProgress = plugin.getPlayerDataManager().getProgress(player.getUniqueId(), badgeId);
        int newProgress = oldProgress + amount;
        int requiredProgress = badgeSection.getInt(KEY_REQUIRED_PROGRESS, Integer.MAX_VALUE);

        // If progress meets or exceeds the requirement, award the badge and stop.
        if (newProgress >= requiredProgress) {
            plugin.getPlayerDataManager().setProgress(player.getUniqueId(), badgeId, requiredProgress);
            awardBadge(player, badgeId);
            return;
        }

        // Save the new progress value.
        plugin.getPlayerDataManager().setProgress(player.getUniqueId(), badgeId, newProgress);

        // Check if the player wants to see progress messages.
        if (plugin.getPlayerDataManager().areProgressMessagesEnabled(player.getUniqueId())) {
            sendProgressMessage(player, badgeId, badgeSection, newProgress, requiredProgress);
        }
    }

    /**
     * Formats and sends a progress update message to the player.
     *
     * @param player The player to notify.
     * @param badgeId The ID of the badge.
     * @param badgeSection The ConfigurationSection for the badge.
     * @param currentProgress The player's current progress.
     * @param requiredProgress The total progress required for the badge.
     */
    private void sendProgressMessage(Player player, String badgeId, ConfigurationSection badgeSection, int currentProgress, int requiredProgress) {
        String messageFormat = plugin.getConfig().getString(KEY_PROGRESS_MESSAGE_FORMAT, "&e{badgeName}: &a{progress}&8/&7{required} &b({percentage}%)");
        String badgeName = badgeSection.getString("name", badgeId);
        int percentage = (currentProgress * 100) / requiredProgress;

        String message = messageFormat.replace("{badgeName}", badgeName)
                .replace("{progress}", String.valueOf(currentProgress))
                .replace("{required}", String.valueOf(requiredProgress))
                .replace("{percentage}", String.valueOf(percentage));

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}