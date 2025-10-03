package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class CreateArenaSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public CreateArenaSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "createarena";
    }

    @Override
    public String getDescription() {
        return "Inicia a criação de uma nova arena de duelo.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin duel createarena <nome>";
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.admin.duel.create";
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso correto: " + getSyntax());
            return;
        }

        Player player = (Player) sender;
        String arenaName = args[0];

        plugin.getDuelManager().startArenaCreation(player, arenaName);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}