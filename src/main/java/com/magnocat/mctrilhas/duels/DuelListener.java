package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener para eventos relacionados ao sistema de Duelos, como a seleção de kits.
 */
public class DuelListener implements Listener {

    private final MCTrilhasPlugin plugin;
    private final DuelManager duelManager;

    public DuelListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.duelManager = plugin.getDuelManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(KitSelectionMenu.INVENTORY_TITLE)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        Player challenger = (Player) event.getWhoClicked();
        NamespacedKey key = new NamespacedKey(plugin, "duel_kit_id");

        if (clickedItem.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            String kitId = clickedItem.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

            // Ação de seleção de kit
            challenger.closeInventory();
            duelManager.finalizeChallenge(challenger, kitId);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(KitSelectionMenu.INVENTORY_TITLE)) {
            return;
        }

        Player challenger = (Player) event.getPlayer();
        // Se o jogador fechou o menu e ainda estava em processo de seleção, cancela.
        if (duelManager.isPlayerInKitSelection(challenger.getUniqueId())) {
            duelManager.cancelChallengeCreation(challenger);
        }
    }
}