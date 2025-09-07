package com.magnocat.mctrilhas.data;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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
import java.util.Calendar;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

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
            // Cria um novo objeto PlayerData para jogadores que entram pela primeira vez.
            // O ranque inicial é sempre FILHOTE.
            playerDataCache.put(uuid, new PlayerData(uuid, new HashMap<>(), new EnumMap<>(BadgeType.class), new HashSet<>(), false, 0, Rank.FILHOTE, 0));
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        // A estrutura de insígnias agora é um Map<String, Long> (badgeId -> timestamp).
        Map<String, Long> earnedBadges = new HashMap<>();
        // Lógica de migração: se o formato antigo (lista) existir, converte para o novo (mapa).
        if (config.isList("earned-badges")) {
            plugin.getLogger().info("Migrando dados de insígnias para o novo formato para o jogador " + uuid);
            List<String> oldBadges = config.getStringList("earned-badges");
            oldBadges.forEach(badgeId -> earnedBadges.put(badgeId.toLowerCase(), 1L)); // Usa 1L para indicar que é um dado legado.
            config.set("earned-badges", null); // Remove a chave antiga.
        } else if (config.isConfigurationSection("earned-badges-timed")) {
            ConfigurationSection badgesSection = config.getConfigurationSection("earned-badges-timed");
            if (badgesSection != null) {
                for (String badgeId : badgesSection.getKeys(false)) {
                    earnedBadges.put(badgeId.toLowerCase(), badgesSection.getLong(badgeId));
                }
            }
        }

        boolean progressMessagesDisabled = config.getBoolean("settings.progress-messages-disabled", false);
        long lastDailyReward = config.getLong("last-daily-reward", 0);
        // Carrega a lista de biomas visitados
        List<String> visitedBiomesList = config.getStringList("visited-biomes");
        // Carrega o ranque do jogador, com FILHOTE como padrão.
        Rank rank = Rank.fromString(config.getString("rank", "FILHOTE"));
        // Carrega o tempo de jogo ativo.
        long activePlaytimeTicks = config.getLong("active-playtime-ticks", 0);

        // Lógica de migração única para jogadores existentes sem tempo de jogo ativo.
        if (activePlaytimeTicks == 0 && !config.contains("playtime-migrated")) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.hasPlayedBefore()) {
                activePlaytimeTicks = (long) offlinePlayer.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                config.set("playtime-migrated", true); // Marca que a migração foi feita.
                plugin.getLogger().info("Migrando tempo de jogo para " + offlinePlayer.getName() + ". Tempo de jogo inicial definido como " + (activePlaytimeTicks / 72000) + " horas.");
            }
        }

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

        PlayerData playerData = new PlayerData(uuid, earnedBadges, progressMap, new HashSet<>(visitedBiomesList), progressMessagesDisabled, lastDailyReward, rank, activePlaytimeTicks);
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

        // Salva o novo mapa de insígnias com timestamps.
        playerData.getEarnedBadgesMap().forEach((badgeId, timestamp) -> {
            config.set("earned-badges-timed." + badgeId, timestamp);
        });

        config.set("settings.progress-messages-disabled", playerData.areProgressMessagesDisabled());
        config.set("last-daily-reward", playerData.getLastDailyRewardTime());
        // Salva a lista de biomas visitados
        config.set("visited-biomes", new ArrayList<>(playerData.getVisitedBiomes()));
        config.set("rank", playerData.getRank().name());
        config.set("active-playtime-ticks", playerData.getActivePlaytimeTicks());

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
            // Adiciona a insígnia com o timestamp atual.
            data.getEarnedBadgesMap().put(badgeId.toLowerCase(), System.currentTimeMillis());
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
        data.getEarnedBadgesMap().remove(badgeId.toLowerCase());

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

        // Após conceder uma recompensa, verifica se o jogador pode ser promovido.
        plugin.getRankManager().checkAndPromote(player);
    }

    public void removeBadge(Player player, String badgeId) {
        PlayerData data = getPlayerData(player.getUniqueId());
        if (data != null) {
            data.getEarnedBadges().remove(badgeId.toLowerCase());
        }
    }

    public List<String> getEarnedBadges(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        return data != null ? new ArrayList<>(data.getEarnedBadgesMap().keySet()) : new ArrayList<>();
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
                    allCounts.put(uuid, playerData.getConfigurationSection("earned-badges-timed").getKeys(false).size());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Arquivo de jogador com nome inválido ignorado: " + playerFile.getName());
                }
            }
        }

        // Sobrescreve com dados do cache para garantir que os dados de jogadores online estejam atualizados.
        playerDataCache.values().forEach(data -> allCounts.put(data.getPlayerUUID(), data.getEarnedBadgesMap().size()));
        return allCounts;
    }

    /**
     * Obtém a contagem de insígnias de todos os jogadores de forma assíncrona e segura.
     * Este método não bloqueia o thread principal do servidor, evitando lag.
     *
     * @return Um {@link CompletableFuture} que, quando completo, conterá um mapa com o UUID de cada jogador e sua contagem de insígnias.
     */
    public CompletableFuture<Map<UUID, Integer>> getAllTimeBadgeCountsAsync() {
        // Para o ranking de todos os tempos, não precisamos de filtro de tempo.
        return getFilteredBadgeCountsAsync(timestamp -> true);
    }

    /**
     * Obtém a contagem de insígnias conquistadas hoje por todos os jogadores.
     * @return Um CompletableFuture com o mapa de contagens diárias.
     */
    public CompletableFuture<Map<UUID, Integer>> getDailyBadgeCountsAsync() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        return getFilteredBadgeCountsAsync(timestamp -> timestamp >= startOfDay);
    }

    /**
     * Obtém a contagem de insígnias conquistadas no mês atual por todos os jogadores.
     * @return Um CompletableFuture com o mapa de contagens mensais.
     */
    public CompletableFuture<Map<UUID, Integer>> getMonthlyBadgeCountsAsync() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfMonth = cal.getTimeInMillis();

        return getFilteredBadgeCountsAsync(timestamp -> timestamp >= startOfMonth);
    }

    /**
     * Método genérico para obter contagens de insígnias com base em um filtro de tempo.
     * @param timeFilter Um predicado que retorna true se o timestamp da insígnia deve ser contado.
     * @return Um CompletableFuture com o mapa de contagens filtradas.
     */
    private CompletableFuture<Map<UUID, Integer>> getFilteredBadgeCountsAsync(Predicate<Long> timeFilter) {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Integer> filteredCounts = new HashMap<>();
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    try {
                        UUID uuid = UUID.fromString(playerFile.getName().replace(".yml", ""));
                        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                        ConfigurationSection badgesSection = config.getConfigurationSection("earned-badges-timed");
                        if (badgesSection != null) {
                            long count = badgesSection.getKeys(false).stream()
                                    .map(badgesSection::getLong)
                                    .filter(timeFilter)
                                    .count();
                            if (count > 0) filteredCounts.put(uuid, (int) count);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Arquivo de jogador com nome inválido ignorado: " + playerFile.getName());
                    }
                }
            }
            // Garante que os dados de jogadores online (que podem não ter sido salvos ainda) sejam os mais atuais.
            playerDataCache.values().forEach(data -> {
                long count = data.getEarnedBadgesMap().values().stream().filter(timeFilter).count();
                if (count > 0) filteredCounts.put(data.getPlayerUUID(), (int) count);
            });
            return filteredCounts;
        });
    }
}