package com.magnocat.godmode.commands;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.commands.subcommands.BadgesSubCommand;
import com.magnocat.godmode.commands.subcommands.ProgressSubCommand;
import com.magnocat.godmode.commands.subcommands.ReloadSubCommand;
import com.magnocat.godmode.commands.subcommands.SubCommand;
import com.magnocat.godmode.commands.subcommands.VersionSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.*;

@SuppressWarnings("deprecation") // Suprime avisos de API depreciada (ex: ChatColor)
public class ScoutCommandExecutor implements CommandExecutor {

    private final GodModePlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public ScoutCommandExecutor(GodModePlugin plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        // TODO: Converta os outros comandos para este novo padrão, seguindo o exemplo.
        // Ex: registerSubCommand(new AddBadgeSubCommand(plugin));

        // Comandos já convertidos:
        registerSubCommand(new BadgesSubCommand(plugin));
        registerSubCommand(new ProgressSubCommand(plugin));
        registerSubCommand(new VersionSubCommand(plugin));
        registerSubCommand(new ReloadSubCommand(plugin));
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
}