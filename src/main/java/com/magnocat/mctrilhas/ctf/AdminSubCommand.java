package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminSubCommand implements SubCommand {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public AdminSubCommand(MCTrilhasPlugin plugin) {
        registerSubCommand(new CreateCTFAdminSubCommand(plugin));
        registerSubCommand(new SetCTFAdminSubCommand(plugin));
        registerSubCommand(new SaveCTFAdminSubCommand(plugin));
        registerSubCommand(new CancelCTFAdminSubCommand(plugin));
        registerSubCommand(new StatusCTFAdminSubCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public String getName() { return "admin"; }

    @Override
    public String getDescription() { return "Gerencia as arenas de CTF."; }

    @Override
    public String getSyntax() { return "/ctf admin <create|set|save|cancel|status>"; }

    @Override
    public String getPermission() { return "mctrilhas.ctf.admin"; }

    @Override
    public boolean isAdminCommand() { return true; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Subcomando de admin do CTF desconhecido: " + subCommandName);
            return;
        }

        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return subCommands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length > 1) {
            SubCommand sub = subCommands.get(args[0].toLowerCase());
            if (sub != null) {
                return sub.onTabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return Collections.emptyList();
    }
}