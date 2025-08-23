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
        if (!itemString.isEmpty()) {
            // This command can be used to give complex items with NBT data.
            String command = "give " + player.getName() + " " + itemString;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}