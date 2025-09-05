package com.magnocat.mctrilhas.data;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("deprecation")
public class PlayerDataManager {

    private final MCTrilhasPlugin plugin;
    private final File playerDataFolder;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();

    public PlayerDataManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    /**
     * Carrega os dados de um jogador do arquivo para o cache.
     * @param uuid O UUID do jogador.
     */
    public void loadPlayerData(UUID uuid) {
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        if (!playerFile.exists()) {
            // Cria um novo objeto PlayerData para jogadores que entram pela primeira vez
            playerDataCache.put(uuid, new PlayerData(uuid, new ArrayList<>(), new EnumMap<>(BadgeType.class), new HashSet<>(), false, 0));
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        List<String> earnedBadges = config.getStringList("earned-badges");
        boolean progressMessagesDisabled = config.getBoolean("settings.progress-messages-disabled", false);
        long lastDailyReward = config.getLong("last-daily-reward", 0);
        // Carrega a lista de biomas visitados
        List<String> visitedBiomesList = config.getStringList("visited-biomes");

        Map<BadgeType, Double> progressMap = new EnumMap<>(BadgeType.class);
        if (config.isConfigurationSection("progress")) {
            for (String typeStr : config.getConfigurationSection("progress").getKeys(false)) {
                try {
                    BadgeType type = BadgeType.valueOf(typeStr.toUpperCase());
                    double progress = config.getDouble("progress." + typeStr);
                    progressMap.put(type, progress);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Tipo de progresso inválido '" + typeStr + "' encontrado no arquivo de dados do jogador " + uuid);
                }
            }
        }

        PlayerData playerData = new PlayerData(uuid, earnedBadges, progressMap, new HashSet<>(visitedBiomesList), progressMessagesDisabled, lastDailyReward);
        playerDataCache.put(uuid, playerData);
    }

    /**
     * Salva os dados de um jogador do cache para o arquivo e o remove do cache.
     * @param uuid O UUID do jogador.
     */
    public void unloadPlayerData(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            savePlayerData(uuid);
            playerDataCache.remove(uuid);
        }
    }

    public void savePlayerData(UUID uuid) {
        PlayerData playerData = playerDataCache.get(uuid);
        if (playerData == null) {
            return; // Não há nada para salvar
        }

        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("earned-badges", playerData.getEarnedBadges());
        config.set("settings.progress-messages-disabled", playerData.areProgressMessagesDisabled());
        config.set("last-daily-reward", playerData.getLastDailyRewardTime());
        // Salva a lista de biomas visitados
        config.set("visited-biomes", new ArrayList<>(playerData.getVisitedBiomes()));

        // Salva o mapa de progresso
        playerData.getProgressMap().forEach((type, progress) -> {
            config.set("progress." + type.name(), progress);
        });

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível salvar o arquivo de dados para o jogador " + uuid);
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataCache.get(playerUUID);
    }

    // --- Métodos de conveniência para serem usados pelos listeners e comandos ---

    public void addProgress(Player player, BadgeType type, double amount) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.addProgress(type, amount);

            // --- LÓGICA DE CONCLUSÃO DE INSÍGNIA ---
            // Verifica se o jogador atingiu o progresso necessário para uma nova insígnia.

            String badgeId = type.name();

            // Se o jogador já tem a insígnia, não faz mais nada.
            if (data.hasBadge(badgeId)) {
                return;
            }

            // Usa o método que ignora maiúsculas/minúsculas para encontrar a chave no config.yml
            String configKey = plugin.getBadgeConfigManager().getBadgeConfigKey(badgeId);
            if (configKey == null) {
                return; // A insígnia não está configurada.
            }

            String requiredProgressPath = "badges." + configKey + ".required-progress";
            if (!plugin.getBadgeConfigManager().getBadgeConfig().contains(requiredProgressPath)) {
                return; // Não há progresso requerido para esta insígnia.
            }

            double requiredProgress = plugin.getBadgeConfigManager().getBadgeConfig().getDouble(requiredProgressPath);
            double currentProgress = data.getProgress(type);

            if (currentProgress >= requiredProgress) {
                // O jogador completou a insígnia!
                addBadge(player, configKey);
                grantReward(player, configKey, "Parabéns! Você conquistou a ");
            }
        }
    }

    /**
     * Adiciona um bioma à lista de visitados de um jogador e incrementa o progresso se for um novo bioma.
     * @param player O jogador.
     * @param biomeName O nome do bioma (ex: "PLAINS").
     * @return true se o bioma era novo e foi adicionado, false caso contrário.
     */
    public boolean addVisitedBiome(Player player, String biomeName) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data == null) return false;

        boolean isNewBiome = data.getVisitedBiomes().add(biomeName);

        if (isNewBiome) {
            addProgress(player, BadgeType.EXPLORER, 1);
        }
        return isNewBiome;
    }

    public void addBadge(Player player, String badgeId) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null && !data.hasBadge(badgeId)) {
            data.getEarnedBadges().add(badgeId.toLowerCase());
        }
    }

    /**
     * Remove uma insígnia de um jogador e zera o progresso associado a ela.
     * Este é o método correto para ser usado por comandos de administração.
     * @param player O jogador.
     * @param badgeId O ID da insígnia a ser removida (ex: "MINING").
     */
    public void removeBadgeAndResetProgress(Player player, String badgeId) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data == null) return;

        // Remove a insígnia da lista de conquistadas, ignorando maiúsculas/minúsculas.
        data.getEarnedBadges().removeIf(earned -> earned.equalsIgnoreCase(badgeId));

        // Zera o progresso para o tipo de insígnia correspondente.
        try {
            BadgeType type = BadgeType.valueOf(badgeId.toUpperCase());
            data.getProgressMap().put(type, 0.0);
        } catch (IllegalArgumentException e) {
            // Loga um aviso se o tipo de insígnia for inválido, mas não para a execução.
            plugin.getLogger().warning("Tentativa de zerar progresso para uma insígnia inválida '" + badgeId + "' para o jogador " + player.getName());
        }
    }

    /**
     * Concede uma insígnia a um jogador, define seu progresso como completo e entrega a recompensa.
     * Ideal para ser usado por comandos de administração.
     * @param player O jogador que receberá a insígnia.
     * @param badgeId O ID da insígnia (ex: "MINING").
     * @return true se a insígnia foi concedida, false se o jogador já a possuía ou se a insígnia não existe.
     */
    public boolean grantBadgeAndReward(Player player, String badgeId) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data == null || data.hasBadge(badgeId)) {
            return false; // Jogador não está online ou já possui a insígnia.
        }

        String configKey = plugin.getBadgeConfigManager().getBadgeConfigKey(badgeId);
        if (configKey == null) {
            return false; // Insígnia não encontrada na configuração.
        }

        addBadge(player, configKey);

        try {
            BadgeType type = BadgeType.valueOf(badgeId.toUpperCase());
            double requiredProgress = plugin.getBadgeConfigManager().getBadgeConfig().getDouble("badges." + configKey + ".required-progress", 1.0);
            data.getProgressMap().put(type, requiredProgress);
        } catch (IllegalArgumentException e) {
            // Não deve acontecer se configKey foi encontrado.
        }

        grantReward(player, configKey, "Você recebeu a ");
        return true;
    }

    private void grantReward(Player player, String configKey, String messagePrefix) {
        String badgeName = plugin.getBadgeConfigManager().getBadgeConfig().getString("badges." + configKey + ".name", "uma nova insígnia");
        player.sendMessage(ChatColor.GOLD + messagePrefix + ChatColor.AQUA + badgeName + ChatColor.GOLD + "!");

        String rewardItemPath = "badges." + configKey + ".reward-item-data";
        if (plugin.getBadgeConfigManager().getBadgeConfig().isConfigurationSection(rewardItemPath)) {
            ConfigurationSection itemSection = plugin.getBadgeConfigManager().getBadgeConfig().getConfigurationSection(rewardItemPath);
            try {
                Material material = Material.valueOf(Objects.requireNonNull(itemSection.getString("material")).toUpperCase());
                ItemStack rewardItem = new ItemStack(material, itemSection.getInt("amount", 1));
                ItemMeta meta = rewardItem.getItemMeta();
                if (meta != null) {
                    if (itemSection.contains("name")) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemSection.getString("name")));
                    if (itemSection.contains("lore")) {
                        List<String> lore = new ArrayList<>();
                        itemSection.getStringList("lore").forEach(line -> lore.add(ChatColor.translateAlternateColorCodes('&', line)));
                        meta.setLore(lore);
                    }
                    rewardItem.setItemMeta(meta);
                }
                player.getInventory().addItem(rewardItem);
            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao criar item de recompensa para a insígnia '" + configKey + "': " + e.getMessage());
            }
        }

        double totems = plugin.getBadgeConfigManager().getBadgeConfig().getDouble("badges." + configKey + ".reward-totems", 0);
        if (totems > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, totems);
            player.sendMessage(ChatColor.GREEN + "Você recebeu " + ChatColor.YELLOW + totems + " Totens" + ChatColor.GREEN + " como recompensa!");
        }

        // --- LÓGICA DE RECOMPENSA EM MAPA ---
        ItemStack mapReward = plugin.getMapRewardManager().createMapReward(player, configKey);
        if (mapReward != null) {
            player.getInventory().addItem(mapReward);
            player.sendMessage(ChatColor.GREEN + "Você também recebeu um troféu especial!");
        }

        /* Comentado temporariamente para desativar a integração com BlueMap
        // --- LÓGICA DE INTEGRAÇÃO COM BLUEMAP ---
        if (plugin.getBlueMapManager() != null) {
            // Passa a chave da configuração para obter o nome da insígnia.
            plugin.getBlueMapManager().addBadgeMarker(player, configKey);
        }
        */
        // --- LÓGICA DE ANÚNCIO GLOBAL ---
        if (plugin.getConfig().getBoolean("badge-announcement.enabled", false)) {
            String title = plugin.getConfig().getString("badge-announcement.title", "&6&lINSÍGNIA!");
            String subtitle = plugin.getConfig().getString("badge-announcement.subtitle", "&e{player} &7conquistou a insígnia &b{badgeName}&7!");

            // Substitui os placeholders
            title = ChatColor.translateAlternateColorCodes('&', title.replace("{player}", player.getName()).replace("{badgeName}", badgeName));
            subtitle = ChatColor.translateAlternateColorCodes('&', subtitle.replace("{player}", player.getName()).replace("{badgeName}", badgeName));

            // Envia o título para todos os jogadores online
            // Os números são: fadeIn (em ticks), stay (em ticks), fadeOut (em ticks)
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendTitle(title, subtitle, 10, 70, 20);
            }
        }
    }

    public void removeBadge(Player player, String badgeId) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.getEarnedBadges().remove(badgeId.toLowerCase());
        }
    }

    public List<String> getEarnedBadges(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        return data != null ? data.getEarnedBadges() : new ArrayList<>();
    }

    // --- Métodos para o comando /scout ---

    public boolean toggleProgressMessages(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        if (data == null) return false;
        boolean newState = !data.areProgressMessagesDisabled();
        data.setProgressMessagesDisabled(newState);
        return newState;
    }

    public long getLastDailyRewardTime(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        return data != null ? data.getLastDailyRewardTime() : 0;
    }

    public void setLastDailyRewardTime(UUID uuid, long time) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            data.setLastDailyRewardTime(time);
        }
    }

    // --- Métodos para o comando /scout top ---

    /**
     * Obtém a contagem de insígnias de todos os jogadores que possuem dados salvos.
     * <strong>AVISO:</strong> Este método é perigoso, pois executa uma grande quantidade de operações de I/O no thread principal,
     * o que pode causar lag severo ou travar o servidor.
     * @deprecated Use {@link #getAllBadgeCountsAsync()} para uma alternativa segura e assíncrona.
     * @return Um mapa com UUID do jogador e sua contagem de insígnias, mas com alto risco de performance.
     */
    @Deprecated
    public Map<UUID, Integer> getAllBadgeCounts_UNSAFE_SYNCHRONOUS() {
        Map<UUID, Integer> allCounts = new HashMap<>();
        File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                try {
                    UUID uuid = UUID.fromString(playerFile.getName().replace(".yml", ""));
                    FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
                    allCounts.put(uuid, playerData.getStringList("earned-badges").size());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Arquivo de jogador com nome inválido ignorado: " + playerFile.getName());
                }
            }
        }

        // Sobrescreve com dados do cache para garantir que os dados de jogadores online estejam atualizados.
        playerDataCache.values().forEach(data -> allCounts.put(data.getPlayerUUID(), data.getEarnedBadges().size()));
        return allCounts;
    }

    /**
     * Obtém a contagem de insígnias de todos os jogadores de forma assíncrona e segura.
     * Este método não bloqueia o thread principal do servidor, evitando lag.
     *
     * @return Um {@link CompletableFuture} que, quando completo, conterá um mapa com o UUID de cada jogador e sua contagem de insígnias.
     */
    public CompletableFuture<Map<UUID, Integer>> getAllBadgeCountsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Integer> allCounts = new HashMap<>(); // Use um mapa normal aqui, pois é preenchido em um único thread.
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    try {
                        UUID uuid = UUID.fromString(playerFile.getName().replace(".yml", ""));
                        FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
                        allCounts.put(uuid, playerData.getStringList("earned-badges").size());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Arquivo de jogador com nome inválido ignorado: " + playerFile.getName());
                    }
                }
            }
            // Garante que os dados de jogadores online (que podem não ter sido salvos ainda) sejam os mais atuais.
            playerDataCache.values().forEach(data -> allCounts.put(data.getPlayerUUID(), data.getEarnedBadges().size()));
            return allCounts;
        });
    }
}