package com.magnocat.mctrilhas.commands.subcommands;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.utils.ProgressBarUtil;
import com.magnocat.mctrilhas.badges.Badge;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor
public class ProgressSubCommand implements SubCommand {

    private final MCTrilhasPlugin plugin;

    public ProgressSubCommand(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "progress"; }

    @Override
    public String getDescription() { return "Mostra o progresso para insígnias não conquistadas."; }

    @Override
    public String getSyntax() { return "/scout progress [jogador]"; }

    @Override
    public String getPermission() { return "mctrilhas.scout.use"; }

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
            if (!sender.hasPermission("mctrilhas.progress.other")) {
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
            // Carrega os dados do jogador se não estiverem no cache.
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
            if (playerData == null) {
                plugin.getPlayerDataManager().loadPlayerData(target.getUniqueId());
                playerData = plugin.getPlayerDataManager().getPlayerData(target.getUniqueId());
                if (playerData == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.RED + "Não foi possível carregar os dados de progresso para " + targetName + "."));
                    return;
                }
            }

            final PlayerData finalPlayerData = playerData;
            List<Badge> unearnedBadges = plugin.getBadgeManager().getAllBadges().stream()
                    .filter(badge -> !finalPlayerData.hasBadge(badge.getId()))
                    .collect(Collectors.toList());

            List<String> progressMessages = new ArrayList<>();
            for (Badge badge : unearnedBadges) {
                double currentProgress = finalPlayerData.getProgress(badge.getType());
                double requiredProgress = badge.getRequirement();

                if (requiredProgress <= 0) continue; // Evita divisão por zero e barras de progresso sem sentido.

                String progressBar = ProgressBarUtil.buildProgressBar((int) currentProgress, (int) requiredProgress);
                String message = ChatColor.YELLOW + badge.getName() + ": " + ChatColor.AQUA + (long) currentProgress + "/" + (long) requiredProgress + " " + progressBar;
                progressMessages.add(message);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (unearnedBadges.isEmpty() && !plugin.getBadgeManager().getAllBadges().isEmpty()) {
                    sender.sendMessage(ChatColor.GREEN + "Parabéns! " + targetName + " já conquistou todas as insígnias disponíveis!");
                } else {
                    sender.sendMessage(ChatColor.GOLD + "--- Progresso de " + targetName + " ---");
                    progressMessages.forEach(sender::sendMessage);
                }
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("mctrilhas.progress.other")) {
            return null; // Usa o completador padrão do Bukkit para nomes de jogadores
        }
        return Collections.emptyList();
    }
}