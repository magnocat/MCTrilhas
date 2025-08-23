package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.GameMode;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class FishingListener implements Listener {

    private final GodModePlugin plugin;
    private static final String FISHING_BADGE_ID = "fishing";

    public FishingListener(GodModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // We only care about the event when a fish is actually caught.
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item) {
            plugin.getBadgeManager().incrementProgress(player, FISHING_BADGE_ID, 1);
        }
    }
}