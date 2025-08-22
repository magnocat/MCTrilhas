package com.magnocat.godmode.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerData {
    private final JavaPlugin plugin;
    private final File playersFolder;
    // Cache para armazenar os dados dos jogadores online de forma eficiente.
    private final Map<UUID, PlayerDataObject> playerDataCache = new HashMap<>();

    // Classe interna para agrupar os dados de um jogador, mantendo o código organizado.
    private static class PlayerDataObject {
        List<String> badges = new ArrayList<>();
        Map<String, Integer> progress = new HashMap<>();
    }

    public PlayerData(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playersFolder = new File(plugin.getDataFolder(), "players");
        if (!playersFolder.exists()) {
            playersFolder.mkdirs();
        }
    }

    /**
     * Carrega os dados de um jogador do arquivo YAML para o cache na memória.
     * Deve ser chamado quando o jogador entra no servidor.
     */
    public void loadPlayerData(UUID playerId) {
        File playerFile = new File(playersFolder, playerId.toString() + ".yml");
        PlayerDataObject data = new PlayerDataObject();

        if (playerFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            data.badges.addAll(config.getStringList("badges"));
            if (config.isConfigurationSection("progress")) {
                config.getConfigurationSection("progress").getKeys(false).forEach(key ->
                        data.progress.put(key, config.getInt("progress." + key))
                );
            }
        }
        playerDataCache.put(playerId, data);
        plugin.getLogger().info("Dados do jogador " + playerId + " carregados para o cache.");
    }

    /**
     * Salva os dados de um jogador do cache para o arquivo YAML e o remove do cache.
     * Deve ser chamado quando o jogador sai do servidor.
     */
    public void savePlayerData(UUID playerId) {
        PlayerDataObject data = playerDataCache.get(playerId);
        if (data == null) {
            return; // Nenhum dado em cache para salvar.
        }

        File playerFile = new File(playersFolder, playerId.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("badges", data.badges);
        config.set("progress", data.progress);

        try {
            config.save(playerFile);
            plugin.getLogger().info("Dados do jogador " + playerId + " salvos no disco.");
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao salvar dados do jogador " + playerId + ": " + e.getMessage());
        }

        playerDataCache.remove(playerId); // Limpa do cache para liberar memória.
    }

    /**
     * Garante que todos os jogadores online tenham seus dados salvos.
     * Deve ser chamado quando o plugin é desativado.
     */
    public void saveAllPlayerData() {
        if (playerDataCache.isEmpty()) return;
        plugin.getLogger().info("Salvando dados de todos os jogadores em cache...");
        // Cria uma cópia das chaves para evitar erros de modificação concorrente.
        new ArrayList<>(playerDataCache.keySet()).forEach(this::savePlayerData);
        plugin.getLogger().info("Todos os dados foram salvos.");
    }

    // --- MÉTODOS DE MANIPULAÇÃO DE DADOS (AGORA OPERAM NO CACHE RÁPIDO) ---

    private PlayerDataObject getPlayerDataObject(UUID playerId) {
        // Garante que o jogador tenha um objeto de dados no cache, mesmo que seja novo.
        return playerDataCache.computeIfAbsent(playerId, k -> {
            // Se o jogador não estava no cache (ex: /reload), carrega seus dados.
            loadPlayerData(k);
            return playerDataCache.get(k);
        });
    }

    public List<String> getPlayerBadges(UUID playerId) {
        return Collections.unmodifiableList(getPlayerDataObject(playerId).badges);
    }

    public void addPlayerBadge(UUID playerId, String badgeId) {
        List<String> badges = getPlayerDataObject(playerId).badges;
        if (!badges.contains(badgeId)) {
            badges.add(badgeId);
        }
    }

    public boolean removePlayerBadge(UUID playerId, String badgeId) {
        return getPlayerDataObject(playerId).badges.remove(badgeId);
    }

    public Map<String, Integer> getPlayerProgress(UUID playerId) {
        return Collections.unmodifiableMap(getPlayerDataObject(playerId).progress);
    }

    public void updatePlayerProgress(UUID playerId, String badgeId, int increment) {
        Map<String, Integer> progress = getPlayerDataObject(playerId).progress;
        progress.put(badgeId, progress.getOrDefault(badgeId, 0) + increment);
    }
}
