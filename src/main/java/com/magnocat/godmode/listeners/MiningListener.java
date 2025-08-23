package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.EnumSet;
import java.util.Set;

public class MiningListener implements Listener {

    private final GodModePlugin plugin;
    private static final String MINER_BADGE_ID = "miner";

    // Using a Set is highly efficient for checking if a material is in the list.
    // This list includes stone, deepslate, and common ores to match the badge description.
    private static final Set<Material> MINING_MATERIALS = EnumSet.of(
            Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
            Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE, Material.GOLD_ORE,
            Material.REDSTONE_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
            Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE
    );

    public MiningListener(GodModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // Check if the broken block is one of the designated mining materials.
        if (MINING_MATERIALS.contains(event.getBlock().getType())) {
            // All logic for checking progress, milestones, and awarding is now in the manager.
            plugin.getBadgeManager().incrementProgress(player, MINER_BADGE_ID, 1);
        }
    }
}