package com.magnocat.godmode;

import org.bukkit.plugin.java.JavaPlugin;

public final class GodModePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("GodMode-MCTrilhas foi ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GodMode-MCTrilhas foi desativado.");
    }
}