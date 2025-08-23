package com.magnocat.godmode;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.magnocat.godmode.badges.BadgeManager;
import com.magnocat.godmode.commands.ScoutCommand;
import com.magnocat.godmode.data.PlayerData;
import com.magnocat.godmode.listeners.BadgeListener;
import com.magnocat.godmode.listeners.PlayerConnectionListener;

import net.milkbowl.vault.economy.Economy;

public class GodModePlugin extends JavaPlugin {
    private Economy economy;
    private BadgeManager badgeManager;
    private PlayerData playerData;

    @Override
    public void onEnable() {
        getLogger().info("GodMode-MCtrilhas iniciando...");
        saveDefaultConfig();

        // Inicializar e verificar dependência do Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault não encontrado! O plugin será desativado, pois é uma dependência essencial.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializar gerenciadores e dados
        this.badgeManager = new BadgeManager();
        this.badgeManager.loadBadges(this);
        this.playerData = new PlayerData(this);

        // Registrar eventos e comandos
        getServer().getPluginManager().registerEvents(new BadgeListener(this, badgeManager, playerData, economy), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(playerData), this);

        // Otimização: Cria uma única instância do comando para o executor e o tab-completer.
        ScoutCommand scoutCommand = new ScoutCommand(badgeManager, playerData);
        getCommand("scout").setExecutor(scoutCommand);
        getCommand("scout").setTabCompleter(scoutCommand);

        // Garante que jogadores online durante um /reload tenham seus dados carregados
        getServer().getOnlinePlayers().forEach(player -> this.playerData.loadPlayerData(player.getUniqueId()));

        getLogger().info("GodMode-MCtrilhas iniciado com sucesso!");
    }

    @Override
    public void onDisable() {
        // Salva os dados de todos os jogadores em cache antes de desligar para evitar perda de dados.
        if (this.playerData != null) {
            this.playerData.saveAllPlayerData();
        }
        getLogger().info("GodMode-MCtrilhas desativado!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        getLogger().info("Vault integrado com sucesso!");
        return economy != null;
    }
}
