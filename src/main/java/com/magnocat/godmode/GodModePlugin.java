package com.magnocat.godmode;

import org.bukkit.plugin.java.JavaPlugin;

public class GodModePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("MC Trilhas plugin iniciado!");
        saveDefaultConfig(); // Cria o config.yml se não existir
        // Registrar comandos e eventos aqui
    }

    @Override
    public void onDisable() {
        getLogger().info("MC Trilhas plugin desativado!");
    }
}
