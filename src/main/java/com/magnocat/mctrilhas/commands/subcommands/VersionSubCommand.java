package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor and getDescription
public class VersionSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public VersionSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "version";
    }

    @Override
    public String getDescription() {
        return "Exibe a versão do plugin.";
    }

    @Override
    public String getSyntax() {
        return "/scout version";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.use";
    }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String version = plugin.getDescription().getVersion();
        sender.sendMessage(ChatColor.GOLD + "MCTrilhas " + ChatColor.AQUA + "v" + version);
    }
}