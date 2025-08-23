package com.magnocat.godmode;

import com.magnocat.godmode.badge.BadgeManager;
import com.magnocat.godmode.commands.ScoutCommand;
import com.magnocat.godmode.data.PlayerDataManager;
import com.magnocat.godmode.listeners.MiningListener;
import com.magnocat.godmode.listeners.BuilderListener;
import com.magnocat.godmode.listeners.CookingListener;
import com.magnocat.godmode.listeners.LumberjackListener;
import com.magnocat.godmode.listeners.FishingListener;
import com.magnocat.godmode.listeners.PlayerJoinListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class GodModePlugin extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private BadgeManager badgeManager;
    private Economy econ = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        setupEconomy();
        loadManagers();
        registerCommands();
        registerListeners();

        getLogger().info("GodMode-MCTrilhas foi ativado com sucesso!");
    }

    @Override
    public void onDisable() {
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
        this.playerDataManager = new PlayerDataManager(this);
        this.badgeManager = new BadgeManager(this);
        getLogger().info("Gerenciadores de dados e insígnias inicializados.");
    }

    private void registerCommands() {
        getCommand("scout").setExecutor(new ScoutCommand(this));
        getLogger().info("Comandos registrados.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new LumberjackListener(this), this);
        getServer().getPluginManager().registerEvents(new CookingListener(this), this);
        getServer().getPluginManager().registerEvents(new BuilderListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        getLogger().info("Ouvintes de eventos registrados.");
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public BadgeManager getBadgeManager() {
        return badgeManager;
    }

    public Economy getEconomy() {
        return econ;
    }
}