package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Implementa o subcomando de administrador `/scout admin reload`.
 * <p>
 * Este comando recarrega todos os arquivos de configuração do plugin
 * (config.yml, treasure_locations.yml, etc.) e atualiza os caches
 * dependentes, como a lista de insígnias e locais de tesouro.
 */
@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor
public class ReloadSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ReloadSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recarrega os arquivos de configuração e caches do plugin.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin reload";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.admin";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    /**
     * Executa a lógica de recarregamento do plugin.
     *
     * @param sender A entidade que executou o comando.
     * @param args Argumentos do comando (não utilizados neste subcomando).
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Recarregando configurações do MCTrilhas...");

        plugin.reloadConfig();
        plugin.getBadgeManager().loadBadgesFromConfig();
        plugin.getTreasureLocationsManager().loadLocations();

        sender.sendMessage(ChatColor.GREEN + "Configurações do MCTrilhas recarregadas com sucesso!");
    }
}