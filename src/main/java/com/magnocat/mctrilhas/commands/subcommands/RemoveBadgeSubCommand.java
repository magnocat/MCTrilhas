package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemoveBadgeSubCommand extends SubCommand {

    public RemoveBadgeSubCommand(MCTrilhasPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "removebadge";
    }

    @Override
    public String getDescription() {
        return "Remove uma insígnia de um jogador e zera seu progresso.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin removebadge <jogador> <insignia>";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.admin";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso incorreto. Sintaxe: " + getSyntax());
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "O jogador '" + args[0] + "' não está online.");
            return;
        }

        String badgeId = args[1];
        plugin.getPlayerDataManager().removeBadgeAndResetProgress(target, badgeId);
        sender.sendMessage(ChatColor.GREEN + "A insígnia '" + badgeId + "' e seu progresso foram removidos de " + target.getName() + ".");
    }
}