package com.magnocat.godmode.commands.subcommands;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.utils.ProgressBarUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProgressSubCommand extends SubCommand {

    public ProgressSubCommand(GodModePlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() { return "progress"; }

    @Override
    public String getDescription() { return "Mostra o progresso para insígnias não conquistadas."; }

    @Override
    public String getSyntax() { return "/scout progress [jogador]"; }

    @Override
    public String getPermission() { return "godmode.scout.use"; }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 1) {
            sender.sendMessage(ChatColor.RED + "Uso: " + getSyntax());
            return;
        }

        OfflinePlayer target;
        if (args.length == 1) {
            if (!sender.hasPermission("godmode.scout.progress.other")) {
                sender.sendMessage(ChatColor.RED + "Você não tem permissão para ver o progresso de outros jogadores.");
                return;
            }
            target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + args[0] + "' nunca foi visto neste servidor.");
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "O console deve especificar um jogador. Uso: /scout progress <jogador>");
                return;
            }
            target = (Player) sender;
        }

        displayProgressFor(sender, target);
    }

    private void displayProgressFor(CommandSender sender, OfflinePlayer target) {
        String targetName = target.getName() != null ? target.getName() : "Desconhecido";
        sender.sendMessage(ChatColor.YELLOW + "Calculando progresso para " + targetName + "...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> earnedBadges = plugin.getPlayerDataManager().getEarnedBadges(target.getUniqueId());
            var badgeSection = plugin.getConfig().getConfigurationSection("badges");
            if (badgeSection == null) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Nenhuma insígnia configurada."));
                return;
            }
            Set<String> allBadgeIds = badgeSection.getKeys(false);

            List<String> progressMessages = new ArrayList<>();
            for (String badgeId : allBadgeIds) {
                if (earnedBadges.contains(badgeId)) continue;

                String badgeName = plugin.getConfig().getString("badges." + badgeId + ".name", badgeId);
                int currentProgress = plugin.getPlayerDataManager().getProgress(target.getUniqueId(), badgeId);
                int requiredProgress = plugin.getConfig().getInt("badges." + badgeId + ".required-progress", 1);

                String progressBar = ProgressBarUtil.buildProgressBar(currentProgress, requiredProgress);
                String message = ChatColor.YELLOW + badgeName + ": " + ChatColor.AQUA + currentProgress + "/" + requiredProgress + " " + progressBar;
                progressMessages.add(message);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (progressMessages.isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "Parabéns! " + targetName + " já conquistou todas as insígnias disponíveis!");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "--- Progresso de " + targetName + " ---");
                    progressMessages.forEach(sender::sendMessage);
                }
            });
        });
    }
}