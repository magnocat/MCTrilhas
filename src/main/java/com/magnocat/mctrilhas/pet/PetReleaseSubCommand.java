package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout pet liberar`.
 */
public class PetReleaseSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public PetReleaseSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "liberar"; }

    @Override
    public String getDescription() { return "Guarda seu pet invocado."; }

    @Override
    public String getSyntax() { return "/scout pet liberar"; }

    @Override
    public String getPermission() { return "mctrilhas.pet.release"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return;
        }
        plugin.getPetManager().releasePet((Player) sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}