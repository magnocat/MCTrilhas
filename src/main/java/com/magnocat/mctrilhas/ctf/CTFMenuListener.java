package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Ouve os cliques dentro da GUI do CTF e executa as ações correspondentes.
 */
public class CTFMenuListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public CTFMenuListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(CTFMenu.MENU_TITLE)) {
            return; // Não é o menu do CTF, ignora.
        }

        event.setCancelled(true); // Impede que o jogador pegue os itens do menu.

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        // Verifica qual item foi clicado e executa a ação
        if (clickedItem.getType() == Material.GREEN_WOOL) {
            player.closeInventory();
            plugin.getCtfManager().addPlayerToQueue(player);
        } else if (clickedItem.getType() == Material.RED_WOOL) {
            player.closeInventory();
            plugin.getCtfManager().handlePlayerLeave(player);
        }
    }
}