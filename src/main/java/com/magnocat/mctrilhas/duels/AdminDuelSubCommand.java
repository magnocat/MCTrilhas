package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Roteador para os subcomandos de administração de duelos, como /scout admin duel <subcomando>.
 */
public class AdminDuelSubCommand implements SubCommand {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public AdminDuelSubCommand(MCTrilhasPlugin plugin) {
        // Registra todos os subcomandos de admin de duelo aqui
        registerSubCommand(new CreateArenaSubCommand(plugin));
        registerSubCommand(new SetSpawnSubCommand(plugin, 1));
        registerSubCommand(new SetSpawnSubCommand(plugin, 2));
        registerSubCommand(new SetSpecSubCommand(plugin));
        registerSubCommand(new SaveArenaSubCommand(plugin));
        registerSubCommand(new CancelArenaSubCommand(plugin));
        registerSubCommand(new ReloadKitsSubCommand(plugin)); // Nosso novo comando
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public String getName() {
        return "duel";
    }

    @Override
    public String getDescription() {
        return "Gerencia o sistema de duelos.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin duel <subcomando>";
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
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Subcomando de duelo desconhecido: " + subCommandName);
            return;
        }

        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length > 1) {
            SubCommand sub = subCommands.get(args[0].toLowerCase());
            if (sub != null) {
                return sub.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return Collections.emptyList();
    }
}