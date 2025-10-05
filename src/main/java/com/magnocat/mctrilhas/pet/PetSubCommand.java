package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementa o comando `/scout pet` e atua como um roteador para seus subcomandos.
 */
public class PetSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public PetSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("invocar", new PetSummonSubCommand(plugin));
        subCommands.put("liberar", new PetReleaseSubCommand(plugin));
        subCommands.put("nome", new PetNameSubCommand(plugin));
        subCommands.put("loja", new PetShopSubCommand(plugin));
        subCommands.put("info", new PetInfoSubCommand(plugin));
        subCommands.put("alimentar", new PetFeedSubCommand(plugin));
        subCommands.put("ficar", new PetStaySubCommand(plugin));
    }

    @Override
    public String getName() { return "pet"; }

    @Override
    public String getDescription() { return "Gerencia seu pet de estimação."; }

    @Override
    public String getSyntax() { return "/scout pet <subcomando>"; }

    @Override
    public String getPermission() { return "mctrilhas.pet.use"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public boolean isModuleEnabled(MCTrilhasPlugin plugin) {
        return plugin.getPetManager() != null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "--- Comandos de Pet ---");
            subCommands.values().forEach(sub -> {
                if (sender.hasPermission(sub.getPermission())) {
                    sender.sendMessage(ChatColor.AQUA + sub.getSyntax() + ChatColor.GRAY + " - " + sub.getDescription());
                }
            });
            return;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Comando de pet desconhecido. Use '/scout pet' para ver a lista.");
            return;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return;
        }

        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream().filter(name -> name.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length > 1) {
            SubCommand sub = subCommands.get(args[0].toLowerCase());
            if (sub != null) {
                return sub.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return List.of();
    }
}