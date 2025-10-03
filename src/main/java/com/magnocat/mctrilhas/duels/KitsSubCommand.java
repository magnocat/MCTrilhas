package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/duelo kits`.
 */
public class KitsSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public KitsSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "kits"; }

    @Override
    public String getDescription() { return "Lista os kits de duelo disponíveis."; }

    @Override
    public String getSyntax() { return "/duelo kits"; }

    @Override
    public String getPermission() { return "mctrilhas.duel.kits"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "--- Kits de Duelo Disponíveis ---");
        if (plugin.getDuelManager().getLoadedKits().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Nenhum kit de duelo foi configurado no servidor.");
        } else {
            plugin.getDuelManager().getLoadedKits().values().forEach(kit -> {
                sender.sendMessage(kit.getDisplayName() + ChatColor.GRAY + " (ID: " + kit.getId() + ")");
                kit.getDescription().forEach(line -> sender.sendMessage(ChatColor.GRAY + "  " + line));
            });
        }
        sender.sendMessage(ChatColor.GOLD + "------------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}