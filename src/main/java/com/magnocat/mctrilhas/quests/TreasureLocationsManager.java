package com.magnocat.mctrilhas.quests;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class TreasureLocationsManager {

    private final MCTrilhasPlugin plugin;
    private File locationsFile;
    private FileConfiguration locationsConfig;

    public TreasureLocationsManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        loadLocations();
    }

    public void loadLocations() {
        locationsFile = new File(plugin.getDataFolder(), "treasure_locations.yml");
        if (!locationsFile.exists()) {
            plugin.saveResource("treasure_locations.yml", false);
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);
    }

    public List<String> getAllLocations() {
        return locationsConfig.getStringList("locations");
    }
}