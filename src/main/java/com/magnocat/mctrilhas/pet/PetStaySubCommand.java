package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout pet ficar`.
 */
public class PetStaySubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public PetStaySubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "ficar"; }

    @Override
    public String getDescription() { return "Dá a ordem para seu pet ficar parado ou seguir você."; }

    @Override
    public String getSyntax() { return "/scout pet ficar"; }

    @Override
    public String getPermission() { return "mctrilhas.pet.stay"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando.");
            return;
        }
        plugin.getPetManager().togglePetStay((Player) sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}