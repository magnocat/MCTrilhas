package com.magnocat.mctrilhas.data;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;
import com.magnocat.mctrilhas.ranks.Rank;
import com.magnocat.mctrilhas.utils.ItemFactory;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@SuppressWarnings("deprecation")
public class PlayerDataManager {

    private final MCTrilhasPlugin plugin;
    private final File playerDataFolder;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    private final Map<String, UUID> tokenToUuidCache = new ConcurrentHashMap<>();

    private final Map<UUID, Rank> offlineRankCache = new ConcurrentHashMap<>();
    // Caches para os rankings, populados pelos métodos Async e lidos pelo PlaceholderAPI.
    private final Map<UUID, Integer> dailyBadgeCountsCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> monthlyBadgeCountsCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> allTimeBadgeCountsCache = new ConcurrentHashMap<>();

    public PlayerDataManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
        // Inicia o carregamento inicial do cache de tokens de forma assíncrona.
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::populateTokenCache);
    }

    private void populateTokenCache() {
        plugin.logInfo("Iniciando varredura inicial para cache de tokens de acesso web...");
        File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles == null) return;

        for (File playerFile : playerFiles) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            String token = config.getString("web-access-token");
            if (token != null && !token.isEmpty()) {
                tokenToUuidCache.put(token, UUID.fromString(playerFile.getName().replace(".yml", "")));
            }
        }
        plugin.logInfo("Cache de tokens populado com " + tokenToUuidCache.size() + " tokens.");
    }

    /**
     * Carrega os dados de um jogador do arquivo para o cache.
     * @param uuid O UUID do jogador.
     */
    public void loadPlayerData(UUID uuid) {
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        if (!playerFile.exists()) {
            // Cria um novo objeto PlayerData para jogadores que entram pela primeira vez.
            // O ranque inicial é sempre FILHOTE. O token é nulo até ser gerado.
            playerDataCache.put(uuid, new PlayerData(uuid, new HashMap<>(), new EnumMap<>(BadgeType.class), new HashSet<>(), false, 0, Rank.FILHOTE, 0, new ArrayList<>(), -1, 0, false, new HashSet<>(), null));
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);

        PlayerData playerData = createPlayerDataFromConfig(uuid, config);
        playerDataCache.put(uuid, playerData);

        // Adiciona o token ao cache rápido
        if (playerData.getWebAccessToken() != null && !playerData.getWebAccessToken().isEmpty()) {
            tokenToUuidCache.put(playerData.getWebAccessToken(), uuid);
        }

        // Remove o jogador do cache de ranques offline, pois agora ele está online e seus dados estão no cache principal.
        offlineRankCache.remove(uuid);
    }

    /**
     * Salva os dados de um jogador do cache para o arquivo e o remove do cache.
     * @param uuid O UUID do jogador.
     */
    public void unloadPlayerData(UUID uuid) {
        // Pega os dados do jogador do cache ANTES de removê-lo.
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            savePlayerData(uuid);

            // Adiciona o ranque do jogador ao cache de ranques offline antes de descarregá-lo.
            offlineRankCache.put(uuid, data.getRank());

            playerDataCache.remove(uuid);
            // Agora, com os dados em mãos, remove o token do cache de acesso rápido.
            if (data.getWebAccessToken() != null && !data.getWebAccessToken().isEmpty()) {
                tokenToUuidCache.remove(data.getWebAccessToken());
            }
        }
    }

    /**
     * Carrega os dados de um jogador offline diretamente do arquivo, SEM adicioná-los ao cache principal.
     * Este método é seguro para ser usado em threads assíncronas para ler dados de jogadores offline
     * sem causar race conditions com o login/logout do jogador.
     *
     * @param uuid O UUID do jogador.
     * @return Um objeto PlayerData com os dados do jogador, ou null se o arquivo não existir.
     */
    public PlayerData loadOfflinePlayerData(UUID uuid) {
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        if (!playerFile.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return createPlayerDataFromConfig(uuid, config);
    }

    /**
     * Método central e privado para criar um objeto PlayerData a partir de um FileConfiguration.
     * Contém toda a lógica de leitura e migração de dados, evitando duplicação de código.
     */
    private PlayerData createPlayerDataFromConfig(UUID uuid, FileConfiguration config) {
        // A estrutura de insígnias agora é um Map<String, Long> (badgeId -> timestamp).
        Map<String, Long> earnedBadges = new HashMap<>(); 
        // Lógica de migração: se o formato antigo (lista) existir, converte para o novo (mapa).
        if (config.isList("earned-badges")) {
            plugin.logInfo("Migrando dados de insígnias para o novo formato para o jogador " + uuid);
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

        // Carrega os dados da caça ao tesouro.
        List<String> treasureHuntLocations = config.getStringList("treasure-hunt.locations");
        int currentTreasureHuntStage = config.getInt("treasure-hunt.stage", -1);
        int treasureHuntsCompleted = config.getInt("treasure-hunt.completions", 0);
        boolean hasReceivedGrandPrize = config.getBoolean("treasure-hunt.grand-prize-received", false);

        // Carrega os marcos de CTF já reivindicados.
        List<String> claimedCtfMilestones = config.getStringList("claimed-ctf-milestones");

        // Carrega o token de acesso web, se existir.
        String webAccessToken = config.getString("web-access-token", null);

        // Lógica de migração única para jogadores existentes sem tempo de jogo ativo.
        if (activePlaytimeTicks == 0 && !config.contains("playtime-migrated")) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.hasPlayedBefore()) {
                activePlaytimeTicks = (long) offlinePlayer.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
                config.set("playtime-migrated", true); // Marca que a migração foi feita.
                plugin.logInfo("Migrando tempo de jogo para " + offlinePlayer.getName() + ". Tempo de jogo inicial definido como " + (activePlaytimeTicks / 72000) + " horas.");
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
                    plugin.logWarn("Tipo de progresso inválido '" + typeStr + "' encontrado no arquivo de dados do jogador " + uuid);
                }
            }
        }

        return new PlayerData(uuid, earnedBadges, progressMap, new HashSet<>(visitedBiomesList), progressMessagesDisabled, lastDailyReward, rank, activePlaytimeTicks, treasureHuntLocations, currentTreasureHuntStage, treasureHuntsCompleted, hasReceivedGrandPrize, new HashSet<>(claimedCtfMilestones), webAccessToken);
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
        // Salva os dados da caça ao tesouro.
        config.set("treasure-hunt.locations", playerData.getTreasureHuntLocations());
        config.set("treasure-hunt.stage", playerData.getCurrentTreasureHuntStage());
        config.set("treasure-hunt.completions", playerData.getTreasureHuntsCompleted());
        config.set("treasure-hunt.grand-prize-received", playerData.hasReceivedTreasureGrandPrize());
        config.set("claimed-ctf-milestones", new ArrayList<>(playerData.getClaimedCtfMilestones()));
        config.set("web-access-token", playerData.getWebAccessToken());

        // Salva o mapa de progresso
        playerData.getProgressMap().forEach((type, progress) -> {
            config.set("progress." + type.name(), progress);
        });

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.logSevere("Não foi possível salvar o arquivo de dados para o jogador " + uuid);
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataCache.get(playerUUID);
    }

    /**
     * Encontra o UUID de um jogador de forma rápida usando o cache de tokens.
     * @param token O token de acesso web.
     * @return O UUID do jogador, ou null se não for encontrado.
     */
    public UUID getPlayerUUIDByToken(String token) {
        return tokenToUuidCache.get(token);
    }

    /**
     * Adiciona ou atualiza um token no cache de acesso rápido.
     * Usado quando um novo token é gerado para um jogador online.
     * @param token O novo token.
     * @param uuid O UUID do jogador.
     */
    public void updateTokenCache(String token, UUID uuid) {
        tokenToUuidCache.put(token, uuid);
    }
    /**
     * Obtém o ranque de um jogador. Para jogadores online, lê do cache. Para jogadores offline, lê do arquivo.
     * <p>
     * <strong>Aviso:</strong> A leitura de arquivos para jogadores offline no thread principal pode impactar a performance
     * se usada em larga escala (ex: em uma tablist com muitos jogadores offline).
     *
     * @param uuid O UUID do jogador.
     * @return O {@link Rank} do jogador, ou o ranque padrão (FILHOTE) se não for encontrado.
     */
    public Rank getRank(UUID uuid) {
        // 1. Verifica o cache de jogadores online (mais rápido)
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid).getRank();
        }

        // 2. Verifica o cache de ranques de jogadores offline
        if (offlineRankCache.containsKey(uuid)) {
            return offlineRankCache.get(uuid);
        }

        // 3. Como último recurso, lê do arquivo (operação mais lenta)
        File playerFile = new File(playerDataFolder, uuid.toString() + ".yml");
        if (!playerFile.exists()) {
            return Rank.FILHOTE; // Jogador provavelmente nunca entrou no servidor.
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        Rank rank = Rank.fromString(config.getString("rank", "FILHOTE"));

        // Armazena o resultado no cache offline para futuras requisições rápidas.
        offlineRankCache.put(uuid, rank);
        return rank;
    }

    /**
     * Obtém o próximo ranque na sequência de progressão.
     * @param currentRank O ranque atual.
     * @return O próximo ranque, ou null se o ranque atual for o último.
     */
    public static Rank getNextRank(Rank currentRank) {
        if (currentRank == null) return null;
        Rank[] allRanks = Rank.values();
        int currentOrdinal = currentRank.ordinal();
        if (currentOrdinal + 1 < allRanks.length) {
            return allRanks[currentOrdinal + 1];
        }
        return null; // Não há próximo ranque
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
            plugin.logWarn("Tentativa de zerar progresso para uma insígnia inválida '" + badgeId + "' para o jogador " + player.getName());
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
            ItemStack rewardItem = ItemFactory.createFromConfig(itemSection);
            if (rewardItem != null) {
                player.getInventory().addItem(rewardItem);
            } else {
                plugin.logSevere("Erro ao criar item de recompensa para a insígnia '" + configKey + "'. Verifique a configuração.");
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

        // --- LÓGICA DE ANÚNCIO GLOBAL ---
        if (plugin.getConfig().getBoolean("badge-announcement.enabled", false)) {
            String title = plugin.getConfig().getString("badge-announcement.title", "&6&lINSÍGNIA!");
            String subtitle = plugin.getConfig().getString("badge-announcement.subtitle", "&e{player} &7conquistou a insígnia &b{badgeName}&7!");

            // Substitui os placeholders
            title = ChatColor.translateAlternateColorCodes('&', title.replace("{player}", player.getName()).replace("{badgeName}", badgeName));
            subtitle = ChatColor.translateAlternateColorCodes('&', subtitle.replace("{player}", player.getName()).replace("{badgeName}", badgeName));

            // Envia o título para todos os jogadores online
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                // fadeIn, stay, fadeOut (em ticks)
                onlinePlayer.sendTitle(title, subtitle, 10, 70, 20);
            }
        }

        // Após conceder todas as recompensas, verifica se o jogador pode ser promovido.
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
                    plugin.logWarn("Arquivo de jogador com nome inválido ignorado: " + playerFile.getName());
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
        CompletableFuture<Map<UUID, Integer>> future = getFilteredBadgeCountsAsync(timestamp -> true);
        return future.thenApply(counts -> {
            this.allTimeBadgeCountsCache.clear();
            this.allTimeBadgeCountsCache.putAll(counts);
            return counts;
        });
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

        CompletableFuture<Map<UUID, Integer>> future = getFilteredBadgeCountsAsync(timestamp -> timestamp >= startOfDay);
        return future.thenApply(counts -> {
            this.dailyBadgeCountsCache.clear();
            this.dailyBadgeCountsCache.putAll(counts);
            return counts;
        });
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

        CompletableFuture<Map<UUID, Integer>> future = getFilteredBadgeCountsAsync(timestamp -> timestamp >= startOfMonth);
        return future.thenApply(counts -> {
            this.monthlyBadgeCountsCache.clear();
            this.monthlyBadgeCountsCache.putAll(counts);
            return counts;
        });
    }

    /**
     * Método genérico para obter contagens de insígnias com base em um filtro de tempo.
     * @param timeFilter Um predicado que retorna true se o timestamp da insígnia deve ser contado.
     * @return Um CompletableFuture com o mapa de contagens filtradas.
     */
    private CompletableFuture<Map<UUID, Integer>> getFilteredBadgeCountsAsync(Predicate<Long> timeFilter) {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Integer> filteredCounts = new HashMap<>();
            List<String> hiddenUuids = plugin.getConfig().getStringList("privacy-settings.hide-from-leaderboards");
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    try {
                        UUID uuid = UUID.fromString(playerFile.getName().replace(".yml", ""));
                        // Não inclui jogadores escondidos nos rankings
                        if (hiddenUuids.contains(uuid.toString())) {
                            continue;
                        }

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
                        plugin.logWarn("Arquivo de jogador com nome inválido ignorado: " + playerFile.getName());
                    }
                }
            }
            // Garante que os dados de jogadores online (que podem não ter sido salvos ainda) sejam os mais atuais.
            playerDataCache.values().forEach(data -> {
                // Não inclui jogadores escondidos nos rankings
                if (hiddenUuids.contains(data.getPlayerUUID().toString())) {
                    return; // Continua para o próximo item do forEach
                }

                long count = data.getEarnedBadgesMap().values().stream().filter(timeFilter).count();
                if (count > 0) filteredCounts.put(data.getPlayerUUID(), (int) count);
            });
            return filteredCounts;
        });
    }

    // --- Métodos Síncronos para PlaceholderAPI ---

    /**
     * Obtém a contagem de insígnias diárias de um jogador a partir do cache.
     * Operação síncrona e rápida para o PlaceholderAPI.
     * @param playerUuid O UUID do jogador.
     * @return O número de insígnias ganhas hoje, ou 0 se não estiver no cache.
     */
    public int getDailyBadgeCount(UUID playerUuid) {
        return dailyBadgeCountsCache.getOrDefault(playerUuid, 0);
    }

    /**
     * Obtém a contagem de insígnias mensais de um jogador a partir do cache.
     * @param playerUuid O UUID do jogador.
     * @return O número de insígnias ganhas este mês, ou 0.
     */
    public int getMonthlyBadgeCount(UUID playerUuid) {
        return monthlyBadgeCountsCache.getOrDefault(playerUuid, 0);
    }

    /**
     * Obtém a contagem total de insígnias de um jogador a partir do cache.
     * @param playerUuid O UUID do jogador.
     * @return O número total de insígnias, ou 0.
     */
    public int getAllTimeBadgeCount(UUID playerUuid) {
        return allTimeBadgeCountsCache.getOrDefault(playerUuid, 0);
    }

    /**
     * Obtém a posição de um jogador em um ranking específico a partir do cache.
     * @param playerUuid O UUID do jogador.
     * @param type O tipo de ranking ("daily", "monthly", "alltime").
     * @return A posição do jogador (ex: 1 para primeiro lugar), ou -1 se ele não estiver no ranking.
     */
    public int getRankPosition(UUID playerUuid, String type) {
        Map<UUID, Integer> targetCache;
        switch (type.toLowerCase()) {
            case "daily":
                targetCache = this.dailyBadgeCountsCache;
                break;
            case "monthly":
                targetCache = this.monthlyBadgeCountsCache;
                break;
            case "alltime":
                targetCache = this.allTimeBadgeCountsCache;
                break;
            default:
                return -1; // Tipo de ranking inválido
        }

        // Se o jogador não está no cache (provavelmente tem 0 pontos), ele não tem posição.
        if (!targetCache.containsKey(playerUuid) || targetCache.get(playerUuid) == 0) {
            return -1;
        }

        // Cria uma lista a partir do mapa para poder ordená-la.
        List<Map.Entry<UUID, Integer>> sortedList = new ArrayList<>(targetCache.entrySet());
        // Ordena a lista em ordem decrescente de pontuação.
        sortedList.sort(Map.Entry.<UUID, Integer>comparingByValue().reversed());

        // Encontra a posição do jogador na lista ordenada.
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getKey().equals(playerUuid)) {
                return i + 1; // A posição é o índice + 1.
            }
        }

        return -1; // Não deve acontecer se a chave existe, mas é um fallback seguro.
    }

    // --- Métodos para o Sistema de Estatísticas e Marcos do CTF ---

    public PlayerCTFStats getPlayerCTFStats(UUID playerUUID) {
        File playerFile = new File(playerDataFolder, playerUUID.toString() + ".yml");
        if (!playerFile.exists()) {
            return new PlayerCTFStats(); // Retorna estatísticas zeradas para um novo jogador
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return PlayerCTFStats.fromConfig(config.getConfigurationSection("ctf-stats"));
    }

    public void savePlayerCTFStats(UUID playerUUID, PlayerCTFStats stats) {
        File playerFile = new File(playerDataFolder, playerUUID.toString() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        stats.saveToConfig(config.createSection("ctf-stats"));
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível salvar as estatísticas de CTF para " + playerUUID);
        }
    }

    public boolean hasClaimedCtfMilestone(UUID playerUUID, String milestoneId) {
        PlayerData data = getPlayerData(playerUUID);
        return data != null && data.getClaimedCtfMilestones().contains(milestoneId.toLowerCase());
    }

    public void addClaimedCtfMilestone(UUID playerUUID, String milestoneId) {
        PlayerData data = getPlayerData(playerUUID);
        if (data != null) {
            data.getClaimedCtfMilestones().add(milestoneId.toLowerCase());
        }
    }

    /**
     * Obtém as estatísticas de CTF de todos os jogadores de forma assíncrona.
     * @return Um CompletableFuture com o mapa de UUID para PlayerCTFStats.
     */
    public CompletableFuture<Map<UUID, PlayerCTFStats>> getAllPlayerCTFStatsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, PlayerCTFStats> allStats = new HashMap<>();
            List<String> hiddenUuids = plugin.getConfig().getStringList("privacy-settings.hide-from-leaderboards");
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    try {
                        UUID uuid = UUID.fromString(playerFile.getName().replace(".yml", ""));
                        // Não inclui jogadores escondidos nos rankings
                        if (hiddenUuids.contains(uuid.toString())) {
                            continue;
                        }
                        FileConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                        allStats.put(uuid, PlayerCTFStats.fromConfig(config.getConfigurationSection("ctf-stats")));
                    } catch (Exception e) {
                        plugin.logWarn("Arquivo de jogador com nome inválido ou erro de leitura ignorado: " + playerFile.getName());
                    }
                }
            }

            // Adiciona as estatísticas ao vivo dos jogos em andamento para garantir que o ranking esteja sempre atualizado.
            List<com.magnocat.mctrilhas.ctf.CTFGame> activeGames = plugin.getCtfManager().getActiveGames();
            for (com.magnocat.mctrilhas.ctf.CTFGame game : activeGames) {
                game.getPlayerStats().forEach((uuid, matchStats) -> {
                    // Pega as estatísticas permanentes que já lemos do arquivo.
                    PlayerCTFStats permanentStats = allStats.get(uuid);
                    if (permanentStats != null) {
                        // Cria uma cópia para não modificar o objeto original e soma as estatísticas da partida atual.
                        PlayerCTFStats liveStats = new PlayerCTFStats(permanentStats.getWins(), permanentStats.getLosses(), permanentStats.getKills(), permanentStats.getDeaths(), permanentStats.getFlagCaptures());
                        liveStats.addMatchStats(matchStats, false); // 'won' é false pois a partida não acabou.
                        allStats.put(uuid, liveStats); // Substitui no mapa com os dados ao vivo.
                    }
                });
            }
            return allStats;
        });
    }
}