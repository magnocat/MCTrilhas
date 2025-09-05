package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AddBadgeSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public AddBadgeSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // /scout admin addbadge <jogador> <insignia>
        // args[0] -> <jogador>
        // args[1] -> <insignia>

        // Completa o nome do jogador para o primeiro argumento
        if (args.length == 1) {
            return null; // Usa o completador padrão do Bukkit para nomes de jogadores
        }

        // Completa o nome da insígnia para o segundo argumento
        if (args.length == 2) {
            String partialBadge = args[1].toLowerCase();
            Set<String> badgeIds = plugin.getBadgeConfigManager().getBadgeConfig().getConfigurationSection("badges").getKeys(false);

            return badgeIds.stream()
                    .filter(id -> !id.equalsIgnoreCase("use-gui"))
                    .filter(id -> id.toLowerCase().startsWith(partialBadge))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}