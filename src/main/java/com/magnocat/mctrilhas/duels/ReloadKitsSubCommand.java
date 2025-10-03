package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout admin duel reloadkits`.
 */
public class ReloadKitsSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ReloadKitsSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reloadkits";
    }

    @Override
    public String getDescription() {
        return "Recarrega os kits de duelo a partir do arquivo duel_kits.yml.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin duel reloadkits";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.admin"; // Permissão geral de admin
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getDuelManager().loadKits();
        sender.sendMessage(ChatColor.GREEN + "Os kits de duelo foram recarregados com sucesso!");
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }
}