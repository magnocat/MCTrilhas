package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SaveCTFAdminSubCommand implements SubCommand {
    private final MCTrilhasPlugin plugin;

    public SaveCTFAdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "save"; }

    @Override
    public String getDescription() { return "Salva a arena de CTF em criação."; }

    @Override
    public String getSyntax() { return "/ctf admin save"; }

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
        plugin.getCtfManager().saveArena((Player) sender);
    }
}