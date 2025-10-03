package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/duelo sairfila`.
 */
public class LeaveQueueSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public LeaveQueueSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "sairfila"; }

    @Override
    public String getDescription() { return "Sai da fila de espera para duelos."; }

    @Override
    public String getSyntax() { return "/duelo sairfila"; }

    @Override
    public String getPermission() { return "mctrilhas.duel.queue"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem sair da fila.");
            return;
        }
        Player player = (Player) sender;
        plugin.getDuelManager().leaveQueue(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}