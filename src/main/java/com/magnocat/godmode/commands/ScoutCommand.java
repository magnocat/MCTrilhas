package com.magnocat.godmode.commands;

import com.magnocat.godmode.badges.Badge;
import com.magnocat.godmode.badges.BadgeManager;
import com.magnocat.godmode.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ScoutCommand implements CommandExecutor {
    private final BadgeManager badgeManager;
    private final PlayerData playerData;
    private static final String PREFIX = "§6[Escoteiro] ";

    public ScoutCommand(BadgeManager badgeManager, PlayerData playerData) {
        this.badgeManager = badgeManager;
        this.playerData = playerData;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Por padrão, mostra as insígnias do jogador ou uma mensagem de ajuda.
            return handleBadges(sender);
        }

        switch (args[0].toLowerCase()) {
            case "badges":
                return handleBadges(sender);
            case "progress":
                return handleProgress(sender);
            case "removebadge":
                return handleRemoveBadge(sender, args);
            default:
                sender.sendMessage(PREFIX + "§cComando desconhecido. Use /scout <badges|progress|removebadge>.");
                return true;
        }
    }

    private boolean handleBadges(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + "§cEste comando só pode ser usado por jogadores.");
            return true;
        }
        Player player = (Player) sender;
        List<String> playerBadges = playerData.getPlayerBadges(player.getUniqueId());
        player.sendMessage(PREFIX + "§eSuas Insígnias:");
        if (playerBadges.isEmpty()) {
            player.sendMessage("§7Você ainda não conquistou nenhuma insígnia. Continue explorando!");
        } else {
            for (String badgeId : playerBadges) {
                Badge badge = badgeManager.getBadges().get(badgeId);
                if (badge != null) {
                    player.sendMessage("§a- " + badge.name() + ": §f" + badge.description());
                }
            }
        }
        return true;
    }

    private boolean handleProgress(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PREFIX + "§cEste comando só pode ser usado por jogadores.");
            return true;
        }
        Player player = (Player) sender;
        player.sendMessage(PREFIX + "§eSeu Progresso para as próximas insígnias:");
        boolean hasPendingBadges = false;
        for (Badge badge : badgeManager.getBadges().values()) {
            if (!playerData.getPlayerBadges(player.getUniqueId()).contains(badge.id())) {
                int progress = playerData.getPlayerProgress(player.getUniqueId()).getOrDefault(badge.id(), 0);
                player.sendMessage("§e- " + badge.name() + ": §f" + progress + "/" + badge.requiredProgress());
                hasPendingBadges = true;
            }
        }
        if (!hasPendingBadges) {
            player.sendMessage("§aVocê já conquistou todas as insígnias disponíveis! Parabéns!");
        }
        return true;
    }

    private boolean handleRemoveBadge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("godmode.scout.admin")) {
            sender.sendMessage(PREFIX + "§cVocê não tem permissão para usar este comando.");
            return true;
        }
        if (args.length != 3) {
            sender.sendMessage(PREFIX + "§cUso correto: /scout removebadge <jogador> <id_da_insignia>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "§cJogador '" + args[1] + "' não encontrado ou está offline.");
            return true;
        }

        String badgeId = args[2].toLowerCase();
        Badge badge = badgeManager.getBadges().get(badgeId);
        if (badge == null) {
            sender.sendMessage(PREFIX + "§cInsígnia com ID '" + badgeId + "' não existe.");
            return true;
        }

        if (playerData.removePlayerBadge(target.getUniqueId(), badgeId)) {
            sender.sendMessage(PREFIX + "§aInsígnia '" + badge.name() + "' removida de " + target.getName() + " com sucesso!");
            if (target.isOnline()) {
                target.sendMessage(PREFIX + "§cSua insígnia '" + badge.name() + "' foi removida por um administrador.");
            }
        } else {
            sender.sendMessage(PREFIX + "§cO jogador " + target.getName() + " não possui a insígnia '" + badge.name() + "'.");
        }
        return true;
    }
}
