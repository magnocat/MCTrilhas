package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ClaimSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ClaimSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "reivindicar"; }

    @Override
    public String getDescription() { return "Reivindica a área selecionada como seu terreno."; }

    @Override
    public String getSyntax() { return "/terreno reivindicar"; }

    @Override
    public String getPermission() { return "mctrilhas.land.claim"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        plugin.getLandManager().createClaim((Player) sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}