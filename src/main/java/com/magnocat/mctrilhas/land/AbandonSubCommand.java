package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AbandonSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public AbandonSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "abandonar"; }

    @Override
    public String getDescription() { return "Abandona seu terreno protegido."; }

    @Override
    public String getSyntax() { return "/terreno abandonar"; }

    @Override
    public String getPermission() { return "mctrilhas.land.abandon"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        plugin.getLandManager().abandonClaim((Player) sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}