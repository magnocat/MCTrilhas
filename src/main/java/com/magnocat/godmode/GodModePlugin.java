package com.magnocat.godmode;

import org.bukkit.plugin.java.JavaPlugin;

public class GodModePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Lógica de inicialização do plugin
        getLogger().info("GodMode Plugin ativado para MC Trilhas!");
        // Exemplo: Registrar um comando ou evento aqui
    }

    @Override
    public void onDisable() {
        // Lógica de desativação do plugin
        getLogger().info("GodMode Plugin desativado.");
    }
}