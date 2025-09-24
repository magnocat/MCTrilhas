package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
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
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        plugin.getScoreboardManager().toggleBoard((Player) sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
