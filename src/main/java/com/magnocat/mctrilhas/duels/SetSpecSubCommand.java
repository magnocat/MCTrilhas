package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/duelo setspec`.
 */
public class SetSpecSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public SetSpecSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "setspec"; }

    @Override
    public String getDescription() { return "Define o ponto de spawn dos espectadores."; }

    @Override
    public String getSyntax() { return "/scout admin duel setspec"; }

    @Override
    public String getPermission() { return "mctrilhas.scout.admin.duel.setspec"; }

    @Override
    public boolean isAdminCommand() { return true; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        Player admin = (Player) sender;
        plugin.getDuelManager().setArenaSpectatorPosition(admin);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}