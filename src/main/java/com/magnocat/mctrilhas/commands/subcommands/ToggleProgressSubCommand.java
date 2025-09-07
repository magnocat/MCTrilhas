package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ToggleProgressSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ToggleProgressSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "toggleprogress";
    }

    @Override
    public String getDescription() {
        return "Ativa ou desativa as mensagens de progresso.";
    }

    @Override
    public String getSyntax() {
        return "/scout toggleprogress";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.use";
    }

    @Override
    public boolean isAdminCommand() {
        return false;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return;
        }

        Player player = (Player) sender;
        boolean isDisabled = plugin.getPlayerDataManager().toggleProgressMessages(player.getUniqueId());

        if (isDisabled) {
            player.sendMessage(ChatColor.YELLOW + "Mensagens de progresso de insígnias foram " + ChatColor.RED + "desativadas.");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Mensagens de progresso de insígnias foram " + ChatColor.GREEN + "ativadas.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}