package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FarmingListener implements Listener {

    private final MCTrilhasPlugin plugin;
    private final Set<Material> trackedCrops = new HashSet<>();

    public FarmingListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        loadTrackedCrops();
    }

    /**
     * Loads the list of trackable crop materials from the config.yml.
     */
    private void loadTrackedCrops() {
        List<String> cropNames = plugin.getConfig().getStringList("badges.FARMING.tracked-items");
        for (String name : cropNames) {
            try {
                trackedCrops.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Material de agricultura inválido no config.yml: " + name);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Only track for players in survival mode.
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // Ignore blocks placed by players to prevent farming.
        if (plugin.getBlockPersistenceManager().isPlayerPlaced(block)) {
            return;
        }

        // Check if the broken block is a tracked crop.
        if (!trackedCrops.contains(block.getType())) {
            return;
        }

        // Ensure the crop is fully grown before counting.
        // This is the most important check for farming.
        if (block.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) block.getBlockData();
            if (ageable.getAge() == ageable.getMaximumAge()) {
                // The crop is fully grown, so we count it.
                plugin.getPlayerDataManager().addProgress(player, BadgeType.FARMING, 1);
            }
        } else {
            // This handles non-ageable crops like MELON and PUMPKIN.
            // The check for trackedCrops already confirmed this is a block we want to count.
            plugin.getPlayerDataManager().addProgress(player, BadgeType.FARMING, 1);
        }
    }
}