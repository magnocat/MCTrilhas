package com.magnocat.mctrilhas.commands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import com.magnocat.mctrilhas.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class RankCommand implements CommandExecutor {

    private final MCTrilhasPlugin plugin;

    public RankCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Não foi possível carregar seus dados. Tente novamente em alguns instantes.");
            return true;
        }

        Rank currentRank = playerData.getRank();
        player.sendMessage(ChatColor.GOLD + "--- Seu Progresso de Ranque ---");
        player.sendMessage(ChatColor.YELLOW + "Seu ranque atual: " + currentRank.getColor() + currentRank.getDisplayName());

        if (currentRank == Rank.PIONEIRO || currentRank == Rank.CHEFE) {
            player.sendMessage(ChatColor.GREEN + "Você alcançou o topo da jornada escoteira! Parabéns!");
            player.sendMessage(ChatColor.GOLD + "---------------------------------");
            return true;
        }

        player.sendMessage(""); // Linha em branco para espaçamento

        // A lógica de exibição foi movida para MessageUtils
        MessageUtils.displayRankProgress(player, player, playerData, plugin);

        player.sendMessage(ChatColor.GOLD + "---------------------------------");

        return true;
    }
}