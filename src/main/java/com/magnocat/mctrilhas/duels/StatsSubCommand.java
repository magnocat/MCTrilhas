package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementa o subcomando `/duelo stats [jogador]`.
 */
public class StatsSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public StatsSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "stats"; }

    @Override
    public String getDescription() { return "Mostra as estatísticas de duelo."; }

    @Override
    public String getSyntax() { return "/duelo stats [jogador]"; }

    @Override
    public String getPermission() { return "mctrilhas.duel.stats"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Especifique um jogador para ver as estatísticas.");
                return;
            }
            Player player = (Player) sender;
            displayStats(sender, player.getUniqueId(), player.getName());
        } else {
            if (!sender.hasPermission("mctrilhas.duel.stats.other")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para ver as estatísticas de outros jogadores.");
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + args[0] + "' não encontrado.");
                return;
            }
            displayStats(sender, target.getUniqueId(), target.getName());
        }
    }

    private void displayStats(CommandSender sender, UUID targetUUID, String targetName) {
        sender.sendMessage(ChatColor.YELLOW + "Buscando estatísticas de " + targetName + "...");

        // Executa a leitura de dados de forma assíncrona para não travar o servidor.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerDuelStats stats = plugin.getPlayerDataManager().getPlayerDuelStats(targetUUID);
            int wins = stats.getWins();
            int losses = stats.getLosses();
            int elo = stats.getElo();
            int totalGames = wins + losses;
            double winRate = (totalGames == 0) ? 0.0 : ((double) wins / totalGames) * 100;

            // Volta para a thread principal para enviar as mensagens ao jogador.
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "--- Estatísticas de Duelo: " + ChatColor.WHITE + targetName + ChatColor.GOLD + " ---");
                sender.sendMessage(ChatColor.YELLOW + "ELO Rating: " + ChatColor.WHITE + elo);
                sender.sendMessage(ChatColor.GREEN + "Vitórias: " + ChatColor.WHITE + wins);
                sender.sendMessage(ChatColor.RED + "Derrotas: " + ChatColor.WHITE + losses);
                sender.sendMessage(ChatColor.BLUE + "Partidas: " + ChatColor.WHITE + totalGames);
                sender.sendMessage(String.format(ChatColor.AQUA + "Taxa de Vitória: " + ChatColor.WHITE + "%.2f%%", winRate));
                sender.sendMessage(ChatColor.GOLD + "------------------------------------");
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("mctrilhas.duel.stats.other")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return List.of();
    }
}