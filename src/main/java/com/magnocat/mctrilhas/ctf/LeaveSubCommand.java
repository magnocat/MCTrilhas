package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class LeaveSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public LeaveSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "leave"; }

    @Override
    public String getDescription() { return "Sai da fila ou da partida atual de CTF."; }

    @Override
    public String getSyntax() { return "/ctf leave"; }

    @Override
    public String getPermission() { return "mctrilhas.ctf.leave"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores.");
            return;
        }
        Player player = (Player) sender;
        plugin.getCtfManager().handlePlayerLeave(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}