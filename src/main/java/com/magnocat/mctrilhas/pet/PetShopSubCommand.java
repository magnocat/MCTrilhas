package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Implementa o subcomando `/scout pet loja`.
 */
public class PetShopSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public PetShopSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "loja"; }

    @Override
    public String getDescription() { return "Abre a loja para adquirir um pet."; }

    @Override
    public String getSyntax() { return "/scout pet loja"; }

    @Override
    public String getPermission() { return "mctrilhas.pet.shop"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem abrir a loja de pets.");
            return;
        }
        plugin.getPetManager().openShop((Player) sender);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}