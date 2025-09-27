package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementa o comando roteador `/scout admin duel`.
 * <p>
 * Gerencia os subcomandos de administração para o sistema de Duelos,
 * como criar arenas, definir kits, etc.
 */
public class AdminDuelSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public AdminDuelSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        // TODO: Registrar os subcomandos de admin de duelo aqui (ex: createarena, setspawn1, etc.)
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
        return "Gerencia o sistema de Duelos (arenas, kits).";
    }

    @Override
    public String getSyntax() {
        return "/scout admin duel <comando> [argumentos]";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.admin.duel";
    }

    @Override
    public boolean isAdminCommand() {
        return true; // É um subcomando do /scout admin
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
            sender.sendMessage(ChatColor.RED + "Comando de admin de duelo desconhecido. Use '/scout admin duel' para ver a lista.");
            sender.sendMessage(ChatColor.YELLOW + "Funcionalidade em desenvolvimento.");
            return;
        }

        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Comandos de Admin de Duelo ---");
        if (subCommands.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Nenhum subcomando disponível ainda. (Em desenvolvimento)");
        } else {
            subCommands.values().stream()
                    .sorted(Comparator.comparing(SubCommand::getName))
                    .forEach(subCmd -> sender.sendMessage(ChatColor.AQUA + subCmd.getSyntax() + ChatColor.GRAY + " - " + subCmd.getDescription()));
        }
        sender.sendMessage(ChatColor.GOLD + "------------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .sorted().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}