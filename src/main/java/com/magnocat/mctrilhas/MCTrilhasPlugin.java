package com.magnocat.mctrilhas;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.magnocat.mctrilhas.badges.BadgeManager;
import com.magnocat.mctrilhas.commands.DailyCommand;
import com.magnocat.mctrilhas.commands.FamilyCommand;
import com.magnocat.mctrilhas.commands.RankCommand;
import com.magnocat.mctrilhas.commands.ScoutCommandExecutor;
import com.magnocat.mctrilhas.commands.TreasureHuntCommand;
import com.magnocat.mctrilhas.ctf.CTFCommand;
import com.magnocat.mctrilhas.ctf.CTFManager;
import com.magnocat.mctrilhas.ctf.CTFArena; // Import para usar o método parseLocation
import com.magnocat.mctrilhas.ctf.CTFMilestoneManager;
import com.magnocat.mctrilhas.listeners.AdminPrivacyListener;
import com.magnocat.mctrilhas.data.PlayerDataManager;
import com.magnocat.mctrilhas.integrations.MCTrilhasExpansion;
import com.magnocat.mctrilhas.listeners.BuilderListener;
import com.magnocat.mctrilhas.listeners.CommandBlockerListener;
import com.magnocat.mctrilhas.listeners.CookingListener;
import com.magnocat.mctrilhas.listeners.CraftingListener;
import com.magnocat.mctrilhas.listeners.ExplorerListener;
import com.magnocat.mctrilhas.listeners.FarmingListener;
import com.magnocat.mctrilhas.listeners.FishingListener;
import com.magnocat.mctrilhas.listeners.LumberjackListener;
import com.magnocat.mctrilhas.listeners.MenuListener;
import com.magnocat.mctrilhas.listeners.MiningListener;
import com.magnocat.mctrilhas.listeners.PlayerJoinListener;
import com.magnocat.mctrilhas.listeners.PlayerQuitListener;
import com.magnocat.mctrilhas.listeners.TreasureHuntListener;
import com.magnocat.mctrilhas.ctf.CTFListener;
import com.magnocat.mctrilhas.managers.BadgeConfigManager;
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

import org.bukkit.Location;
import net.milkbowl.vault.economy.Economy;

public final class MCTrilhasPlugin extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private BadgeManager badgeManager;
    private BadgeConfigManager badgeConfigManager;
    private BlockPersistenceManager blockPersistenceManager;
    private BadgeMenu badgeMenu;
    private MapRewardManager mapRewardManager;
    private RankManager rankManager;
    private TreasureHuntManager treasureHuntManager;
    private TreasureLocationsManager treasureLocationsManager;
    private TreasureHuntRewardManager treasureHuntRewardManager;
    private HttpApiManager httpApiManager;
    private CTFManager ctfManager;
    private CTFMilestoneManager ctfMilestoneManager;
    // private BlueMapManager blueMapManager; // Comentado temporariamente
    private Economy econ = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        setupEconomy();
        loadManagers();
        registerCommands();
        registerListeners();
        setupPlaceholders();

        // Carrega as arenas do CTF
        ctfManager.loadArenas();

        // Tarefa para atualizar os caches dos rankings periodicamente para o PlaceholderAPI.
        // Isso garante que os dados exibidos no jogo (via TAB, por exemplo) estejam sempre recentes,
        // independentemente do acesso à API web.
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                logInfo("Atualizando caches dos rankings (diário, mensal, geral)...");
                // Apenas chamar os métodos já é o suficiente, pois eles atualizam o cache internamente.
                playerDataManager.getDailyBadgeCountsAsync();
                playerDataManager.getMonthlyBadgeCountsAsync();
                playerDataManager.getAllTimeBadgeCountsAsync();
            }
        }.runTaskTimerAsynchronously(this, 20L * 60, 20L * 60 * 5); // Inicia após 1 minuto, repete a cada 5 minutos

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
        this.badgeConfigManager = new BadgeConfigManager(this);
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
        getCommand("familia").setExecutor(new FamilyCommand(this));
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
                new AdminPrivacyListener(this)
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

    public BadgeConfigManager getBadgeConfigManager() {
        return badgeConfigManager;
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

        badgeConfigManager.reloadBadgeConfig();

        // Notifica o BadgeManager para recarregar sua lista interna de insígnias
        badgeManager.loadBadgesFromConfig();

        // Recarrega os locais de tesouro
        treasureLocationsManager.loadLocations();

        // Recarrega as arenas de CTF
        ctfManager.loadArenas();

        logInfo("As configurações (config.yml) do MCTrilhas foram recarregadas.");
    }

    /**
     * Teleporta um jogador para o local do hub definido no config.yml.
     * @param player O jogador a ser teleportado.
     */
    public void teleportToHub(Player player) {
        String hubLocString = getConfig().getString("server-settings.hub-location");
        Location hubLocation = CTFArena.parseLocation(hubLocString); // Reutiliza o método de parse da arena
        if (hubLocation != null) {
            player.teleport(hubLocation);
        } else {
            logWarn("A localização do hub não está definida ou é inválida no config.yml. O jogador não foi teleportado.");
            player.sendMessage(ChatColor.RED + "Erro: O local de retorno (hub) não está configurado no servidor.");
        }
    }

    // --- Métodos de Log com Cores ---

    public void logInfo(String message) {
        getLogger().info(ChatColor.AQUA + "[MCTrilhas] " + ChatColor.WHITE + message);
    }

    public void logWarn(String message) {
        getLogger().warning(ChatColor.YELLOW + "[MCTrilhas] " + ChatColor.WHITE + message);
    }

    public void logSevere(String message) {
        getLogger().severe(ChatColor.RED + "[MCTrilhas] " + ChatColor.WHITE + message);
    }
}