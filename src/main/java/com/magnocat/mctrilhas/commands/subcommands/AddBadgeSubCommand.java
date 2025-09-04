package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AddBadgeSubCommand extends SubCommand {

    public AddBadgeSubCommand(MCTrilhasPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "addbadge";
    }

    @Override
    public String getDescription() {
        return "Concede uma insígnia e sua recompensa a um jogador.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin addbadge <jogador> <insignia>";
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
        boolean success = plugin.getPlayerDataManager().grantBadgeAndReward(target, badgeId);

        if (success) {
            sender.sendMessage(ChatColor.GREEN + "A insígnia '" + badgeId + "' foi concedida com sucesso a " + target.getName() + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Não foi possível conceder a insígnia. O jogador já a possui ou a insígnia '" + badgeId + "' não existe.");
        }
    }
}