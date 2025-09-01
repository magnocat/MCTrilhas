package com.magnocat.godmode.listeners;

import com.magnocat.godmode.menus.BadgeMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener para interações com menus de GUI.
 */
@SuppressWarnings("deprecation")
public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Verifica se o inventário clicado é um dos nossos menus de insígnias.
        if (!event.getView().getTitle().startsWith(BadgeMenu.MENU_TITLE_PREFIX)) {
            return;
        }

        // Impede que os jogadores peguem itens da GUI.
        event.setCancelled(true);

        // Pega o item que foi clicado.
        ItemStack clickedItem = event.getCurrentItem();

        // Verifica se o item clicado é nulo ou não é uma BARRIER (o ícone do nosso botão).
        if (clickedItem == null || clickedItem.getType() != Material.BARRIER) {
            return;
        }

        // Para ter certeza, verifica se o nome do item é "Fechar".
        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().equals(ChatColor.RED + "" + ChatColor.BOLD + "Fechar")) {
            // Se for, fecha o inventário do jogador.
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();
            }
        }
    }
}
