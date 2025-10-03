package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/duelo desistir`.
 */
public class ForfeitSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ForfeitSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "desistir"; }

    @Override
    public String getDescription() { return "Desiste do duelo atual, resultando em um empate."; }

    @Override
    public String getSyntax() { return "/duelo desistir"; }

    @Override
    public String getPermission() { return "mctrilhas.duel.forfeit"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem desistir de duelos.");
            return;
        }

        Player player = (Player) sender;
        plugin.getDuelManager().forfeitDuel(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}