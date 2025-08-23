package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class LumberjackListener implements Listener {

    private final GodModePlugin plugin;
    private static final String LUMBERJACK_BADGE_ID = "lumberjack";

    public LumberjackListener(GodModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // Using Bukkit's built-in Tag.LOGS is the most robust and future-proof way to check for any log type.
        if (Tag.LOGS.isTagged(event.getBlock().getType())) {
            plugin.getBadgeManager().incrementProgress(player, LUMBERJACK_BADGE_ID, 1);
        }
    }
}