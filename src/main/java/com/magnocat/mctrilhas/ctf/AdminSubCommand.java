package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public AdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "admin"; }

    @Override
    public String getDescription() { return "Gerencia as arenas de CTF."; }

    @Override
    public String getSyntax() { return "/ctf admin <create|set|save|cancel|status>"; }

    @Override
    public String getPermission() { return "mctrilhas.ctf.admin"; }

    @Override
    public boolean isAdminCommand() { return true; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Comandos de admin do CTF só podem ser executados por jogadores.");
            return;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendAdminHelp(player);
            return;
        }

        String action = args[0].toLowerCase();
        CTFManager ctfManager = plugin.getCtfManager();

        switch (action) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /ctf admin create <id_da_arena>");
                    return;
                }
                ctfManager.startArenaCreation(player, args[1]);
                break;
            case "set":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /ctf admin set <lobby|redspawn|bluespawn|redflag|blueflag>");
                    return;
                }
                ctfManager.setArenaLocation(player, args[1]);
                break;
            case "save":
                ctfManager.saveArena(player);
                break;
            case "cancel":
                ctfManager.cancelArenaCreation(player);
                break;
            case "status":
                ctfManager.showArenaCreationStatus(player);
                break;
            default:
                sendAdminHelp(player);
                break;
        }
    }

    private void sendAdminHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "--- Comandos de Admin do CTF ---");
        player.sendMessage(ChatColor.AQUA + "/ctf admin create <id>" + ChatColor.GRAY + " - Inicia a criação de uma arena.");
        player.sendMessage(ChatColor.AQUA + "/ctf admin set <tipo>" + ChatColor.GRAY + " - Define um local. Tipos: lobby, redspawn, bluespawn, redflag, blueflag.");
        player.sendMessage(ChatColor.AQUA + "/ctf admin status" + ChatColor.GRAY + " - Mostra o status da criação atual.");
        player.sendMessage(ChatColor.AQUA + "/ctf admin save" + ChatColor.GRAY + " - Salva a arena configurada.");
        player.sendMessage(ChatColor.AQUA + "/ctf admin cancel" + ChatColor.GRAY + " - Cancela a criação da arena.");
        player.sendMessage(ChatColor.GOLD + "---------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "set", "save", "cancel", "status");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Arrays.asList("lobby", "redspawn", "bluespawn", "redflag", "blueflag");
        }
        return Collections.emptyList();
    }
}