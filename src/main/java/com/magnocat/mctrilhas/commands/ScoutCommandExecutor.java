package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.BadgesSubCommand;
import com.magnocat.mctrilhas.commands.subcommands.AdminSubCommand;
import com.magnocat.mctrilhas.commands.subcommands.GetMapSubCommand;
import com.magnocat.mctrilhas.commands.subcommands.ProgressSubCommand;
import com.magnocat.mctrilhas.commands.subcommands.ReloadSubCommand;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import com.magnocat.mctrilhas.commands.subcommands.VersionSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation") // Suprime avisos de API depreciada (ex: ChatColor)
public class ScoutCommandExecutor implements CommandExecutor, TabCompleter {

    private final MCTrilhasPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public ScoutCommandExecutor(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        // Comandos de jogador
        registerSubCommand(new BadgesSubCommand(plugin));
        registerSubCommand(new ProgressSubCommand(plugin));
        registerSubCommand(new VersionSubCommand(plugin));
        registerSubCommand(new GetMapSubCommand(plugin));
        // Comandos de administração
        registerSubCommand(new ReloadSubCommand(plugin));
        registerSubCommand(new AdminSubCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Comando desconhecido. Use /scout para ver a lista de comandos.");
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        // Passa os argumentos restantes (excluindo o nome do subcomando)
        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Comandos de Escoteiro ---");

        // Comandos de jogador
        subCommands.values().stream()
            .filter(cmd -> !cmd.isAdminCommand() && sender.hasPermission(cmd.getPermission()))
            .sorted(Comparator.comparing(SubCommand::getName))
            .forEach(cmd -> sender.sendMessage(ChatColor.AQUA + cmd.getSyntax() + ChatColor.GRAY + " - " + cmd.getDescription()));

        // Comandos de admin
        subCommands.values().stream()
            .filter(cmd -> cmd.isAdminCommand() && sender.hasPermission(cmd.getPermission()))
            .sorted(Comparator.comparing(SubCommand::getName))
            .forEach(cmd -> sender.sendMessage(ChatColor.RED + cmd.getSyntax() + ChatColor.GRAY + " - " + cmd.getDescription()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Se estiver completando o primeiro argumento (o nome do subcomando)
        if (args.length == 1) {
            String partialCommand = args[0].toLowerCase();
            return subCommands.values().stream()
                    .filter(subCmd -> sender.hasPermission(subCmd.getPermission()))
                    .map(SubCommand::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialCommand))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // Se estiver completando argumentos de um subcomando específico
        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                // Delega a lógica de autocompletar para o subcomando correto
                return subCommand.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }

        return Collections.emptyList();
    }
}