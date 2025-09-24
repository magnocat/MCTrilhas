package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/duelo savearena`.
 */
public class SaveArenaSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public SaveArenaSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "savearena"; }

    @Override
    public String getDescription() { return "Salva a arena de duelo em criação."; }

    @Override
    public String getSyntax() { return "/duelo savearena"; }

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
        plugin.getDuelManager().saveArena(admin);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}