package com.magnocat.godmode.data;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

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

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    private FileConfiguration getPlayerConfig(UUID uuid) {
        File playerFile = getPlayerFile(uuid);
        if (!playerFile.exists()) {
            return new YamlConfiguration(); // Return empty config for offline players
        }
        return YamlConfiguration.loadConfiguration(playerFile);
    }

    private void savePlayerConfig(UUID uuid, FileConfiguration config) {
        try {
            config.save(getPlayerFile(uuid));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data file for " + uuid);
        }
    }

    public List<String> getEarnedBadges(UUID uuid) {
        return getPlayerConfig(uuid).getStringList("badges");
    }

    public void addBadge(UUID uuid, String badgeId) {
        FileConfiguration config = getPlayerConfig(uuid);
        List<String> badges = config.getStringList("badges");
        if (!badges.contains(badgeId)) {
            badges.add(badgeId);
            config.set("badges", badges);
            savePlayerConfig(uuid, config);
        }
    }

    public void removeBadge(UUID uuid, String badgeId) {
        FileConfiguration config = getPlayerConfig(uuid);
        List<String> badges = config.getStringList("badges");
        if (badges.remove(badgeId)) {
            config.set("badges", badges);
            savePlayerConfig(uuid, config);
        }
    }

    public int getProgress(UUID uuid, String badgeId) {
        // Always return an integer. If progress is not set, default to 0.
        return getPlayerConfig(uuid).getInt("progress." + badgeId, 0);
    }

    public void setProgress(UUID uuid, String badgeId, int newProgress) {
        FileConfiguration config = getPlayerConfig(uuid);
        // Always save progress as an integer.
        config.set("progress." + badgeId, newProgress);
        savePlayerConfig(uuid, config);
    }

    public boolean areProgressMessagesEnabled(UUID uuid) {
        return getPlayerConfig(uuid).getBoolean("settings.progress-messages-enabled", true);
    }

    public void setProgressMessagesEnabled(UUID uuid, boolean enabled) {
        FileConfiguration config = getPlayerConfig(uuid);
        config.set("settings.progress-messages-enabled", enabled);
        savePlayerConfig(uuid, config);
    }
}