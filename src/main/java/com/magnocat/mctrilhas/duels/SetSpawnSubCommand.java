package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetSpawnSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;
    private final int spawnPoint;

    public SetSpawnSubCommand(MCTrilhasPlugin plugin, int spawnPoint) {
        this.plugin = plugin;
        this.spawnPoint = spawnPoint;
    }

    @Override
    public String getName() {
        return "setspawn" + spawnPoint;
    }

    @Override
    public String getDescription() {
        return "Define o ponto de spawn " + spawnPoint + " para a arena em criação.";
    }

    @Override
    public String getSyntax() {
        return "/scout admin duel setspawn" + spawnPoint;
    }

    @Override
    public String getPermission() {
        return "mctrilhas.scout.admin.duel.setspawn";
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

        Player player = (Player) sender;
        plugin.getDuelManager().setArenaSpawn(player, spawnPoint);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}