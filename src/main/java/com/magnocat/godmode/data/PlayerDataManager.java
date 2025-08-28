package com.magnocat.godmode.data;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final GodModePlugin plugin;
    private final File dataFolder;
    // Cache para os dados dos jogadores online para evitar leituras constantes do disco.
    private final Map<UUID, FileConfiguration> playerCache = new ConcurrentHashMap<>();

    public PlayerDataManager(GodModePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Carrega os dados de um jogador do arquivo para o cache.
     * Ideal para ser chamado no evento PlayerJoinEvent.
     * @param uuid O UUID do jogador.
     */
    public void loadPlayerData(UUID uuid) {
        File playerFile = getPlayerFile(uuid);
        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        playerCache.put(uuid, playerConfig);
    }

    /**
     * Salva os dados de um jogador do cache para o arquivo e o remove do cache.
     * Ideal para ser chamado no evento PlayerQuitEvent.
     * @param uuid O UUID do jogador.
     */
    public void unloadPlayerData(UUID uuid) {
        if (playerCache.containsKey(uuid)) {
            savePlayerConfig(uuid, playerCache.get(uuid));
            playerCache.remove(uuid);
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }

    private FileConfiguration getPlayerConfig(UUID uuid) {
        // Prioriza o cache para jogadores online, melhorando a performance.
        // Para jogadores offline, carrega diretamente do arquivo.
        return playerCache.getOrDefault(uuid, YamlConfiguration.loadConfiguration(getPlayerFile(uuid)));
    }

    private void savePlayerConfig(UUID uuid, FileConfiguration config) {
        try {
            config.save(getPlayerFile(uuid));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data file for " + uuid);
        }
    }

    public boolean hasBadge(UUID uuid, String badgeId) {
        return getEarnedBadges(uuid).contains(badgeId);
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

    public long getProgress(UUID uuid, String badgeId) {
        return getPlayerConfig(uuid).getInt("progress." + badgeId, 0);
    }

    public void setProgress(UUID uuid, String badgeId, long newProgress) {
        FileConfiguration config = getPlayerConfig(uuid);
        config.set("progress." + badgeId, newProgress);
        savePlayerConfig(uuid, config);
    }

    /**
     * Verifica se as mensagens de progresso estão desativadas para um jogador.
     * @param uuid O UUID do jogador.
     * @return true se as mensagens estiverem desativadas, false caso contrário.
     */
    public boolean areProgressMessagesDisabled(UUID uuid) {
        // O padrão é 'false' (mensagens ativadas).
        return getPlayerConfig(uuid).getBoolean("settings.progress-messages-disabled", false);
    }

    /**
     * Alterna a configuração de exibição de mensagens de progresso para um jogador.
     * @param uuid O UUID do jogador.
     * @return true se as mensagens foram desativadas, false se foram ativadas.
     */
    public boolean toggleProgressMessages(UUID uuid) {
        boolean isDisabled = areProgressMessagesDisabled(uuid);
        boolean newSetting = !isDisabled;
        FileConfiguration config = getPlayerConfig(uuid);
        config.set("settings.progress-messages-disabled", newSetting);
        savePlayerConfig(uuid, config);
        return newSetting;
    }

    public long getLastDailyRewardTime(UUID uuid) {
        return getPlayerConfig(uuid).getLong("last-daily-reward", 0);
    }

    public void setLastDailyRewardTime(UUID uuid, long time) {
        FileConfiguration config = getPlayerConfig(uuid);
        config.set("last-daily-reward", time);
        savePlayerConfig(uuid, config);
    }

    /**
     * Obtém a contagem de insígnias de todos os jogadores que possuem dados salvos.
     * Usa o cache para jogadores online para garantir dados atualizados.
     * @return Um mapa com UUID do jogador e sua contagem de insígnias.
     */
    public Map<UUID, Integer> getAllBadgeCounts() {
        Map<UUID, Integer> allCounts = new HashMap<>();
        File[] playerFiles = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                try {
                    UUID uuid = UUID.fromString(playerFile.getName().replace(".yml", ""));
                    FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
                    allCounts.put(uuid, playerData.getStringList("badges").size());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Arquivo de jogador com nome inválido ignorado: " + playerFile.getName());
                }
            }
        }
        // Sobrescreve com dados do cache para garantir que os dados de jogadores online estejam atualizados.
        playerCache.forEach((uuid, config) -> allCounts.put(uuid, config.getStringList("badges").size()));
        return allCounts;
    }
}