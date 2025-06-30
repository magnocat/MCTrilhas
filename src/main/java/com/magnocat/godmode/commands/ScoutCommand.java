package com.magnocat.godmode.commands;

import com.magnocat.godmode.badges.Badge;
import com.magnocat.godmode.badges.BadgeManager;
import com.magnocat.godmode.data.PlayerData;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0 || args[0].equalsIgnoreCase("badges")) {
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
        return false;
    }
}
