package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BuilderListener implements Listener {

    private final MCTrilhasPlugin plugin;

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

        // Any block placed in survival mode counts towards the builder badge.
        plugin.getPlayerDataManager().addProgress(player, BadgeType.BUILDER, 1);
    }
}