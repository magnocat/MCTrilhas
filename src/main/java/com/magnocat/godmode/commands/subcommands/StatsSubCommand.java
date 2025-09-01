package com.magnocat.godmode.commands.subcommands;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.badges.BadgeType;
import com.magnocat.godmode.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.text.NumberFormat;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class StatsSubCommand extends SubCommand {

    private final NumberFormat numberFormat;

    public StatsSubCommand(GodModePlugin plugin) {
        super(plugin);
        this.numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        this.numberFormat.setGroupingUsed(true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /scout stats <jogador>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Jogador não encontrado: " + args[1]);
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Buscando estatísticas de " + target.getName() + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (playerData == null) {
                plugin.getPlayerDataManager().loadPlayerData(target.getUniqueId());
                playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            }

            if (playerData == null) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados para " + target.getName() + "."));
                return;
            }

            final PlayerData finalPlayerData = playerData;
            final int totalBadges = plugin.getBadgeManager().getAllBadges().size();

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "--- Estatísticas de " + ChatColor.WHITE + target.getName() + ChatColor.GOLD + " ---");
                sender.sendMessage(ChatColor.YELLOW + "Insígnias: " + ChatColor.AQUA + finalPlayerData.getEarnedBadges().size() + " / " + totalBadges);
                sender.sendMessage(" ");
                sender.sendMessage(ChatColor.GOLD + "--- Progresso Total (Vida Inteira) ---");

                for (BadgeType type : BadgeType.values()) {
                    double progress = finalPlayerData.getProgress(type);
                    sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + type.getName() + ": " + ChatColor.GREEN + numberFormat.format(progress));
                }
                sender.sendMessage(ChatColor.GOLD + "------------------------------------");
            });
        });
    }

    @Override
    public boolean isAdminCommand() {
        return true;
    }

    @Override
    public String getPermission() {
        return "godmode.scout.stats";
    }

    @Override
    public String getSyntax() {
        return "/scout stats <jogador>";
    }

    @Override
    public String getDescription() {
        return "Vê as estatísticas completas de um jogador.";
    }

    @Override
    public String getName() {
        return "stats";
    }
}