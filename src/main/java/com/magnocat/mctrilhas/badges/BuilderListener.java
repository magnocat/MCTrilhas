package com.magnocat.mctrilhas.badges;

import com.magnocat.mctrilhas.MCTrilhasPlugin;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.EnumSet;
import java.util.Set;

public class BuilderListener implements Listener {

    private final MCTrilhasPlugin plugin;

    // Lista de blocos relacionados à agricultura que não devem contar como construção.
    private static final Set<Material> FARMING_BLOCKS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.MELON_STEM,
            Material.PUMPKIN_STEM,
            Material.COCOA
    );

    public BuilderListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // Mark the block as player-placed using the persistent storage manager.
        plugin.getBlockPersistenceManager().markBlockAsPlayerPlaced(event.getBlock());

        Material placedType = event.getBlock().getType();

        // Do not count farming-related blocks or saplings for the builder badge.
        if (FARMING_BLOCKS.contains(placedType) || Tag.SAPLINGS.isTagged(placedType)) {
            return;
        }

        // Any block placed in survival mode counts towards the builder badge.
        plugin.getPlayerDataManager().addProgress(player, BadgeType.BUILDER, 1);
    }
}