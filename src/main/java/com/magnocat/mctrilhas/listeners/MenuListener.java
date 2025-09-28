package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.menus.BadgeMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener para interações com menus de GUI.
 */
@SuppressWarnings("deprecation")
public class MenuListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public MenuListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Garante que quem clicou foi um jogador.
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        String viewTitle = event.getView().getTitle();

        // Verifica se o inventário clicado é um dos nossos menus de insígnias.
        if (!viewTitle.startsWith(BadgeMenu.MENU_TITLE_PREFIX)) {
            return;
        }

        // Impede que os jogadores peguem itens da GUI.
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();

        // Verifica se o item clicado é nulo ou um item de preenchimento.
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey actionKey = new NamespacedKey(plugin, "gui_action");
        NamespacedKey pageKey = new NamespacedKey(plugin, "gui_target_page");

        // Verifica se o item clicado é um botão de navegação.
        if (container.has(actionKey, PersistentDataType.STRING) && container.has(pageKey, PersistentDataType.INTEGER)) {
            String action = container.get(actionKey, PersistentDataType.STRING);

            if ("page_previous".equals(action) || "page_next".equals(action)) {
                int targetPage = container.get(pageKey, PersistentDataType.INTEGER);

                // Extrai o nome do jogador alvo do título do inventário.
                String tempTitle = viewTitle.substring(BadgeMenu.MENU_TITLE_PREFIX.length());
                String targetName = tempTitle.substring(0, tempTitle.lastIndexOf(" ("));

                // Reabre o menu na página correta.
                OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
                plugin.getBadgeMenu().open(player, target.getUniqueId(), target.getName(), targetPage);
            }
        }
    }
}
