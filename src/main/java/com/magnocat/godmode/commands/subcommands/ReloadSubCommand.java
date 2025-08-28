package com.magnocat.godmode.commands.subcommands;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor
public class ReloadSubCommand extends SubCommand {

    public ReloadSubCommand(GodModePlugin plugin) {
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
        return "godmode.scout.admin";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reloadPluginConfig();
        sender.sendMessage(ChatColor.GREEN + "A configuração do GodMode-MCTrilhas foi recarregada com sucesso!");
    }
}