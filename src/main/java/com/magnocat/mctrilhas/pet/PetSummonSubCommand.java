package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementa o subcomando `/scout pet invocar <tipo>`.
 */
public class PetSummonSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public PetSummonSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "invocar"; }

    @Override
    public String getDescription() { return "Invoca seu pet de estimação."; }

    @Override
    public String getSyntax() { return "/scout pet invocar <tipo>"; }

    @Override
    public String getPermission() { return "mctrilhas.pet.summon"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem invocar pets.");
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            sender.sendMessage(ChatColor.GRAY + "Tipos disponíveis: lobo, gato, porco, papagaio");
            return;
        }

        Player player = (Player) sender;
        String petType = args[0].toLowerCase();

        plugin.getPetManager().summonPet(player, petType);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Stream.of("lobo", "gato", "porco", "papagaio").filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}