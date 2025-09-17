package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class JoinSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public JoinSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "join"; }

    @Override
    public String getDescription() { return "Entra na fila para uma partida de CTF."; }

    @Override
    public String getSyntax() { return "/ctf join"; }

    @Override
    public String getPermission() { return "mctrilhas.ctf.join"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores.");
            return;
        }
        Player player = (Player) sender;
        plugin.getCtfManager().addPlayerToQueue(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}