package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCTFAdminSubCommand implements SubCommand {
    private final MCTrilhasPlugin plugin;

    public CreateCTFAdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "create"; }

    @Override
    public String getDescription() { return "Inicia a criação de uma arena de CTF."; }

    @Override
    public String getSyntax() { return "/ctf admin create <id>"; }

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
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }
        Player player = (Player) sender;
        String arenaId = args[0];
        plugin.getCtfManager().startArenaCreation(player, arenaId);
    }
}