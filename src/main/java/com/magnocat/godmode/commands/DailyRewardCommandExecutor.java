package com.magnocat.godmode.commands;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.data.PlayerDataManager;
import com.magnocat.godmode.utils.ItemCreator;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeUnit;

public class DailyRewardCommandExecutor implements CommandExecutor {

    private final GodModePlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final Economy economy;

    public DailyRewardCommandExecutor(GodModePlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
        this.economy = plugin.getEconomy();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;
        ConfigurationSection dailyRewardSection = plugin.getConfig().getConfigurationSection("daily-reward");

        if (dailyRewardSection == null || !dailyRewardSection.getBoolean("enabled", false)) {
            player.sendMessage(ChatColor.RED + "O sistema de recompensa diária está desativado.");
            return true;
        }

        long lastClaim = playerDataManager.getLastDailyRewardTime(player.getUniqueId());
        long cooldown = TimeUnit.HOURS.toMillis(24);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastClaim >= cooldown) {
            // Dar a recompensa
            giveReward(player, dailyRewardSection);
            playerDataManager.setLastDailyRewardTime(player.getUniqueId(), currentTime);

            String successMessage = dailyRewardSection.getString("messages.success", "&aVocê coletou sua recompensa diária! Volte em 24 horas.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', successMessage));
        } else {
            // Informar o tempo restante
            long remainingTime = (lastClaim + cooldown) - currentTime;
            String formattedTime = formatTime(remainingTime);
            String cooldownMessage = dailyRewardSection.getString("messages.cooldown", "&cVocê já coletou sua recompensa. Tempo restante: &e{time}");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', cooldownMessage.replace("{time}", formattedTime)));
        }

        return true;
    }

    private void giveReward(Player player, ConfigurationSection section) {
        // Recompensa em Totens (economia)
        int totems = section.getInt("reward-totems", 0);
        if (economy != null && totems > 0) {
            economy.depositPlayer(player, totems);
        }

        // Recompensa em item
        ConfigurationSection itemSection = section.getConfigurationSection("reward-item-data");
        if (itemSection != null) {
            ItemStack rewardItem = ItemCreator.createItemFromConfig(itemSection);
            if (rewardItem != null) {
                player.getInventory().addItem(rewardItem);
            }
        }
    }

    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}