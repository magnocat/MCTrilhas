package com.magnocat.godmode.commands.subcommands;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class VersionSubCommand extends SubCommand {

    public VersionSubCommand(GodModePlugin plugin) {
        super(plugin);
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
        return "godmode.scout.use";
    }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String version = plugin.getDescription().getVersion();
        sender.sendMessage(ChatColor.GOLD + "GodMode-MCTrilhas " + ChatColor.AQUA + "v" + version);
    }
}