package com.magnocat.mctrilhas.scoreboard;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout board`.
 * <p>
 * Este comando permite que um jogador ative ou desative o painel lateral de
 * estatísticas (scoreboard).
 */
public class BoardSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public BoardSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "board";
    }

    @Override
    public String getDescription() {
        return "Ativa ou desativa o painel de estatísticas.";
    }

    @Override
    public String getSyntax() {
        return "/scout board";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.board";
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }

    @Override
    public boolean isModuleEnabled(MCTrilhasPlugin plugin) {
        return plugin.getScoreboardManager() != null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        ScoreboardManager manager = plugin.getScoreboardManager();
        if (manager != null) {
            manager.toggleBoard((Player) sender);
        } else {
            sender.sendMessage(ChatColor.RED + "O sistema de painel está temporariamente desativado.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
