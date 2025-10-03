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
 * Implementa o comando `/scout admin pet` e atua como um roteador para seus subcomandos.
 */
public class AdminPetSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public AdminPetSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("reset", new PetResetAdminSubCommand(plugin));
        // Futuramente: setlevel, settype, etc.
    }

    @Override
    public String getName() { return "pet"; }

    @Override
    public String getDescription() { return "Gerencia os pets dos jogadores."; }

    @Override
    public String getSyntax() { return "/scout admin pet <subcomando>"; }

    @Override
    public String getPermission() { return "mctrilhas.admin.pet"; }

    @Override
    public boolean isAdminCommand() { return true; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            subCommands.values().forEach(sub -> sender.sendMessage(ChatColor.AQUA + sub.getSyntax() + ChatColor.GRAY + " - " + sub.getDescription()));
            return;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Subcomando de pet desconhecido. Use '/scout admin pet' para ver a lista.");
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