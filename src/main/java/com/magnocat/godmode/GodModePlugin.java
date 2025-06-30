package com.magnocat.godmode;

import com.magnocat.godmode.badges.BadgeManager;
import com.magnocat.godmode.commands.ScoutCommand;
import com.magnocat.godmode.data.PlayerData;
import com.magnocat.godmode.listeners.BadgeListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GodModePlugin extends JavaPlugin {
    private Economy economy;

    @Override
    public void onEnable() {
        getLogger().info("GodMode-MCtrilhas iniciado!");
        saveDefaultConfig();

        // Inicializar Vault
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                getLogger().info("Vault integrado com sucesso!");
            }
        }

        // Inicializar gerenciadores
        BadgeManager badgeManager = new BadgeManager();
        badgeManager.loadBadges(this);
        PlayerData playerData = new PlayerData(this);

        // Registrar eventos e comandos
        getServer().getPluginManager().registerEvents(new BadgeListener(this, badgeManager, playerData, economy), this);
        getCommand("scout").setExecutor(new ScoutCommand(badgeManager, playerData));
    }

    @Override
    public void onDisable() {
        getLogger().info("GodMode-MCtrilhas desativado!");
    }
}
