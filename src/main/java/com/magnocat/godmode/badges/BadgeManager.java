package com.magnocat.godmode.badges;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;

public class BadgeManager {
    private final Map<String, Badge> badges = new HashMap<>();

    public void loadBadges(JavaPlugin plugin) {
        ConfigurationSection badgeSection = plugin.getConfig().getConfigurationSection("badges");
        if (badgeSection != null) {
            for (String key : badgeSection.getKeys(false)) {
                String name = badgeSection.getString(key + ".name");
                String description = badgeSection.getString(key + ".description");
                int rewardTotems = badgeSection.getInt(key + ".reward-totems");
                String rewardItem = badgeSection.getString(key + ".reward-item");
                int rewardAmount = badgeSection.getInt(key + ".reward-amount");
                String rewardRegion = badgeSection.getString(key + ".reward-region");
                badges.put(key, new Badge(key, name, description, rewardTotems, rewardItem, rewardAmount, rewardRegion));
            }
        }
    }

    public Map<String, Badge> getBadges() {
        return badges;
    }
}
