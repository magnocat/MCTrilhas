package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class ScoutCommandExecutor implements CommandExecutor, TabCompleter {

    private final MCTrilhasPlugin plugin;
    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

    public ScoutCommandExecutor(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        // Registra todos os subcomandos na ordem desejada para o help
        registerSubCommand(new BadgesSubCommand(plugin));
        registerSubCommand(new ProgressSubCommand(plugin));
        registerSubCommand(new GetMapSubCommand(plugin));
        registerSubCommand(new ToggleProgressSubCommand(plugin));
        registerSubCommand(new VersionSubCommand(plugin));
        registerSubCommand(new ReloadSubCommand(plugin));
        registerSubCommand(new AdminSubCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Comando desconhecido. Use '/scout' para ver a lista de comandos.");
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Comandos do MCTrilhas ---");
        // Exibe os comandos de jogador
        for (SubCommand subCommand : subCommands.values()) {
            // Pula o comando 'admin' em si, pois seus subcomandos serão listados separadamente
            if (subCommand.isAdminCommand()) {
                continue;
            }
            if (sender.hasPermission(subCommand.getPermission())) {
                sender.sendMessage(ChatColor.AQUA + subCommand.getSyntax() + ChatColor.GRAY + " - " + subCommand.getDescription());
            }
        }

        // Se o remetente for um administrador, exibe a seção de administração
        SubCommand adminCommand = subCommands.get("admin");
        if (adminCommand != null && sender.hasPermission(adminCommand.getPermission())) {
            sender.sendMessage(""); // Linha de espaçamento
            sender.sendMessage(ChatColor.RED + "--- Comandos de Admin ---");
            if (adminCommand instanceof AdminSubCommand) {
                AdminSubCommand adminSubCommand = (AdminSubCommand) adminCommand;
                // Itera sobre os subcomandos internos do AdminSubCommand
                for (SubCommand adminSub : adminSubCommand.getAdminSubCommands().values()) {
                    sender.sendMessage(ChatColor.AQUA + adminSub.getSyntax() + ChatColor.GRAY + " - " + adminSub.getDescription());
                }
            }
        }
        // Adiciona a dica sobre o saldo de Totens
        if (plugin.getEconomy() != null) {
            sender.sendMessage(ChatColor.GRAY + "Para ver seu saldo de Totens, use /balance.");
        }
        sender.sendMessage(ChatColor.GOLD + "-----------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partialCommand = args[0].toLowerCase();
            return subCommands.keySet().stream()
                    .filter(name -> sender.hasPermission(subCommands.get(name).getPermission()))
                    .filter(name -> name.toLowerCase().startsWith(partialCommand))
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                return subCommand.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return Collections.emptyList();
    }
}