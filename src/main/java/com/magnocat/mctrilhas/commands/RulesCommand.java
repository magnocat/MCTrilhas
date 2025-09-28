package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class RulesCommand implements CommandExecutor {

    private final MCTrilhasPlugin plugin;

    public RulesCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> rules = plugin.getConfig().getStringList("server-rules");

        if (rules.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "As regras do servidor ainda não foram configuradas.");
            return true;
        }

        // Envia o cabeçalho
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("server-rules-header", "&6--- [ Regras do Servidor MC Trilhas ] ---")));

        // Envia cada regra
        for (String rule : rules) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', rule));
        }

        return true;
    }
}