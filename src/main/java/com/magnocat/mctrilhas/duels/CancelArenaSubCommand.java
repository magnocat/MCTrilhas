package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/duelo cancelarena`.
 */
public class CancelArenaSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public CancelArenaSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "cancelarena"; }

    @Override
    public String getDescription() { return "Cancela a criação da arena de duelo atual."; }

    @Override
    public String getSyntax() { return "/duelo cancelarena"; }

    @Override
    public String getPermission() { return "mctrilhas.duel.admin"; }

    @Override
    public boolean isAdminCommand() { return true; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        Player admin = (Player) sender;
        plugin.getDuelManager().cancelArenaCreation(admin);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}