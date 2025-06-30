package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.badges.Badge;
import com.magnocat.godmode.badges.BadgeManager;
import com.magnocat.godmode.data.PlayerData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        if (event.getBlock().getType().name().contains("_LOG")) {
            badgeId = "lumberjack";
        } else if (event.getBlock().getType() == Material.STONE || event.getBlock().getType().name().contains("_ORE")) {
            badgeId = "miner";
        }
        if (badgeId != null && !playerData.getPlayerBadges(player.getUniqueId()).contains(badgeId)) {
            // Simples por agora, vamos adicionar contadores depois
            grantBadge(player, badgeId);
        }
    }

    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        // Detectar cozimento de alimentos (simples por agora)
        if (event.getResult().getType().isEdible()) {
            // Encontrar o jogador mais próximo (simplificado)
            for (Player player : event.getBlock().getWorld().getPlayers()) {
                if (player.getLocation().distance(event.getBlock().getLocation()) < 5) {
                    String badgeId = "cook";
                    if (!playerData.getPlayerBadges(player.getUniqueId()).contains(badgeId)) {
                        grantBadge(player, badgeId);
                    }
                    break;
                }
            }
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
                // Adicionar encantamentos (simplificado, usar um parser adequado depois)
                item.addEnchantment(org.bukkit.enchantments.Enchantment.EFFICIENCY, 2); // Exemplo
            }
            item.setAmount(badge.getRewardAmount());
            player.getInventory().addItem(item);
        }
        if (badge.getRewardRegion() != null) {
            // Configurar permissões do WorldGuard depois
            player.sendMessage("§aVocê ganhou acesso à área: " + badge.getRewardRegion());
        }
    }
}
