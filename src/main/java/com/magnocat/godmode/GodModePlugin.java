package com.magnocat.godmode;

import com.magnocat.godmode.badges.BadgeManager;
import com.magnocat.godmode.commands.ScoutCommand;
import com.magnocat.godmode.commands.DailyRewardCommandExecutor;
import com.magnocat.godmode.managers.BadgeConfigManager;
import com.magnocat.godmode.data.PlayerDataManager;
import com.magnocat.godmode.listeners.BuilderListener;
import com.magnocat.godmode.listeners.CookingListener;
import com.magnocat.godmode.listeners.FishingListener;
import com.magnocat.godmode.listeners.LumberjackListener;
import com.magnocat.godmode.listeners.MiningListener;
import com.magnocat.godmode.listeners.PlayerJoinListener;
import com.magnocat.godmode.listeners.PlayerQuitListener;
import com.magnocat.godmode.storage.BlockPersistenceManager;
import com.magnocat.godmode.updater.UpdateChecker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public final class GodModePlugin extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private BadgeManager badgeManager;
    private BadgeConfigManager badgeConfigManager;
    private BlockPersistenceManager blockPersistenceManager;
    private Economy econ = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        setupEconomy();
        loadManagers();
        registerCommands();
        registerListeners();

        getLogger().info("GodMode-MCTrilhas foi ativado com sucesso!");

        // Inicia a verificação por atualizações.
        new UpdateChecker(this, "magnocat/GodMode-MCTrilhas").checkForUpdates();
    }

    @Override
    public void onDisable() {
        // Salva os dados de todos os jogadores online para evitar perda de dados durante um reload ou desligamento.
        if (playerDataManager != null) {
            getLogger().info("Salvando dados dos jogadores online...");
            // Itera sobre os jogadores online e salva seus dados do cache para o arquivo.
            for (Player player : getServer().getOnlinePlayers()) {
                playerDataManager.unloadPlayerData(player.getUniqueId());
            }
            getLogger().info("Dados dos jogadores salvos com sucesso.");
        }

        getLogger().info("GodMode-MCTrilhas foi desativado.");
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault não encontrado! As recompensas em Totens serão desativadas.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("Nenhum provedor de economia do Vault foi encontrado!");
            return;
        }
        econ = rsp.getProvider();
        if (econ != null) {
            getLogger().info("Vault e um provedor de economia foram encontrados com sucesso!");
        }
    }

    private void loadManagers() {
        this.badgeConfigManager = new BadgeConfigManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.badgeManager = new BadgeManager(this);
        this.blockPersistenceManager = new BlockPersistenceManager(this);
        getLogger().info("Gerenciadores de configuração, dados, insígnias e persistência de blocos inicializados.");
    }

    private void registerCommands() {
        // ScoutCommand agora implementa CommandExecutor e TabCompleter
        ScoutCommand scoutCommandHandler = new ScoutCommand(this);
        getCommand("scout").setExecutor(scoutCommandHandler);
        getCommand("scout").setTabCompleter(scoutCommandHandler);

        getCommand("daily").setExecutor(new DailyRewardCommandExecutor(this));
        getLogger().info("Comandos registrados.");
    }

    private void registerListeners() {
        List<Listener> listenersToRegister = Arrays.asList(
                new PlayerJoinListener(this),
                new MiningListener(this),
                new LumberjackListener(this),
                new CookingListener(this),
                new BuilderListener(this),
                new FishingListener(this),
                new PlayerQuitListener(this) // Essencial para salvar os dados do jogador ao sair.
        );

        listenersToRegister.forEach(listener -> getServer().getPluginManager().registerEvents(listener, this));
        getLogger().info("Ouvintes de eventos registrados.");
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

    public Economy getEconomy() {
        return econ;
    }

    /**
     * Recarrega a configuração do plugin a partir do arquivo config.yml.
     */
    public void reloadPluginConfig() {
        this.reloadConfig();
        this.badgeConfigManager.reloadBadgeConfig();

        // Notifica o BadgeManager para recarregar sua lista interna de insígnias
        this.badgeManager.loadBadges();

        getLogger().info("As configurações (config.yml e badges.yml) do GodMode-MCTrilhas foram recarregadas.");
    }
}