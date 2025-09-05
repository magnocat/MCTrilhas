package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public AdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        // Registra os subcomandos de administração aqui
        registerSubCommand(new AddBadgeSubCommand(plugin));
        registerSubCommand(new RemoveBadgeSubCommand(plugin));
        registerSubCommand(new StatsSubCommand(plugin));
        registerSubCommand(new ForceWebDataSubCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getDescription() {
        return "Gerencia as insígnias dos jogadores.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin <comando> [argumentos]";
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
            sendHelpMessage(sender);
            return;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Comando de administrador desconhecido. Use '/scout admin' para ver a lista de comandos.");
            return;
        }

        // Remove o nome do subcomando dos argumentos para passá-los adiante
        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Comandos de Administração MCTrilhas ---");
        for (SubCommand subCommand : subCommands.values()) {
            sender.sendMessage(ChatColor.AQUA + subCommand.getSyntax() + ChatColor.GRAY + " - " + subCommand.getDescription());
        }
        sender.sendMessage(ChatColor.GOLD + "-----------------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // /scout admin <sub-comando> [argumentos...]
        // args[0] é o nome do sub-comando (ex: "addbadge")

        // Se estiver completando o primeiro argumento (o nome do sub-comando de admin)
        if (args.length == 1) {
            String partialCommand = args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(name -> name.toLowerCase().startsWith(partialCommand))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // Se estiver completando argumentos para um sub-comando de admin específico
        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                // Delega para o sub-comando específico (ex: AddBadgeSubCommand)
                return subCommand.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return Collections.emptyList();
    }
}