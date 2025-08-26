package com.magnocat.godmode.commands;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScoutCommand implements TabCompleter {

    private final GodModePlugin plugin;

    public ScoutCommand(GodModePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> completions = new ArrayList<>();
        final List<String> subCommands = new ArrayList<>(Arrays.asList("badges", "progress", "top", "toggleprogress", "version"));

        if (sender.hasPermission("godmode.scout.admin")) {
            subCommands.addAll(Arrays.asList("addbadge", "removebadge", "reload"));
        }

        // Tab completion for the first argument (subcommands)
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
            return completions;
        }

        // Tab completion for the second argument
        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("addbadge") || subCommand.equals("removebadge") || subCommand.equals("progress")) {
                if (sender.hasPermission("godmode.scout.admin") || sender.hasPermission("godmode.scout.progress.other")) {
                    // Suggest online player names
                    return sender.getServer().getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> StringUtil.startsWithIgnoreCase(name, args[1]))
                            .collect(Collectors.toList());
                }
            }
        }

        // Tab completion for the third argument
        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("addbadge") || subCommand.equals("removebadge")) {
                if (sender.hasPermission("godmode.scout.admin")) {
                    // Suggest badge IDs from config
                    var badgeSection = plugin.getConfig().getConfigurationSection("badges");
                    if (badgeSection != null) {
                        List<String> badgeIds = new ArrayList<>(badgeSection.getKeys(false));
                        StringUtil.copyPartialMatches(args[2], badgeIds, completions);
                        return completions;
                    }
                }
            }
        }

        return new ArrayList<>(); // Return empty list for other arguments
    }
}