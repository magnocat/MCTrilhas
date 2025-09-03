package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

public class CraftingListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public CraftingListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        ItemStack resultItem = event.getRecipe().getResult();
        if (resultItem == null || resultItem.getAmount() == 0) {
            return;
        }

        // The event is fired for each crafting operation. We add the amount of items produced by the recipe.
        // The server handles firing this event multiple times for shift-clicks.
        plugin.getPlayerDataManager().addProgress(player, BadgeType.CRAFTING, resultItem.getAmount());
    }
}