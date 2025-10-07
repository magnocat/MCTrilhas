package com.magnocat.mctrilhas;

// Java Standard Library
import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.magnocat.mctrilhas.badges.BadgeManager;
import com.magnocat.mctrilhas.badges.BadgeMenu; // Importa todas as classes do pacote badges
import com.magnocat.mctrilhas.commands.DailyCommand; // Importa todas as classes do pacote commands
import com.magnocat.mctrilhas.commands.FamilyCommand;      // Importa todas as classes do pacote ctf
import com.magnocat.mctrilhas.commands.RulesCommand;    // Importa todas as classes do pacote data
import com.magnocat.mctrilhas.commands.ScoutCommandExecutor;    // Importa todas as classes do pacote duels
import com.magnocat.mctrilhas.ctf.CTFCommand;      // Importa todas as classes do pacote hud
import com.magnocat.mctrilhas.chat.ProximityChatListener;
import com.magnocat.mctrilhas.chat.ProximityChatManager;
import com.magnocat.mctrilhas.ctf.CTFListener;      // Importa todas as classes do pacote npc
import com.magnocat.mctrilhas.ctf.CTFManager; // Importa todas as classes do pacote listeners
import com.magnocat.mctrilhas.ctf.CTFMenuListener;
import com.magnocat.mctrilhas.ctf.CTFMilestoneManager; // Importa todas as classes do pacote integrations
import com.magnocat.mctrilhas.data.ActivityTracker; // Importa todas as classes do pacote quests
import com.magnocat.mctrilhas.data.PlayerDataManager;      // Importa todas as classes do pacote maps
import com.magnocat.mctrilhas.duels.DuelCommand;      // Importa todas as classes do pacote pet
import com.magnocat.mctrilhas.duels.DuelListener;    // Importa todas as classes do pacote ranks
import com.magnocat.mctrilhas.duels.DuelManager; // Importa todas as classes do pacote scoreboard
import com.magnocat.mctrilhas.duels.DuelRewardManager;   // Importa todas as classes do pacote storage
import com.magnocat.mctrilhas.duels.GameListener;   // Importa todas as classes do pacote updater
import com.magnocat.mctrilhas.hud.HUDManager;      // Importa todas as classes do pacote web
import com.magnocat.mctrilhas.integrations.MCTrilhasExpansion;    // Importa todas as classes do pacote utils
import com.magnocat.mctrilhas.land.LandCommand;
import com.magnocat.mctrilhas.land.ClaimToolListener;
import com.magnocat.mctrilhas.land.LandManager;
import com.magnocat.mctrilhas.listeners.AdminPrivacyListener;
import com.magnocat.mctrilhas.listeners.CommandBlockerListener;
import com.magnocat.mctrilhas.listeners.GameChatListener;
import com.magnocat.mctrilhas.listeners.MenuListener;
import com.magnocat.mctrilhas.listeners.PlayerJoinListener;
import com.magnocat.mctrilhas.listeners.PlayerProtectionListener;
import com.magnocat.mctrilhas.listeners.PlayerQuitListener;
import com.magnocat.mctrilhas.listeners.PunishmentListener;
import com.magnocat.mctrilhas.listeners.TreasureHuntListener;
import com.magnocat.mctrilhas.maps.MapRewardManager;
import com.magnocat.mctrilhas.npc.DialogueManager;
import com.magnocat.mctrilhas.npc.NPCListener;
import com.magnocat.mctrilhas.npc.NPCManager;
import com.magnocat.mctrilhas.pet.PetListener;
import com.magnocat.mctrilhas.pet.PetManager;
import com.magnocat.mctrilhas.quests.TreasureHuntCommand;
import com.magnocat.mctrilhas.quests.TreasureHuntManager;
import com.magnocat.mctrilhas.quests.TreasureHuntRewardManager;
import com.magnocat.mctrilhas.quests.TreasureLocationsManager;
import com.magnocat.mctrilhas.ranks.ApadrinharCommand;
import com.magnocat.mctrilhas.ranks.RankCommand;
import com.magnocat.mctrilhas.ranks.RankManager;
import com.magnocat.mctrilhas.scoreboard.ScoreboardManager;
import com.magnocat.mctrilhas.sorting.ChestSortListener;
import com.magnocat.mctrilhas.storage.BlockPersistenceManager;
import com.magnocat.mctrilhas.updater.UpdateChecker;
import com.magnocat.mctrilhas.web.HttpApiManager;

import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;

public final class MCTrilhasPlugin extends JavaPlugin {

    // --- Core Managers ---
    private PlayerDataManager playerDataManager;
    private BadgeManager badgeManager;
    private BlockPersistenceManager blockPersistenceManager;
    private RankManager rankManager;

    // --- Feature Managers ---
    private BadgeMenu badgeMenu;
    private MapRewardManager mapRewardManager;
    private TreasureHuntManager treasureHuntManager;
    private TreasureLocationsManager treasureLocationsManager;
    private TreasureHuntRewardManager treasureHuntRewardManager;
    private CTFManager ctfManager;
    private CTFMilestoneManager ctfMilestoneManager;
    private HUDManager hudManager;
    private ScoreboardManager scoreboardManager;
    private DuelManager duelManager;
    private DuelRewardManager duelRewardManager;
    private PetManager petManager;
    private HttpApiManager httpApiManager;
    private NPCManager npcManager;
    private DialogueManager dialogueManager;
    private ProximityChatManager proximityChatManager;
    private LandManager landManager;

    // --- Integrations & Tasks ---
    private BukkitTask placeholderApiCacheUpdater;
    private BukkitTask webApiCacheUpdater;
    private Economy econ = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        saveResource("duel_arenas.yml", false); // Garante que o arquivo de arenas seja criado
        saveResource("duel_kits.yml", false);
        saveResource("npcs.yml", false);
        setupEconomy();
        loadManagers();

        // Garante que o config.yml no servidor seja atualizado com novas opções
        // sem sobrescrever as configurações existentes do usuário.
        // Isso resolve o problema de novas opções não aparecerem após uma atualização.
        getConfig().options().copyDefaults(true);
        // Inicializa o bStats para coletar métricas anônimas de uso.
        int pluginId = 27457; // Substitua pelo seu ID do bStats
        new Metrics(this, pluginId);
        saveConfig();
        registerCommands();
        registerListeners();
        setupPlaceholders();

        // Inicia as tarefas agendadas para atualizar os caches de ranking.
        // A lógica de iniciar/parar foi movida para o PlayerJoinListener para economizar recursos.
        startCacheUpdateTasks();

        // Inicia o novo servidor de API web.
        httpApiManager.start();

        // Inicia o rastreador de atividade de jogadores.
        new ActivityTracker(this).schedule();

        logInfo("MCTrilhas foi ativado com sucesso!");

        // Inicia a verificação por atualizações.
        new UpdateChecker(this, "magnocat/MCTrilhas").checkForUpdates();

        // Popula os caches de ranking na inicialização para garantir que a API web
        // nunca esteja vazia.
        httpApiManager.updateAllLeaderboardCaches();

        // Restaura os renderizadores de mapas de insígnias para corrigir o bug dos quadros.
        restoreMapRenderers();
    }

    @Override
    public void onDisable() {
        // Para as tarefas agendadas para evitar erros durante o reload.
        if (placeholderApiCacheUpdater != null) placeholderApiCacheUpdater.cancel();
        if (webApiCacheUpdater != null) webApiCacheUpdater.cancel();
        // Para as tarefas do DuelManager
        if (duelManager != null) duelManager.stopTasks();

        // Salva os dados de todos os jogadores online para evitar perda de dados durante um reload ou desligamento.
        if (playerDataManager != null) {
            logInfo("Salvando dados dos jogadores online...");
            // Itera sobre os jogadores online e salva seus dados do cache para o arquivo.
            for (Player player : getServer().getOnlinePlayers()) {
                playerDataManager.unloadPlayerData(player.getUniqueId());
            }
            logInfo("Dados dos jogadores salvos com sucesso.");
        }

        // Para o servidor da API web.
        if (httpApiManager != null) {
            httpApiManager.stop();
        }

        // Para o gerenciador de HUD
        if (hudManager != null) {
            hudManager.stop();
        }

        logInfo("MCTrilhas foi desativado.");
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            logSevere("Vault não encontrado! As recompensas em Totens serão desativadas.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            logSevere("Nenhum provedor de economia do Vault foi encontrado!");
            return;
        }
        econ = rsp.getProvider();
        if (econ != null) {
            logInfo("Vault e um provedor de economia foram encontrados com sucesso!");
        }
    }

    private void loadManagers() {
        // Módulos Essenciais (se falharem, o plugin não deve continuar)
        try {
            this.rankManager = new RankManager(this);
            this.playerDataManager = new PlayerDataManager(this);
            logInfo("Módulos essenciais (Ranques, Dados) carregados.");
        } catch (Exception e) {

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Módulos de Funcionalidades (podem falhar individualmente)
        initWebApiSystem();
        initBadgeSystem();
        initBlockPersistenceSystem();
        initBadgeMenuSystem();
        initMapRewardSystem();
        initTreasureHuntSystem();
        initCtfSystem();
        initHudSystem();
        initScoreboardSystem();
        initDuelSystem();
        initProximityChatSystem();
        initPetSystem();
        initNpcSystem();
        initLandSystem();

        /* Comentado temporariamente para desativar a integração com BlueMap
        // Inicializa integrações opcionais
        if (getServer().getPluginManager().isPluginEnabled("BlueMap")) {
            this.blueMapManager = new BlueMapManager(this);
            getLogger().info("Integração com BlueMap ativada.");
        }*/
        logInfo("Gerenciadores e menus inicializados.");
    }

    private void initLandSystem() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            logWarn("WorldGuard não encontrado. O sistema de proteção de terrenos será desativado.");
            this.landManager = null;
            return;
        }
        this.landManager = new LandManager(this);
    }

    private void initProximityChatSystem() {
        try {
            this.proximityChatManager = new ProximityChatManager(this);
        } catch (Exception e) {
            logSevere("Módulo de Chat por Proximidade falhou ao iniciar e será desativado.", e);
            this.proximityChatManager = null;
        }
    }

    private void initWebApiSystem() {
        try {
            this.httpApiManager = new HttpApiManager(this);
            this.httpApiManager.start();
        } catch (Exception e) {
            logSevere("Módulo de API Web falhou ao iniciar e será desativado.", e);
            this.httpApiManager = null;
        }
    }

    private void initBadgeSystem() {
        try {
            this.badgeManager = new BadgeManager(this);
            this.badgeManager.registerBadgeListeners();
        } catch (Exception e) {
            logSevere("Módulo de Insígnias falhou ao iniciar e será desativado.", e);
            this.badgeManager = null;
        }
    }

    private void initBlockPersistenceSystem() {
        try {
            this.blockPersistenceManager = new BlockPersistenceManager(this);
        } catch (Exception e) {
            logSevere("Módulo de Persistência de Blocos falhou ao iniciar e será desativado.", e);
            this.blockPersistenceManager = null;
        }
    }

    private void initBadgeMenuSystem() {
        try {
            this.badgeMenu = new BadgeMenu(this);
        } catch (Exception e) {
            logSevere("Módulo de Menu de Insígnias falhou ao iniciar e será desativado.", e);
            this.badgeMenu = null;
        }
    }

    private void initMapRewardSystem() {
        try {
            this.mapRewardManager = new MapRewardManager(this);
        } catch (Exception e) {
            logSevere("Módulo de Recompensa de Mapas falhou ao iniciar e será desativado.", e);
            this.mapRewardManager = null;
        }
    }

    private void initTreasureHuntSystem() {
        try {
            this.treasureLocationsManager = new TreasureLocationsManager(this);
            this.treasureHuntManager = new TreasureHuntManager(this);
            this.treasureHuntRewardManager = new TreasureHuntRewardManager(this);
        } catch (Exception e) {
            logSevere("Módulo de Caça ao Tesouro falhou ao iniciar e será desativado.", e);
            this.treasureHuntManager = null;
            this.treasureLocationsManager = null;
            this.treasureHuntRewardManager = null;
        }
    }

    private void initCtfSystem() {
        try {
            this.ctfManager = new CTFManager(this);
            // O CTFManager agora carrega as arenas em seu construtor,
            // mas chamamos explicitamente para garantir o recarregamento em /reload.
            this.ctfManager.loadArenas();
            this.ctfMilestoneManager = new CTFMilestoneManager(this);
        } catch (Exception e) {
            logSevere("Módulo de CTF falhou ao iniciar e será desativado.", e);
            this.ctfManager = null;
        }
    }

    private void initHudSystem() {
        try {
            this.hudManager = new HUDManager(this);
        } catch (Exception e) {
            logSevere("Módulo de HUD falhou ao iniciar e será desativado.", e);
            this.hudManager = null;
        }
    }

    private void initScoreboardSystem() {
        try {
            this.scoreboardManager = new ScoreboardManager(this);
        } catch (Exception e) {
            logSevere("Módulo de Scoreboard falhou ao iniciar e será desativado.", e);
            this.scoreboardManager = null;
        }
    }

    private void initDuelSystem() {
        try {
            this.duelRewardManager = new DuelRewardManager(this);
            this.duelManager = new DuelManager(this);
        } catch (Exception e) {
            logSevere("Módulo de Duelos falhou ao iniciar e será desativado.", e);
            this.duelManager = null;
        }
    }

    private void initPetSystem() {
        try {
            this.petManager = new PetManager(this);
        } catch (Exception e) {
            logSevere("Módulo de Pets falhou ao iniciar e será desativado.", e);
            this.petManager = null;
        }
    }

    private void initNpcSystem() {
        try {
            this.dialogueManager = new DialogueManager(this);
            this.npcManager = new NPCManager(this);
        } catch (Exception e) {
            logSevere("Módulo de NPCs falhou ao iniciar e será desativado.", e);
            this.npcManager = null;
            this.dialogueManager = null;
        }
    }

    private void registerCommands() {
        // O ScoutCommandExecutor gerencia todos os subcomandos do /scout
        ScoutCommandExecutor scoutExecutor = new ScoutCommandExecutor(this);
        getCommand("scout").setExecutor(scoutExecutor);
        getCommand("scout").setTabCompleter(scoutExecutor);
        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("ranque").setExecutor(new RankCommand(this));
        getCommand("familia").setExecutor(new FamilyCommand(this)); // O comando /hud foi movido para /scout hud
        getCommand("regras").setExecutor(new RulesCommand(this));
        getCommand("apadrinhar").setExecutor(new ApadrinharCommand(this));
        if (treasureHuntManager != null) {
            TreasureHuntCommand treasureHuntExecutor = new TreasureHuntCommand(this);
            getCommand("tesouro").setExecutor(treasureHuntExecutor);
            getCommand("tesouro").setTabCompleter(treasureHuntExecutor);
        }
        if (duelManager != null) {
            DuelCommand duelExecutor = new DuelCommand(this);
            getCommand("duelo").setExecutor(duelExecutor);
            getCommand("duelo").setTabCompleter(duelExecutor);
        }
        logInfo("Comandos registrados.");

        // Registra comandos de módulos apenas se eles foram inicializados com sucesso
        if (ctfManager != null) {
            CTFCommand ctfExecutor = new CTFCommand(this);
            getCommand("ctf").setExecutor(ctfExecutor);
            getCommand("ctf").setTabCompleter(ctfExecutor);
        }
        if (landManager != null) {
            LandCommand landExecutor = new LandCommand(this);
            getCommand("terreno").setExecutor(landExecutor);
            getCommand("terreno").setTabCompleter(landExecutor);
        }

    }

    private void registerListeners() {
        List<Listener> listenersToRegister = Arrays.asList(
                // Listeners de insígnias foram movidos para o BadgeManager
                // Listeners essenciais que devem sempre ser registrados
                new PlayerJoinListener(this),
                new PlayerQuitListener(this), // Essencial para salvar os dados do jogador ao sair.
                new CommandBlockerListener(this),
                new AdminPrivacyListener(this),
                new PlayerProtectionListener(this),
                new PunishmentListener(this),
                new ChestSortListener(this) // Adiciona o novo listener de organização de baús
        );

        listenersToRegister.forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));

        // Registra listeners de módulos opcionais apenas se eles foram inicializados com sucesso
        if (getHttpApiManager() != null) getServer().getPluginManager().registerEvents(new GameChatListener(this), this);
        if (getBadgeMenu() != null || getDialogueManager() != null) getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        if (treasureHuntManager != null) getServer().getPluginManager().registerEvents(new TreasureHuntListener(this), this);
        if (petManager != null) getServer().getPluginManager().registerEvents(new PetListener(this), this);
        if (duelManager != null) {
            getServer().getPluginManager().registerEvents(new DuelListener(this), this);
            if (proximityChatManager != null && proximityChatManager.isEnabled()) getServer().getPluginManager().registerEvents(new ProximityChatListener(this), this);
            getServer().getPluginManager().registerEvents(new GameListener(this), this);
        }
        if (npcManager != null) getServer().getPluginManager().registerEvents(new NPCListener(this), this);
        if (ctfManager != null) {
            getServer().getPluginManager().registerEvents(new CTFListener(this), this);
            getServer().getPluginManager().registerEvents(new CTFMenuListener(this), this); // Adicionado o listener do menu
        }
        if (landManager != null) getServer().getPluginManager().registerEvents(new ClaimToolListener(this), this);

        logInfo("Ouvintes de eventos registrados.");
    }


    private void setupPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MCTrilhasExpansion(this).register();
            logInfo("Expansão do PlaceholderAPI registrada com sucesso.");
        } else {
            logInfo("PlaceholderAPI não encontrado. Placeholders estarão desativados.");
        }
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public BadgeManager getBadgeManager() {
        if (badgeManager == null) {
            logWarn("O módulo de Insígnias está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return badgeManager;
    }

    public BlockPersistenceManager getBlockPersistenceManager() {
        if (blockPersistenceManager == null) {
            logWarn("O módulo de Persistência de Blocos está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return blockPersistenceManager;
    }

    public BadgeMenu getBadgeMenu() {
        if (badgeMenu == null) {
            logWarn("O módulo de Menu de Insígnias está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return badgeMenu;
    }

    public MapRewardManager getMapRewardManager() {
        if (mapRewardManager == null) {
            logWarn("O módulo de Recompensa de Mapas está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return mapRewardManager;
    }

    public RankManager getRankManager() {
        if (rankManager == null) {
            logWarn("O módulo de Ranques está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return rankManager;
    }

    public TreasureHuntManager getTreasureHuntManager() {
        if (treasureHuntManager == null) {
            logWarn("O módulo de Caça ao Tesouro está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return treasureHuntManager;
    }

    public TreasureLocationsManager getTreasureLocationsManager() {
        if (treasureLocationsManager == null) {
            logWarn("O módulo de Locais de Tesouro está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return treasureLocationsManager;
    }

    public TreasureHuntRewardManager getTreasureHuntRewardManager() {
        if (treasureHuntRewardManager == null) {
            logWarn("O módulo de Recompensas da Caça ao Tesouro está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return treasureHuntRewardManager;
    }

    public CTFManager getCtfManager() {
        if (ctfManager == null) {
            logWarn("O módulo de CTF está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return ctfManager;
    }

    public CTFMilestoneManager getCtfMilestoneManager() {
        if (ctfMilestoneManager == null) {
            logWarn("O módulo de Marcos de CTF está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return ctfMilestoneManager;
    }

    public HUDManager getHudManager() {
        if (hudManager == null) {
            logWarn("O módulo de HUD está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return hudManager;
    }

    public ScoreboardManager getScoreboardManager() {
        if (scoreboardManager == null) {
            logWarn("O módulo de Scoreboard está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return scoreboardManager;
    }

    public DuelManager getDuelManager() {
        if (duelManager == null) {
            logWarn("O módulo de Duelos está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return this.duelManager;
    }

    public DuelRewardManager getDuelRewardManager() {
        if (duelRewardManager == null) {
            logWarn("O módulo de Recompensas de Duelos está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return duelRewardManager;
    }

    public PetManager getPetManager() {
        if (petManager == null) {
            logWarn("O módulo de Pets está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return petManager;
    }

    public HttpApiManager getHttpApiManager() {
        return httpApiManager;
    }

    public NPCManager getNpcManager() {
        if (npcManager == null) {
            logWarn("O módulo de NPCs está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return npcManager;
    }

    public DialogueManager getDialogueManager() {
        if (dialogueManager == null) {
            logWarn("O módulo de Diálogos está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return dialogueManager;
    }

    public ProximityChatManager getProximityChatManager() {
        if (proximityChatManager == null) {
            logWarn("O módulo de Chat por Proximidade está desativado devido a um erro. Esta funcionalidade não está disponível.");
        }
        return proximityChatManager;
    }

    public LandManager getLandManager() {
        if (landManager == null) {
            logWarn("O módulo de Proteção de Terrenos está desativado. Esta funcionalidade não está disponível.");
        }
        return landManager;
    }

    /* Comentado temporariamente
    public BlueMapManager getBlueMapManager() {
        return blueMapManager;
    }
     */
    public Economy getEconomy() {
        return econ;
    }

    /**
     * Recarrega a configuração do plugin a partir do arquivo config.yml.
     */
    public void reloadPluginConfig() {
        reloadConfig();
        // Tenta reestabelecer a conexão com a economia, caso tenha sido adicionada após o boot.
        setupEconomy();

        // Notifica o BadgeManager para recarregar sua lista interna de insígnias
        if (badgeManager != null) {
            badgeManager.loadBadgesFromConfig();
        }

        // Recarrega os locais de tesouro
        if (treasureLocationsManager != null) {
            treasureLocationsManager.loadLocations();
        }

        // Recarrega as arenas de CTF
        if (ctfManager != null) {
            ctfManager.loadArenas();
        }

        // Recarrega as arenas de Duelo
        if (duelManager != null) {
            duelManager.loadArenas();
        }

        // Recarrega os kits de Duelo
        if (duelManager != null) {
            duelManager.loadKits();
        }

        logInfo("As configurações (config.yml) do MCTrilhas foram recarregadas.");
    }

    /**
     * Teleporta um jogador para o local do hub definido no config.yml.
     *
     * @param player O jogador a ser teleportado.
     */
    public void teleportToHub(Player player) {
        String hubLocString = getConfig().getString("server-settings.hub-location", "world,0.5,100.0,0.5,0,0");
        Location hubLocation = com.magnocat.mctrilhas.ctf.CTFArena.parseLocation(hubLocString); // Reutiliza o método de parse da arena
        if (hubLocation != null) {
            player.teleport(hubLocation);
        } else {
            logWarn("A localização do hub não está definida ou é inválida no config.yml. O jogador não foi teleportado.");
            player.sendMessage(ChatColor.RED + "Erro: O local de retorno (hub) não está configurado no servidor.");
        }
    }

    /**
     * Inicia as tarefas agendadas para atualizar os caches de ranking. Esta
     * função é chamada quando o primeiro jogador entra no servidor.
     */
    public void startCacheUpdateTasks() {
        // Verifica se as tarefas já estão em execução para evitar duplicação.
        if ((placeholderApiCacheUpdater != null && !placeholderApiCacheUpdater.isCancelled())
                || (webApiCacheUpdater != null && !webApiCacheUpdater.isCancelled())) {
            return; // Tarefas já estão ativas.
        }

        logInfo("Primeiro jogador entrou. Iniciando tarefa de atualização de cache de ranking de insígnias.");

        // Tarefa para o PlaceholderAPI (ranking de insígnias)
        placeholderApiCacheUpdater = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                playerDataManager.getDailyBadgeCountsAsync();
                playerDataManager.getMonthlyBadgeCountsAsync();
                playerDataManager.getAllTimeBadgeCountsAsync();
            }
        }.runTaskTimerAsynchronously(this, 20L * 30, 20L * 60 * 5); // Inicia após 30s, repete a cada 5 min

        // Tarefa para a API Web
        // A atualização dos rankings de Duelo e CTF agora é feita sob demanda.
        // Este timer agora atualiza apenas o ranking de insígnias para o site.
        if (httpApiManager != null) {
            httpApiManager.startBadgeCacheUpdater();
        }
    }

    /**
     * Para as tarefas agendadas de atualização de cache. Esta função é chamada
     * quando o último jogador sai do servidor.
     */
    public void stopCacheUpdateTasks() {
        logInfo("Último jogador saiu. Parando tarefas de atualização de cache.");
        if (placeholderApiCacheUpdater != null) {
            placeholderApiCacheUpdater.cancel();
            placeholderApiCacheUpdater = null;
        }
        // Para a tarefa de cache de insígnias da API web.
        httpApiManager.stopBadgeCacheUpdater();
    }

    /**
     * Restaura as imagens dos mapas-troféu que estão em quadros de itens.
     * Este método é chamado na inicialização do plugin para corrigir o problema de
     * mapas ficarem em branco após um reinício do servidor.
     */
    private void restoreMapRenderers() {
        // Delega a lógica de restauração para o MapRewardManager.
        // Isso mantém o código do sistema de mapas encapsulado em seu próprio gerenciador.
        if (getMapRewardManager() != null) {
            getMapRewardManager().restoreAllMapRenderers();
        } else {
            logInfo("Restauração de mapas-troféu pulada, pois o módulo está desativado.");
        }
    }
    // --- Métodos de Log com Cores ---

    public void logInfo(String message) {
        // Removido o prefixo e as cores manuais. O logger do Bukkit já adiciona o nome do plugin.
        // Isso corrige a exibição de caracteres estranhos como '?b' no console.
        getLogger().info(message);
    }

    public void logSevere(String message, Throwable throwable) {
        getLogger().log(java.util.logging.Level.SEVERE, message, throwable);
    }

    public void logWarn(String message) {
        // Removido o prefixo e as cores manuais.
        getLogger().warning(message);
    }

    public void logSevere(String message) {
        // Removido o prefixo e as cores manuais.
        getLogger().severe(message);
    }
}
