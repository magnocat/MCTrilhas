package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CancelCTFAdminSubCommand implements SubCommand {
    private final MCTrilhasPlugin plugin;

    public CancelCTFAdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "cancel"; }

    @Override
    public String getDescription() { return "Cancela a criação da arena de CTF."; }

    @Override
    public String getSyntax() { return "/ctf admin cancel"; }

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
        plugin.getCtfManager().cancelArenaCreation((Player) sender);
    }
}