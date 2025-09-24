package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.commands.subcommands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementa o subcomando `/duelo top`.
 */
public class TopSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public TopSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "top"; }

    @Override
    public String getDescription() { return "Mostra o ranking dos melhores duelistas por ELO."; }

    @Override
    public String getSyntax() { return "/duelo top"; }

    @Override
    public String getPermission() { return "mctrilhas.duel.top"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Calculando o ranking de ELO, por favor aguarde...");

        plugin.getPlayerDataManager().getAllPlayerDuelStatsAsync().thenAcceptAsync(allStats -> {
            // Sort the stats by ELO in descending order
            Map<UUID, PlayerDuelStats> sortedStats = allStats.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(PlayerDuelStats::getElo).reversed()))
                    .limit(10) // Limit to top 10
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));

            // Bukkit API calls must be on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "--- Top 10 Duelistas (Ranking ELO) ---");
                int rank = 1;
                if (sortedStats.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "Nenhum jogador no ranking ainda.");
                } else {
                    for (Map.Entry<UUID, PlayerDuelStats> entry : sortedStats.entrySet()) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
                        String name = player.getName() != null ? player.getName() : "Desconhecido";
                        int elo = entry.getValue().getElo();
                        sender.sendMessage(String.format("%s#%d %s%s %s- %s%d ELO",
                                ChatColor.GRAY, rank, ChatColor.AQUA, name, ChatColor.GRAY, ChatColor.YELLOW, elo));
                        rank++;
                    }
                }
                sender.sendMessage(ChatColor.GOLD + "------------------------------------");
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}