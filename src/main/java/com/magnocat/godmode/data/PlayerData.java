package com.magnocat.godmode.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final JavaPlugin plugin;

    public PlayerData(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> getPlayerBadges(UUID playerId) {
        File file = new File(plugin.getDataFolder(), "players/" + playerId.toString() + ".yml");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getStringList("badges");
    }

    public void addPlayerBadge(UUID playerId, String badgeId) {
        File folder = new File(plugin.getDataFolder(), "players");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, playerId.toString() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> badges = config.getStringList("badges");
        if (!badges.contains(badgeId)) {
            badges.add(badgeId);
            config.set("badges", badges);
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao salvar dados do jogador: " + e.getMessage());
            }
        }
    }

    public boolean removePlayerBadge(UUID playerId, String badgeId) {
        File file = new File(plugin.getDataFolder(), "players/" + playerId.toString() + ".yml");
        if (!file.exists()) {
            return false;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> badges = config.getStringList("badges");
        if (badges.remove(badgeId)) {
            config.set("badges", badges);
            try {
                config.save(file);
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("Erro ao salvar dados do jogador: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public Map<String, Integer> getPlayerProgress(UUID playerId) {
        File file = new File(plugin.getDataFolder(), "players/" + playerId.toString() + ".yml");
        if (!file.exists()) {
            return new HashMap<>();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Map<String, Integer> progress = new HashMap<>();
        if (config.contains("progress")) {
            config.getConfigurationSection("progress").getKeys(false).forEach(key ->
                progress.put(key, config.getInt("progress." + key))
            );
        }
        return progress;
    }

    public void updatePlayerProgress(UUID playerId, String badgeId, int increment) {
        File folder = new File(plugin.getDataFolder(), "players");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, playerId.toString() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int current = config.getInt("progress." + badgeId, 0);
        config.set("progress." + badgeId, current + increment);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar progresso do jogador: " + e.getMessage());
        }
    }
}
