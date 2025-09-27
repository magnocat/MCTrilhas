package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Constrói e gerencia a interface gráfica (GUI) para seleção de kits de duelo.
 */
public class KitSelectionMenu {

    public static final String INVENTORY_TITLE = "Escolha o Kit de Duelo";
    private final MCTrilhasPlugin plugin;

    public KitSelectionMenu(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player challenger) {
        Map<String, DuelKit> kits = plugin.getDuelManager().getLoadedKits();
        int inventorySize = (int) (Math.ceil(kits.size() / 9.0) * 9);
        if (inventorySize == 0) inventorySize = 9;

        Inventory gui = Bukkit.createInventory(null, inventorySize, INVENTORY_TITLE);

        for (DuelKit kit : kits.values()) {
            ItemStack item = new ItemStack(kit.getIcon());
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(kit.getDisplayName());

                List<String> lore = new ArrayList<>();
                // Usa a descrição já processada do objeto DuelKit
                lore.addAll(kit.getDescription());
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Clique para selecionar este kit.");
                meta.setLore(lore);

                NamespacedKey key = new NamespacedKey(plugin, "duel_kit_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, kit.getId());

                item.setItemMeta(meta);
            }
            gui.addItem(item);
        }

        challenger.openInventory(gui);
    }
}