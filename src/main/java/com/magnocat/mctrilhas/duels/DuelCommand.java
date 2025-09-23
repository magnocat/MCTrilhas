package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Implementa o comando base /duelo e seus futuros subcomandos.
 */
public class DuelCommand implements CommandExecutor {
    private final MCTrilhasPlugin plugin;
    private final DuelManager duelManager;

    public DuelCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.duelManager = plugin.getDuelManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // A lógica dos subcomandos (desafiar, aceitar, etc.) virá aqui.
        sender.sendMessage(ChatColor.GOLD + "[Duelos] " + ChatColor.GRAY + "Sistema de Duelos em desenvolvimento.");
        return true;
    }
}