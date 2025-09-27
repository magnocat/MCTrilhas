package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import com.magnocat.mctrilhas.duels.AcceptSubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementa o comando base /duelo e seus futuros subcomandos.
 */
public class DuelCommand implements CommandExecutor, TabCompleter {
    private final MCTrilhasPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public DuelCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("desafiar", new ChallengeSubCommand(plugin));
        subCommands.put("aceitar", new AcceptSubCommand(plugin));
        subCommands.put("negar", new DenySubCommand(plugin));
        subCommands.put("desistir", new ForfeitSubCommand(plugin));
        subCommands.put("kits", new KitsSubCommand(plugin));
        subCommands.put("stats", new StatsSubCommand(plugin));
        subCommands.put("top", new TopSubCommand(plugin));
        subCommands.put("assistir", new SpectateSubCommand(plugin));
        subCommands.put("sair", new LeaveSpectateSubCommand(plugin));
        subCommands.put("sairfila", new LeaveQueueSubCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Mostra uma mensagem de ajuda geral para o comando /duelo
            sender.sendMessage(ChatColor.GOLD + "--- Sistema de Duelos ---");
            subCommands.values().forEach(sub -> {
                if (!sub.isAdminCommand() || sender.hasPermission("mctrilhas.admin")) {
                    sender.sendMessage(ChatColor.YELLOW + sub.getSyntax() + " - " + ChatColor.GRAY + sub.getDescription());
                }
            });
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Comando desconhecido. Use /duelo para ver os comandos disponíveis.");
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        // Remove o nome do subcomando dos argumentos para passar para o execute
        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            // Sugere os nomes dos subcomandos
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .filter(name -> sender.hasPermission(subCommands.get(name).getPermission()))
                    .collect(Collectors.toList());
        }

        if (args.length > 1) {
            // Delega o tab-complete para o subcomando apropriado
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.onTabComplete(sender, subCommandArgs);
            }
        }

        return new ArrayList<>();
    }
}