package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class RemoveMemberSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public RemoveMemberSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "remove"; }

    @Override
    public String getDescription() { return "Remove um amigo do seu terreno."; }

    @Override
    public String getSyntax() { return "/terreno remove <jogador>"; }

    @Override
    public String getPermission() { return "mctrilhas.land.removemember"; }

    @Override
    public boolean isAdminCommand() { return false; }

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
        plugin.getLandManager().removeMember((Player) sender, args[0]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // Idealmente, isso listaria os membros do terreno. Por enquanto, lista jogadores online.
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}