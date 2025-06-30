package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.badges.Badge;
import com.magnocat.godmode.badges.BadgeManager;
import com.magnocat.godmode.data.PlayerData;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

public class BadgeListener implements Listener {
    private final GodModePlugin plugin;
    private final BadgeManager badgeManager;
    private final PlayerData playerData;
    private final Economy economy;

    public BadgeListener(GodModePlugin plugin, BadgeManager badgeManager, PlayerData playerData, Economy economy) {
        this.plugin = plugin;
        this.badgeManager = badgeManager;
        this.playerData = playerData;
        this.economy = economy;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String badgeId = null;
        int increment = 1;
        if (event.getBlock().getType().name().contains("_LOG")) {
            badgeId = "lumberjack";
        } else if (event.getBlock().getType() == Material.STONE || event.getBlock().getType().name().contains("_ORE")) {
            badgeId = "miner";
        }
        if (badgeId != null && !playerData.getPlayerBadges(player.getUniqueId()).contains(badgeId)) {
            playerData.updatePlayerProgress(player.getUniqueId(), badgeId, increment);
            checkProgress(player, badgeId);
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (event.getResult().getType().isEdible()) {
            for (Player player : event.getBlock().getWorld().getPlayers()) {
                if (player.getLocation().distance(event.getBlock().getLocation()) < 5) {
                    String badgeId = "cook";
                    if (!playerData.getPlayerBadges(player.getUniqueId()).contains(badgeId)) {
                        playerData.updatePlayerProgress(player.getUniqueId(), badgeId, 1);
                        checkProgress(player, badgeId);
                    }
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String badgeId = "builder";
        if (!playerData.getPlayerBadges(player.getUniqueId()).contains(badgeId)) {
            Material type = event.getBlock().getType();
            if (type.name().contains("_PLANKS") || type == Material.STONE || type == Material.BRICK) {
                playerData.updatePlayerProgress(player.getUniqueId(), badgeId, 1);
                checkProgress(player, badgeId);
            }
        }
    }

    private void checkProgress(Player player, String badgeId) {
        Badge badge = badgeManager.getBadges().get(badgeId);
        int progress = playerData.getPlayerProgress(player.getUniqueId()).getOrDefault(badgeId, 0);
        if (progress >= badge.getRequiredProgress()) {
            grantBadge(player, badgeId);
        } else {
            player.sendMessage("§eProgresso para " + badge.getName() + ": " + progress + "/" + badge.getRequiredProgress());
        }
    }

    private void grantBadge(Player player, String badgeId) {
        playerData.addPlayerBadge(player.getUniqueId(), badgeId);
        Badge badge = badgeManager.getBadges().get(badgeId);
        player.sendMessage("§aVocê conquistou a " + badge.getName() + "!");
        if (badge.getRewardTotems() > 0 && economy != null) {
            economy.depositPlayer(player, badge.getRewardTotems());
            player.sendMessage("§aVocê recebeu " + badge.getRewardTotems() + " Totens!");
        }
        if (badge.getRewardItem() != null) {
            ItemStack item = new ItemStack(Material.valueOf(badge.getRewardItem().split("\\{")[0].replace("minecraft:", "").toUpperCase()));
            if (badge.getRewardItem().contains("{")) {
                item.addEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, 2); // Simplificado
            }
            item.setAmount(badge.getRewardAmount());
            player.getInventory().addItem(item);
        }
        if (badge.getRewardRegion() != null) {
            WorldGuardPlugin wg = WorldGuardPlugin.inst();
            RegionManager rm = wg.getRegionManager(player.getWorld());
            ProtectedRegion region = rm.getRegion(badge.getRewardRegion());
            if (region != null) {
                region.getMembers().addPlayer(player.getUniqueId());
                player.sendMessage("§aVocê ganhou acesso à área: " + badge.getRewardRegion());
            }
        }
    }
}
