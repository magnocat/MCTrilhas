package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetFarewellSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public SetFarewellSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "setfarewell"; }

    @Override
    public String getDescription() { return "Define a mensagem de despedida do seu terreno."; }

    @Override
    public String getSyntax() { return "/terreno setfarewell <mensagem>"; }

    @Override
    public String getPermission() { return "mctrilhas.land.setfarewell"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }
        String message = String.join(" ", args);
        plugin.getLandManager().setFarewellMessage((Player) sender, message); // Corrigido para o nome correto do método
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}