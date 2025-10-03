package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.SubCommand;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementa o subcomando `/scout admin pet reset <jogador>`.
 */
public class PetResetAdminSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public PetResetAdminSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "reset"; }

    @Override
    public String getDescription() { return "Reseta os dados do pet de um jogador."; }

    @Override
    public String getSyntax() { return "/scout admin pet reset <jogador>"; }

    @Override
    public String getPermission() { return "mctrilhas.admin.pet.reset"; }

    @Override
    public boolean isAdminCommand() { return true; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        PlayerData playerData = plugin.getPlayerDataManager().loadOfflinePlayerData(target.getUniqueId());

        if (playerData == null || playerData.getPetData() == null) {
            sender.sendMessage(ChatColor.RED + "O jogador '" + args[0] + "' não possui dados de pet para resetar.");
            return;
        }

        playerData.setPetData(null);
        plugin.getPlayerDataManager().savePlayerData(playerData);
        sender.sendMessage(ChatColor.GREEN + "Os dados do pet de " + target.getName() + " foram resetados com sucesso.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).collect(Collectors.toList());
        }
        return List.of();
    }
}