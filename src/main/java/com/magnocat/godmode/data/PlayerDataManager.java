package com.magnocat.godmode.data;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PlayerDataManager {

    private final GodModePlugin plugin;
    private final File dataFolder;

    public PlayerDataManager(GodModePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private File getPlayerFile(Player player) {
        return new File(dataFolder, player.getUniqueId().toString() + ".yml");
    }

    public FileConfiguration getPlayerConfig(Player player) {
        return YamlConfiguration.loadConfiguration(getPlayerFile(player));
    }

    public void savePlayerConfig(Player player, FileConfiguration config) {
        try {
            config.save(getPlayerFile(player));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data file for " + player.getName());
        }
    }

    public List<String> getEarnedBadges(Player player) {
        return getPlayerConfig(player).getStringList("badges");
    }

    public void addBadge(Player player, String badgeId) {
        FileConfiguration config = getPlayerConfig(player);
        List<String> badges = config.getStringList("badges");
        if (!badges.contains(badgeId)) {
            badges.add(badgeId);
            config.set("badges", badges);
            savePlayerConfig(player, config);
        }
    }

    public int getProgress(Player player, String badgeId) {
        return getPlayerConfig(player).getInt("progress." + badgeId, 0);
    }

    public void setProgress(Player player, String badgeId, int newProgress) {
        FileConfiguration config = getPlayerConfig(player);
        config.set("progress." + badgeId, newProgress);
        savePlayerConfig(player, config);
    }
}

    public boolean areProgressMessagesEnabled(Player player) {
        // Default to true so players see messages unless they opt-out.
        return getPlayerConfig(player).getBoolean("settings.progress-messages-enabled", true);
    }

    public void setProgressMessagesEnabled(Player player, boolean enabled) {
        FileConfiguration config = getPlayerConfig(player);
        config.set("settings.progress-messages-enabled", enabled);
        savePlayerConfig(player, config);
    }