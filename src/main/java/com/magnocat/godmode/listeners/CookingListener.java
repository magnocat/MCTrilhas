package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.badges.BadgeType;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

public class CookingListener implements Listener {

    private final GodModePlugin plugin;
    private static final int RESULT_SLOT = 2;

    public CookingListener(GodModePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        // FurnaceInventory is the parent for Furnace, Smoker, and BlastFurnace inventories.
        if (!(event.getInventory() instanceof FurnaceInventory)) {
            return;
        }

        // We only care about clicks on the result slot.
        if (event.getSlot() != RESULT_SLOT) {
            return;
        }

        ItemStack resultItem = event.getCurrentItem();
        if (resultItem != null && resultItem.getAmount() > 0) {
            // The amount of items taken is the amount in the result stack.
            // This works for both regular clicks and shift-clicks.
            plugin.getPlayerDataManager().addProgress(player, BadgeType.COOKING, resultItem.getAmount());
        }
    }
}