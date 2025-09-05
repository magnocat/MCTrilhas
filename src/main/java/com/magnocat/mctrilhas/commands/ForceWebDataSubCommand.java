package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.web.WebDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ForceWebDataSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ForceWebDataSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "forcewebdata";
    }

    @Override
    public String getDescription() {
        return "Força a atualização dos rankings na página web.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin forcewebdata";
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
        sender.sendMessage(ChatColor.YELLOW + "Iniciando a geração manual dos rankings para a página web...");
        plugin.getWebDataManager().forceGenerateAllRankings();
        sender.sendMessage(ChatColor.GREEN + "Geração dos rankings iniciada. Verifique o console para o progresso.");
    }
}