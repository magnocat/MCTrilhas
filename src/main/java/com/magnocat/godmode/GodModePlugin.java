package com.magnocat.godmode;

import org.bukkit.plugin.java.JavaPlugin;

public class GodModePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("GodMode-MCtrilhas iniciado!");
        saveDefaultConfig(); // Cria o config.yml se não existir
    }

    @Override
    public void onDisable() {
        getLogger().info("GodMode-MCtrilhas desativado!");
    }
}
