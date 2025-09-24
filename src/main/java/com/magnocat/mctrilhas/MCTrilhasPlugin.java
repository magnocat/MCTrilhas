package com.magnocat.mctrilhas;

// Java Standard Library
import java.util.Arrays;
import java.util.List;

// Bukkit & Spigot API
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

// Vault API
import net.milkbowl.vault.economy.Economy;

// Project-specific Classes
import com.magnocat.mctrilhas.badges.BadgeManager;
import com.magnocat.mctrilhas.commands.DailyCommand;
import com.magnocat.mctrilhas.commands.FamilyCommand;
import com.magnocat.mctrilhas.commands.RankCommand;
import com.magnocat.mctrilhas.commands.ScoutCommandExecutor;
import com.magnocat.mctrilhas.commands.TreasureHuntCommand;
import com.magnocat.mctrilhas.ctf.CTFCommand;
import com.magnocat.mctrilhas.ctf.CTFManager;
import com.magnocat.mctrilhas.ctf.CTFMilestoneManager;
import com.magnocat.mctrilhas.duels.DuelCommand;
import com.magnocat.mctrilhas.duels.DuelManager;
import com.magnocat.mctrilhas.duels.DuelRewardManager;
import com.magnocat.mctrilhas.listeners.AdminPrivacyListener;
import com.magnocat.mctrilhas.data.PlayerDataManager;
import com.magnocat.mctrilhas.integrations.MCTrilhasExpansion;
import com.magnocat.mctrilhas.listeners.BuilderListener;
import com.magnocat.mctrilhas.listeners.CommandBlockerListener;
import com.magnocat.mctrilhas.listeners.CookingListener;
import com.magnocat.mctrilhas.listeners.CraftingListener;
import com.magnocat.mctrilhas.listeners.ExplorerListener;
import com.magnocat.mctrilhas.listeners.FarmingListener;
import com.magnocat.mctrilhas.listeners.GameChatListener;
import com.magnocat.mctrilhas.listeners.FishingListener;
import com.magnocat.mctrilhas.listeners.LumberjackListener;
import com.magnocat.mctrilhas.listeners.MenuListener;
import com.magnocat.mctrilhas.listeners.MiningListener;
import com.magnocat.mctrilhas.listeners.PlayerJoinListener;
import com.magnocat.mctrilhas.listeners.PlayerQuitListener;
import com.magnocat.mctrilhas.listeners.TreasureHuntListener;
import com.magnocat.mctrilhas.ctf.CTFListener;
import com.magnocat.mctrilhas.pet.PetManager;
import com.magnocat.mctrilhas.maps.MapRewardManager;
import com.magnocat.mctrilhas.menus.BadgeMenu;
import com.magnocat.mctrilhas.quests.TreasureHuntManager;
import com.magnocat.mctrilhas.quests.TreasureHuntRewardManager;
import com.magnocat.mctrilhas.quests.TreasureLocationsManager;
import com.magnocat.mctrilhas.ranks.RankManager;
import com.magnocat.mctrilhas.storage.BlockPersistenceManager;
import com.magnocat.mctrilhas.data.ActivityTracker;
import com.magnocat.mctrilhas.updater.UpdateChecker;
import com.magnocat.mctrilhas.web.HttpApiManager;
import com.magnocat.mctrilhas.hud.HUDManager;
import com.magnocat.mctrilhas.scoreboard.ScoreboardManager;

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

    // --- Integrations & Tasks ---
    private BukkitTask placeholderApiCacheUpdater;
    private BukkitTask webApiCacheUpdater;
    private Economy econ = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        saveResource("duel_kits.yml", false);
        saveResource("duel_arenas.yml", false); // Garante que o arquivo de arenas seja criado
        setupEconomy();
        loadManagers();

        // Garante que o config.yml no servidor seja atualizado com novas opções
        // sem sobrescrever as configurações existentes do usuário.
        // Isso resolve o problema de novas opções não aparecerem após uma atualização.
        getConfig().options().copyDefaults(true);
        saveConfig();
        registerCommands();
        registerListeners();
        setupPlaceholders();

        // Inicia o novo servidor de API web.
        httpApiManager.start();

        // Inicia o rastreador de atividade de jogadores.
        new ActivityTracker(this).schedule();

        logInfo("MCTrilhas foi ativado com sucesso!");

        // Inicia a verificação por atualizações.
        new UpdateChecker(this, "magnocat/MCTrilhas").checkForUpdates();
    }

    @Override
    public void onDisable() {
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

        // Para as tarefas do DuelManager
        if (duelManager != null) {
            duelManager.stopTasks();
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
        this.playerDataManager = new PlayerDataManager(this);
        this.badgeManager = new BadgeManager(this);
        this.blockPersistenceManager = new BlockPersistenceManager(this);
        this.badgeMenu = new BadgeMenu(this);
        this.mapRewardManager = new MapRewardManager(this);
        this.rankManager = new RankManager(this);
        this.treasureHuntManager = new TreasureHuntManager(this);
        this.treasureLocationsManager = new TreasureLocationsManager(this);
        this.treasureHuntRewardManager = new TreasureHuntRewardManager(this);
        this.httpApiManager = new HttpApiManager(this);
        this.ctfManager = new CTFManager(this);
        this.ctfMilestoneManager = new CTFMilestoneManager(this);
        this.hudManager = new HUDManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.duelManager = new DuelManager(this);
        this.duelRewardManager = new DuelRewardManager(this);
        this.petManager = new PetManager(this);

        /* Comentado temporariamente para desativar a integração com BlueMap
        // Inicializa integrações opcionais
        if (getServer().getPluginManager().isPluginEnabled("BlueMap")) {
            this.blueMapManager = new BlueMapManager(this);
            getLogger().info("Integração com BlueMap ativada.");
        }*/
        logInfo("Gerenciadores e menus inicializados.");
    }

    private void registerCommands() {
        // O ScoutCommandExecutor gerencia todos os subcomandos do /scout
        ScoutCommandExecutor scoutExecutor = new ScoutCommandExecutor(this);
        getCommand("scout").setExecutor(scoutExecutor);
        getCommand("scout").setTabCompleter(scoutExecutor);
        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("ranque").setExecutor(new RankCommand(this));
        TreasureHuntCommand treasureHuntExecutor = new TreasureHuntCommand(this);
        getCommand("tesouro").setExecutor(treasureHuntExecutor);
        getCommand("tesouro").setTabCompleter(treasureHuntExecutor);
        CTFCommand ctfExecutor = new CTFCommand(this);
        getCommand("ctf").setExecutor(ctfExecutor);
        getCommand("ctf").setTabCompleter(ctfExecutor);
        getCommand("familia").setExecutor(new FamilyCommand(this)); // O comando /hud foi movido para /scout hud
        DuelCommand duelExecutor = new DuelCommand(this);
        getCommand("duelo").setExecutor(duelExecutor);
        getCommand("duelo").setTabCompleter(duelExecutor);
        logInfo("Comandos registrados.");
    }

    private void registerListeners() {
        List<Listener> listenersToRegister = Arrays.asList(
                new PlayerJoinListener(this),
                new MiningListener(this),
                new LumberjackListener(this),
                new CookingListener(this),
                new BuilderListener(this),
                new FishingListener(this),
                new FarmingListener(this),
                new CraftingListener(this),
                new ExplorerListener(this),
                new PlayerQuitListener(this), // Essencial para salvar os dados do jogador ao sair.
                new MenuListener(),
                new TreasureHuntListener(this),
                new CTFListener(this),
                new CommandBlockerListener(this),
                new AdminPrivacyListener(this),
                new GameChatListener(this)
        );

        listenersToRegister.forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
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
        return badgeManager;
    }

    public BlockPersistenceManager getBlockPersistenceManager() {
        return blockPersistenceManager;
    }

    public BadgeMenu getBadgeMenu() {
        return badgeMenu;
    }

    public MapRewardManager getMapRewardManager() {
        return mapRewardManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public TreasureHuntManager getTreasureHuntManager() {
        return treasureHuntManager;
    }

    public TreasureLocationsManager getTreasureLocationsManager() {
        return treasureLocationsManager;
    }

    public TreasureHuntRewardManager getTreasureHuntRewardManager() {
        return treasureHuntRewardManager;
    }

    public CTFManager getCtfManager() {
        return ctfManager;
    }

    public CTFMilestoneManager getCtfMilestoneManager() {
        return ctfMilestoneManager;
    }

    public HUDManager getHudManager() {
        return hudManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public DuelRewardManager getDuelRewardManager() {
        return duelRewardManager;
    }

    public PetManager getPetManager() {
        return petManager;
    }

    public HttpApiManager getHttpApiManager() {
        return httpApiManager;
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
        badgeManager.loadBadgesFromConfig();

        // Recarrega os locais de tesouro
        treasureLocationsManager.loadLocations();

        // Recarrega as arenas de CTF
        ctfManager.loadArenas();

        // Recarrega as arenas de Duelo
        duelManager.loadArenas();

        // Recarrega os kits de Duelo
        duelManager.loadKits();

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

        logInfo("Primeiro jogador entrou. Iniciando tarefas de atualização de cache de rankings.");

        // Tarefa para o PlaceholderAPI
        placeholderApiCacheUpdater = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                logInfo("Atualizando caches de ranking para PlaceholderAPI...");
                playerDataManager.getDailyBadgeCountsAsync();
                playerDataManager.getMonthlyBadgeCountsAsync();
                playerDataManager.getAllTimeBadgeCountsAsync();
            }
        }.runTaskTimerAsynchronously(this, 20L * 60, 20L * 60 * 5); // Inicia após 1 min, repete a cada 5 min

        // Tarefa para a API Web
        if (httpApiManager != null) {
            webApiCacheUpdater = new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    httpApiManager.updateAllLeaderboardCaches();
                }
            }.runTaskTimerAsynchronously(this, 20L * 10, 20L * 60 * 5); // Inicia após 10s, repete a cada 5 min
        }
    }

    /**
     * Para as tarefas agendadas de atualização de cache. Esta função é chamada
     * quando o último jogador sai do servidor.
     */
    public void stopCacheUpdateTasks() {
        logInfo("Último jogador saiu. Parando tarefas de atualização de cache de rankings.");
        if (placeholderApiCacheUpdater != null) {
            placeholderApiCacheUpdater.cancel();
            placeholderApiCacheUpdater = null;
        }
        if (webApiCacheUpdater != null) {
            webApiCacheUpdater.cancel();
            webApiCacheUpdater = null;
        }
    }
    // --- Métodos de Log com Cores ---

    public void logInfo(String message) {
        // Removido o prefixo e as cores manuais. O logger do Bukkit já adiciona o nome do plugin.
        // Isso corrige a exibição de caracteres estranhos como '?b' no console.
        getLogger().info(message);
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
