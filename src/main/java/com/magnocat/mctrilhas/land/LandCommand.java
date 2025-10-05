package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LandCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public LandCommand(MCTrilhasPlugin plugin) {
        registerSubCommand(new ClaimSubCommand(plugin));
        registerSubCommand(new CostSubCommand(plugin));
        registerSubCommand(new AddMemberSubCommand(plugin));
        registerSubCommand(new RemoveMemberSubCommand(plugin));
        registerSubCommand(new InfoSubCommand(plugin));
        registerSubCommand(new AbandonSubCommand(plugin));
        registerSubCommand(new HomeSubCommand(plugin));
        // registerSubCommand(new SetHomeSubCommand(plugin)); // Desativado temporariamente
        registerSubCommand(new SetGreetingSubCommand(plugin));
        registerSubCommand(new SetFarewellSubCommand(plugin));
        registerSubCommand(new SellSubCommand(plugin));
        registerSubCommand(new AcceptSaleSubCommand(plugin));
        registerSubCommand(new SetFlagSubCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand == null) {
            sendHelp(sender);
            return true;
        }

        if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        subCommand.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .filter(name -> sender.hasPermission(subCommands.get(name).getPermission()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Comandos de Terreno ---");
        subCommands.values().stream()
                .filter(sc -> sc.getPermission() == null || sender.hasPermission(sc.getPermission()))
                .forEach(sc -> sender.sendMessage(ChatColor.AQUA + sc.getSyntax() + " - " + ChatColor.GRAY + sc.getDescription()));
        sender.sendMessage(ChatColor.GOLD + "--------------------------");
    }
}