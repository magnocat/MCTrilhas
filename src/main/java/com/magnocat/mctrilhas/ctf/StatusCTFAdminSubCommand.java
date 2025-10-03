package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatusCTFAdminSubCommand implements SubCommand {
    private final MCTrilhasPlugin plugin;

    public StatusCTFAdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "status"; }

    @Override
    public String getDescription() { return "Mostra o status da arena em criação."; }

    @Override
    public String getSyntax() { return "/ctf admin status"; }

    @Override
    public String getPermission() { return "mctrilhas.ctf.admin"; }

    @Override
    public boolean isAdminCommand() { return true; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        plugin.getCtfManager().showArenaCreationStatus((Player) sender);
    }
}