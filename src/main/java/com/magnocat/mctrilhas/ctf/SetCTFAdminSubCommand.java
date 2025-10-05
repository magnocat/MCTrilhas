package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SetCTFAdminSubCommand implements SubCommand {
    private final MCTrilhasPlugin plugin;
    private final List<String> locationTypes = Arrays.asList("lobby", "red-spawn", "blue-spawn", "red-flag", "blue-flag");

    public SetCTFAdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "set"; }

    @Override
    public String getDescription() { return "Define um local para a arena em criação."; }

    @Override
    public String getSyntax() { return "/ctf admin set <tipo>"; }

    @Override
    public String getPermission() { return "mctrilhas.ctf.admin"; }

    @Override
    public boolean isAdminCommand() { return true; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax() + ". Tipos: " + String.join(", ", locationTypes));
            return;
        }
        plugin.getCtfManager().setArenaLocation((Player) sender, args[0]);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return locationTypes.stream().filter(type -> type.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}