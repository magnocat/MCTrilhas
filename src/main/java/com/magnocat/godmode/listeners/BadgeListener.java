package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.badges.Badge;
import com.magnocat.godmode.badges.BadgeManager;
import com.magnocat.godmode.data.PlayerData;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;

import java.util.Set;

public class BadgeListener implements Listener {
    private final GodModePlugin plugin;
    private final BadgeManager badgeManager;
    private final PlayerData playerData;
    private final Economy economy;

    // Usar Sets para checagens de materiais é mais limpo e performático se a lista crescer.
    private static final Set<Material> BUILDER_MATERIALS = Set.of(
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.BAMBOO_PLANKS,
            Material.STONE, Material.BRICKS
    );

    public BadgeListener(GodModePlugin plugin, BadgeManager badgeManager, PlayerData playerData, Economy economy) {
        this.plugin = plugin;
        this.badgeManager = badgeManager;
        this.playerData = playerData;
        this.economy = economy;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material type = event.getBlock().getType();
        String badgeId = null;

        if (type.name().endsWith("_LOG")) {
            badgeId = "lumberjack";
        } else if (type == Material.STONE || type.name().contains("_ORE")) {
            badgeId = "miner";
        }

        if (badgeId != null && !playerData.getPlayerBadges(player.getUniqueId()).contains(badgeId)) {
            playerData.updatePlayerProgress(player.getUniqueId(), badgeId, 1);
            checkProgress(player, badgeId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        if (!event.getResult().getType().isEdible()) {
            return;
        }
        // Otimização: Busca apenas entidades próximas em vez de iterar todos os jogadores do mundo.
        event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation(), 5, 5, 5).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .findFirst() // Pega o primeiro jogador encontrado no raio
                .ifPresent(player -> {
                    String badgeId = "cook";
                    if (!playerData.getPlayerBadges(player.getUniqueId()).contains(badgeId)) {
                        playerData.updatePlayerProgress(player.getUniqueId(), badgeId, 1);
                        checkProgress(player, badgeId);
                    }
                });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String badgeId = "builder";

        if (!playerData.getPlayerBadges(player.getUniqueId()).contains(badgeId)) {
            if (BUILDER_MATERIALS.contains(event.getBlock().getType())) {
                playerData.updatePlayerProgress(player.getUniqueId(), badgeId, 1);
                checkProgress(player, badgeId);
            }
        }
    }

    private void checkProgress(Player player, String badgeId) {
        Badge badge = badgeManager.getBadges().get(badgeId);
        if (badge == null) return;

        int progress = playerData.getPlayerProgress(player.getUniqueId()).getOrDefault(badgeId, 0);
        int required = badge.getRequiredProgress();

        if (progress >= required) {
            grantBadge(player, badgeId);
        } else {
            // Otimização de UX: Para evitar spam, notifica o jogador apenas em marcos de progresso.
            // Ex: a cada 25 itens/blocos.
            if (progress > 0 && progress % 25 == 0) {
                player.sendMessage("§eProgresso para " + badge.getName() + ": " + progress + "/" + required);
            }
        }
    }

    private void grantBadge(Player player, String badgeId) {
        playerData.addPlayerBadge(player.getUniqueId(), badgeId);
        Badge badge = badgeManager.getBadges().get(badgeId);
        if (badge == null) return;

        player.sendMessage("§aVocê conquistou a " + badge.getName() + "!");

        if (badge.getRewardTotems() > 0 && economy != null) {
            economy.depositPlayer(player, badge.getRewardTotems());
            player.sendMessage("§aVocê recebeu " + badge.getRewardTotems() + " Totens!");
        }

        // Correção: Usa o console para dar o item, garantindo que o NBT (encantamentos, etc.) seja aplicado corretamente.
        if (badge.getRewardItem() != null && !badge.getRewardItem().isEmpty()) {
            String giveCommand = String.format("give %s %s %d", player.getName(), badge.getRewardItem(), badge.getRewardAmount());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCommand);
        }

        // Correção: Usa a API moderna do WorldGuard.
        if (badge.getRewardRegion() != null) {
            RegionManager rm = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            if (rm != null) {
                ProtectedRegion region = rm.getRegion(badge.getRewardRegion());
                if (region != null) {
                    region.getMembers().addPlayer(player.getUniqueId());
                    player.sendMessage("§aVocê ganhou acesso à área: " + badge.getRewardRegion());
                } else {
                    plugin.getLogger().warning("A região '" + badge.getRewardRegion() + "' não foi encontrada no mundo '" + player.getWorld().getName() + "'.");
                }
            }
        }
    }
}
