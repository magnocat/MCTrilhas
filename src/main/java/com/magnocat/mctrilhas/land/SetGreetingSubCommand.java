package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetGreetingSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public SetGreetingSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "setgreeting"; }

    @Override
    public String getDescription() { return "Define a mensagem de boas-vindas do seu terreno."; }

    @Override
    public String getSyntax() { return "/terreno setgreeting <mensagem>"; }

    @Override
    public String getPermission() { return "mctrilhas.land.setgreeting"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        String message = String.join(" ", args);
        plugin.getLandManager().setGreetingMessage((Player) sender, message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}