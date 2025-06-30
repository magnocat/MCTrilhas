package com.magnocat.godmode.commands;

import com.magnocat.godmode.badges.Badge;
import com.magnocat.godmode.badges.BadgeManager;
import com.magnocat.godmode.data.PlayerData;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoutCommand implements CommandExecutor {
    private final BadgeManager badgeManager;
    private final PlayerData playerData;

    public ScoutCommand(BadgeManager badgeManager, PlayerData playerData) {
        this.badgeManager = badgeManager;
        this.playerData = playerData;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length > 0 && !args[0].equalsIgnoreCase("removebadge")) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("badges")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cEste comando requer um jogador!");
                return true;
            }
            Player player = (Player) sender;
            List<String> playerBadges = playerData.getPlayerBadges(player.getUniqueId());
            player.sendMessage("§6Suas Insígnias:");
            if (playerBadges.isEmpty()) {
                player.sendMessage("§7Você ainda não conquistou nenhuma insígnia.");
            } else {
                for (String badgeId : playerBadges) {
                    Badge badge = badgeManager.getBadges().get(badgeId);
                    player.sendMessage("§a- " + badge.getName() + ": " + badge.getDescription());
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("progress")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cEste comando requer um jogador!");
                return true;
            }
            Player player = (Player) sender;
            player.sendMessage("§6Seu Progresso:");
            for (Badge badge : badgeManager.getBadges().values()) {
                if (!playerData.getPlayerBadges(player.getUniqueId()).contains(badge.getId())) {
                    int progress = playerData.getPlayerProgress(player.getUniqueId()).getOrDefault(badge.getId(), 0);
                    player.sendMessage("§e- " + badge.getName() + ": " + progress + "/" + badge.getRequiredProgress());
                }
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("removebadge") && sender.hasPermission("godmode.scout.admin")) {
            if (args.length != 3) {
                sender.sendMessage("§cUso: /scout removebadge <jogador> <badgeId>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cJogador não encontrado!");
                return true;
            }
            String badgeId = args[2];
            if (!badgeManager.getBadges().containsKey(badgeId)) {
                sender.sendMessage("§cInsígnia inválida!");
                return true;
            }
            if (playerData.removePlayerBadge(target.getUniqueId(), badgeId)) {
                Badge badge = badgeManager.getBadges().get(badgeId);
                sender.sendMessage("§aInsígnia " + badgeId + " removida de " + target.getName() + "!");
                if (target.isOnline()) {
                    target.sendMessage("§cSua insígnia " + badge.getName() + " foi removida!");
                    if (badge.getRewardRegion() != null) {
                        WorldGuardPlugin wg = WorldGuardPlugin.inst();
                        RegionManager rm = wg.getRegionManager(target.getWorld());
                        ProtectedRegion region = rm.getRegion(badge.getRewardRegion());
                        if (region != null) {
                            region.getMembers().removePlayer(target.getUniqueId());
                            target.sendMessage("§cVocê perdeu acesso à área: " + badge.getRewardRegion());
                        }
                    }
                }
            } else {
                sender.sendMessage("§cO jogador não possui essa insígnia!");
            }
            return true;
        }

        sender.sendMessage("§cComando inválido ou permissão insuficiente!");
        return true;
    }
}
