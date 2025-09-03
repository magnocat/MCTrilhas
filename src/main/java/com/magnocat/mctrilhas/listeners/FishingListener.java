package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;
import org.bukkit.GameMode;
import org.bukkit.Tag;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public class FishingListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public FishingListener(MCTrilhasPlugin plugin) {
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
            Item caughtEntity = (Item) event.getCaught();

            // Check if the caught item is specifically a fish, matching the badge's intent.
            if (Tag.ITEMS_FISHES.isTagged(caughtEntity.getItemStack().getType())) {
                plugin.getPlayerDataManager().addProgress(player, BadgeType.FISHING, 1);
            }
        }
    }
}