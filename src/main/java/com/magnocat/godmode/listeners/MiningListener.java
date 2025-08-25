package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class MiningListener implements Listener {

    private final GodModePlugin plugin;
    private static final String MINER_BADGE_ID = "miner";

    // Using a Set is highly efficient for checking if a material is in the list.
    // This list includes stone, deepslate, and common ores to match the badge description.
    private static final Set<Material> MINING_MATERIALS = EnumSet.of(
            // Overworld Materials
            Material.STONE, Material.DEEPSLATE,
            Material.COAL_ORE, Material.IRON_ORE, Material.COPPER_ORE, Material.GOLD_ORE,
            Material.REDSTONE_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
            Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_IRON_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE,
            // Nether Materials
            Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS
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

        // Check if the block has the "player_placed" metadata.
        // This prevents players from placing and breaking blocks to farm progress.
        if (event.getBlock().hasMetadata("player_placed")) {
            return;
        }

        // Get the item the player used to break the block.
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // If the item has Silk Touch, do not count the progress.
        // This prevents players from mining ore blocks, placing them, and mining again.
        if (itemInHand.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        // Check if the broken block is one of the designated mining materials.
        if (MINING_MATERIALS.contains(event.getBlock().getType())) {
            // All logic for checking progress, milestones, and awarding is now in the manager.
            plugin.getBadgeManager().incrementProgress(player, MINER_BADGE_ID, 1);
        }
    }
}