package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TreasureHuntCommand implements CommandExecutor, TabCompleter {

    private final MCTrilhasPlugin plugin;

    public TreasureHuntCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "iniciar":
                plugin.getTreasureHuntManager().startHunt(player);
                break;
            case "pista":
                plugin.getTreasureHuntManager().giveClue(player);
                break;
            case "cancelar":
                plugin.getTreasureHuntManager().cancelHunt(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Comando desconhecido. Use /tesouro para ver a ajuda.");
                break;
        }
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "--- Caça ao Tesouro ---");
        player.sendMessage(ChatColor.AQUA + "/tesouro iniciar" + ChatColor.GRAY + " - Começa uma nova caça ao tesouro.");
        player.sendMessage(ChatColor.AQUA + "/tesouro pista" + ChatColor.GRAY + " - Recebe uma pista para o próximo local.");
        player.sendMessage(ChatColor.AQUA + "/tesouro cancelar" + ChatColor.GRAY + " - Abandona a caça ao tesouro atual.");
        player.sendMessage(ChatColor.GOLD + "-----------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("iniciar", "pista", "cancelar")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}