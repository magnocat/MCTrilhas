package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implements the `/scout pet feed` subcommand.
 */
public class PetFeedSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public PetFeedSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "alimentar"; }

    @Override
    public String getDescription() { return "Alimenta seu pet para aumentar a felicidade."; }

    @Override
    public String getSyntax() { return "/scout pet alimentar"; }

    @Override
    public String getPermission() { return "mctrilhas.pet.alimentar"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem alimentar pets.");
            return;
        }
        plugin.getPetManager().feedPet((Player) sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}