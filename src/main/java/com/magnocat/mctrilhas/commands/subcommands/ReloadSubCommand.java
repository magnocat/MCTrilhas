package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor
public class ReloadSubCommand extends SubCommand {

    public ReloadSubCommand(MCTrilhasPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Recarrega os arquivos de configuração (config.yml e badges.yml).";
    }

    @Override
    public String getSyntax() {
        return "/scout reload";
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
        plugin.reloadPluginConfig();
        sender.sendMessage(ChatColor.GREEN + "A configuração do MCTrilhas foi recarregada com sucesso!");
    }
}