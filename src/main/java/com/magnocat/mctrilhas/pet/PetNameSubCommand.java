package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout pet nome <nome>`.
 */
public class PetNameSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public PetNameSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "nome"; }

    @Override
    public String getDescription() { return "Dá um nome ao seu pet."; }

    @Override
    public String getSyntax() { return "/scout pet nome <nome>"; }

    @Override
    public String getPermission() { return "mctrilhas.pet.name"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem nomear pets.");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        Player player = (Player) sender;
        String newName = String.join(" ", args);

        plugin.getPetManager().renamePet(player, newName);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}