package com.magnocat.mctrilhas.managers;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Manages loading and accessing the badges.yml configuration file.
 */
public class BadgeConfigManager {

    private final MCTrilhasPlugin plugin;
    private FileConfiguration badgeConfig = null;
    private File badgeConfigFile = null;

    public BadgeConfigManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
        reloadBadgeConfig();
    }

    public void reloadBadgeConfig() {
        if (badgeConfigFile == null) {
            badgeConfigFile = new File(plugin.getDataFolder(), "badges.yml");
        }
        badgeConfig = YamlConfiguration.loadConfiguration(badgeConfigFile);

        InputStream defaultConfigStream = plugin.getResource("badges.yml");
        if (defaultConfigStream != null) {
            badgeConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream)));
        }
    }

    public FileConfiguration getBadgeConfig() {
        if (badgeConfig == null) {
            reloadBadgeConfig();
        }
        return badgeConfig;
    }

    public void saveDefaultConfig() {
        if (badgeConfigFile == null) {
            badgeConfigFile = new File(plugin.getDataFolder(), "badges.yml");
        }
        if (!badgeConfigFile.exists()) {
            plugin.saveResource("badges.yml", false);
        }
    }
}