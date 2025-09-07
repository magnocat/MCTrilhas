package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import com.magnocat.mctrilhas.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ProgressSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ProgressSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "progress"; }

    @Override
    public String getDescription() { return "Mostra seu progresso para as próximas insígnias e ranque."; }

    @Override
    public String getSyntax() { return "/scout progress [jogador]"; }

    @Override
    public String getPermission() { return "mctrilhas.scout.use"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player target = getTargetPlayer(sender, args);
        if (target == null) return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
        if (playerData == null) {
            sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados de " + target.getName() + ".");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "--- Progresso de " + target.getName() + " ---");

        displayBadgeProgress(sender, playerData);
        displayRankProgress(sender, target, playerData);

        sender.sendMessage(ChatColor.GOLD + "---------------------------------");
    }

    private void displayBadgeProgress(CommandSender sender, PlayerData playerData) {
        sender.sendMessage(ChatColor.DARK_AQUA + "» Progresso das Insígnias:");
        boolean hasUnearnedBadges = false;

        ConfigurationSection badgesSection = plugin.getBadgeConfigManager().getBadgeConfig().getConfigurationSection("badges");
        if (badgesSection == null) return;

        Set<String> badgeKeys = badgesSection.getKeys(false);

        for (String key : badgeKeys) {
            if (key.equalsIgnoreCase("use-gui")) continue;

            if (!playerData.hasBadge(key)) {
                hasUnearnedBadges = true;
                String name = badgesSection.getString(key + ".name", key);
                long required = badgesSection.getLong(key + ".required-progress", 0);

                try {
                    BadgeType type = BadgeType.valueOf(key.toUpperCase());
                    long current = (long) playerData.getProgress(type);
                    MessageUtils.displayRequirement(sender, name, current, required);
                } catch (IllegalArgumentException ignored) {
                    // Ignora insígnias sem um tipo de progresso direto (ex: futuras quests)
                }
            }
        }

        if (!hasUnearnedBadges) {
            sender.sendMessage(ChatColor.GREEN + "   Todas as insígnias foram conquistadas!");
        }
    }

    private void displayRankProgress(CommandSender sender, Player target, PlayerData playerData) {
        sender.sendMessage(""); // Spacer
        sender.sendMessage(ChatColor.DARK_AQUA + "» Progresso de Ranque:");

        MessageUtils.displayRankProgress(sender, target, playerData, plugin);
    }

    private Player getTargetPlayer(CommandSender sender, String[] args) {
        if (args.length > 0) {
            if (!sender.hasPermission("mctrilhas.progress.other")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para ver o progresso de outros jogadores.");
                return null;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "O jogador '" + args[0] + "' não está online.");
                return null;
            }
            return target;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Especifique um jogador para ver o progresso.");
            return null;
        }
        return (Player) sender;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("mctrilhas.progress.other")) {
            return null; // Deixa o Bukkit autocompletar nomes de jogadores
        }
        return Collections.emptyList();
    }
}