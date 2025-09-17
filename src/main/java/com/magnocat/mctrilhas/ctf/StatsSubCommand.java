package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import com.magnocat.mctrilhas.data.PlayerCTFStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class StatsSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    public StatsSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "stats"; }

    @Override
    public String getDescription() { return "Mostra suas estatísticas do CTF."; }

    @Override
    public String getSyntax() { return "/ctf stats [jogador]"; }

    @Override
    public String getPermission() { return "mctrilhas.ctf.stats"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length > 0) {
            if (!sender.hasPermission("mctrilhas.ctf.stats.other")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para ver as estatísticas de outros jogadores.");
                return;
            }
            target = Bukkit.getOfflinePlayer(args[0]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Uso: /ctf stats <jogador>");
            return;
        }

        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado.");
            return;
        }

        PlayerCTFStats stats = plugin.getPlayerDataManager().getPlayerCTFStats(target.getUniqueId());

        sender.sendMessage(ChatColor.GOLD + "--- Estatísticas de CTF de " + target.getName() + " ---");
        sender.sendMessage(ChatColor.AQUA + "Vitórias: " + ChatColor.WHITE + stats.getWins());
        sender.sendMessage(ChatColor.AQUA + "Partidas Jogadas: " + ChatColor.WHITE + stats.getGamesPlayed());
        sender.sendMessage(ChatColor.AQUA + "Abates: " + ChatColor.WHITE + stats.getKills());
        sender.sendMessage(ChatColor.AQUA + "Mortes: " + ChatColor.WHITE + stats.getDeaths());
        sender.sendMessage(ChatColor.AQUA + "K/D Ratio: " + ChatColor.WHITE + df.format(stats.getKdRatio()));
        sender.sendMessage(ChatColor.AQUA + "Capturas de Bandeira: " + ChatColor.WHITE + stats.getFlagCaptures());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}