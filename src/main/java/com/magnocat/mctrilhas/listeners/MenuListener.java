package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeMenu;
import com.magnocat.mctrilhas.npc.DialogueMenu;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
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

        // Verifica se o inventário clicado é um dos nossos menus.
        if (!viewTitle.startsWith(BadgeMenu.MENU_TITLE_PREFIX) && !viewTitle.equals(DialogueMenu.MENU_TITLE)) {
            return;
        }

        // Impede que os jogadores peguem itens da GUI.
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();

        // Verifica se o item clicado é nulo ou um item de placeholder/decoração.
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE || clickedItem.getType() == Material.BOOK) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        // Lógica para o menu de insígnias (paginação)
        if (viewTitle.startsWith(BadgeMenu.MENU_TITLE_PREFIX)) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey actionKey = new NamespacedKey(plugin, "gui_action");
            NamespacedKey pageKey = new NamespacedKey(plugin, "gui_target_page");

            if (container.has(actionKey, PersistentDataType.STRING) && container.has(pageKey, PersistentDataType.INTEGER)) {
                String action = container.get(actionKey, PersistentDataType.STRING);

                if ("page_previous".equals(action) || "page_next".equals(action)) {
                    // Extrai o nome do jogador alvo do título do inventário.
                    int targetPage = container.get(pageKey, PersistentDataType.INTEGER);
                    String tempTitle = viewTitle.substring(BadgeMenu.MENU_TITLE_PREFIX.length());
                    String targetName = tempTitle.substring(0, tempTitle.lastIndexOf(" ("));

                    // Reabre o menu na página correta.
                    if (plugin.getBadgeMenu() != null) {
                        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
                        plugin.getBadgeMenu().open(player, target.getUniqueId(), target.getName(), targetPage);
                    }
                }
            }
        }
        // Lógica para o menu de diálogo
        else if (viewTitle.equals(DialogueMenu.MENU_TITLE)) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey actionKey = new NamespacedKey(plugin, "dialogue_action");

            if (container.has(actionKey, PersistentDataType.STRING)) {
                String action = container.get(actionKey, PersistentDataType.STRING);
                if (plugin.getDialogueManager() != null) {
                    handleDialogueAction(player, action);
                }
            }
        }
    }

    private void handleDialogueAction(Player player, String action) {
        if (action.equalsIgnoreCase("close")) {
            player.closeInventory();
        } else if (action.startsWith("dialogue:")) {
            if (plugin.getDialogueManager() != null) {
                String nextDialogueId = action.substring("dialogue:".length());
                plugin.getDialogueManager().startDialogue(player, nextDialogueId);
            }
        } else {
            // Placeholder para futuras ações (quests, comandos, etc.)
            player.sendMessage(ChatColor.GRAY + "[DEBUG] Ação '" + action + "' ainda não implementada.");
            player.closeInventory();
        }
    }
}
