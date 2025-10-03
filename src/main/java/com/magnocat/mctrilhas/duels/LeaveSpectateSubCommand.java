package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/duelo sair`.
 */
public class LeaveSpectateSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public LeaveSpectateSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "sair"; }

    @Override
    public String getDescription() { return "Para de assistir a um duelo."; }

    @Override
    public String getSyntax() { return "/duelo sair"; }

    @Override
    public String getPermission() { return "mctrilhas.duel.spectate"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return;
        }
        Player spectator = (Player) sender;
        plugin.getDuelManager().stopSpectating(spectator);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}