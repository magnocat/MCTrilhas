package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import com.magnocat.mctrilhas.ctf.CTFArena;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ListSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ListSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "list"; }

    @Override
    public String getDescription() { return "Lista as arenas de CTF disponíveis."; }

    @Override
    public String getSyntax() { return "/ctf list"; }

    @Override
    public String getPermission() { return "mctrilhas.ctf.list"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        List<CTFArena> arenas = plugin.getCtfManager().getArenas();
        if (arenas.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nenhuma arena de CTF está configurada no momento.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "--- Arenas de CTF Disponíveis ---");
        for (CTFArena arena : arenas) {
            String formattedName = ChatColor.translateAlternateColorCodes('&', arena.getName());
            sender.sendMessage(ChatColor.AQUA + "- " + formattedName + ChatColor.GRAY + " (para " + arena.getMinPlayers() + "-" + arena.getMaxPlayers() + " jogadores)");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}