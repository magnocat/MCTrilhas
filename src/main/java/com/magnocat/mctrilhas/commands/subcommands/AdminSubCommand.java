package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminSubCommand extends SubCommand {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public AdminSubCommand(MCTrilhasPlugin plugin) {
        super(plugin);
        // Registra os subcomandos de administração aqui
        registerSubCommand(new AddBadgeSubCommand(plugin));
        registerSubCommand(new RemoveBadgeSubCommand(plugin));
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
}