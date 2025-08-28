package com.magnocat.godmode.commands.subcommands;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@SuppressWarnings("deprecation") // Suppress warnings for deprecated ChatColor
public class BadgesSubCommand extends SubCommand {

    public BadgesSubCommand(GodModePlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "badges";
    }

    @Override
    public String getDescription() {
        return "Exibe as insígnias conquistadas.";
    }

    @Override
    public String getSyntax() {
        return "/scout badges";
    }

    @Override
    public String getPermission() {
        return "godmode.scout.use";
    }

    @Override
    public boolean isAdminCommand() { return false; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }

        Player player = (Player) sender;
        List<String> earnedBadges = plugin.getPlayerDataManager().getEarnedBadges(player.getUniqueId());

        if (earnedBadges.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Você ainda não conquistou nenhuma insígnia. Continue se esforçando!");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "--- Suas Insígnias Conquistadas ---");
        for (String badgeId : earnedBadges) {
            String badgeName = plugin.getBadgeConfigManager().getBadgeConfig().getString(badgeId + ".name", badgeId);
            String description = plugin.getBadgeConfigManager().getBadgeConfig().getString(badgeId + ".description", "Sem descrição.");
            player.sendMessage(ChatColor.AQUA + "- " + badgeName + ": " + ChatColor.GRAY + description);
        }
    }
}