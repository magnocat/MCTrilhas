package com.magnocat.mctrilhas.sorting;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ouve eventos de inventário para adicionar a funcionalidade de organização de baús.
 */
public class ChestSortListener implements Listener {

    private final MCTrilhasPlugin plugin;
    private final NamespacedKey sortButtonKey;

    public ChestSortListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.sortButtonKey = new NamespacedKey(plugin, "chest_sort_button");
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.CHEST || !(event.getPlayer() instanceof Player)) {
            return;
        }

        // Verifica se o inventário pertence a um baú (simples ou duplo)
        if (!(event.getInventory().getHolder() instanceof Chest) && !(event.getInventory().getHolder() instanceof DoubleChest)) {
            return;
        }

        // Adiciona o botão de organizar no canto superior direito da interface do jogador
        // Slot 8 é o último slot da primeira linha do inventário do jogador na visão do baú.
        event.getView().getTopInventory().setItem(8, createSortButton());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null || !(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !isSortButton(clickedItem)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        Inventory chestInventory = event.getView().getTopInventory();

        // Pega todos os itens, exceto o próprio botão de organizar
        List<ItemStack> items = Arrays.stream(chestInventory.getContents())
                .filter(item -> item != null && !isSortButton(item))
                .collect(Collectors.toList());

        // Ordena por nome do material e depois por nome customizado (se houver)
        items.sort(Comparator.comparing((ItemStack item) -> item.getType().toString())
                .thenComparing(item -> item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : ""));

        // Limpa o baú completamente, exceto o botão de organizar
        chestInventory.clear();
        chestInventory.setItem(8, createSortButton());

        // Re-adiciona os itens de forma agrupada e ordenada
        for (ItemStack sortedItem : items) {
            chestInventory.addItem(sortedItem);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.5f);
        player.sendMessage(ChatColor.GREEN + "Baú organizado com sucesso!");
    }

    private ItemStack createSortButton() {
        ItemStack button = new ItemStack(Material.COMPASS);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Organizar Baú");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Clique para organizar os itens");
            lore.add(ChatColor.GRAY + "deste baú automaticamente.");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(sortButtonKey, PersistentDataType.BYTE, (byte) 1);
            button.setItemMeta(meta);
        }
        return button;
    }

    private boolean isSortButton(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(sortButtonKey, PersistentDataType.BYTE);
    }
}