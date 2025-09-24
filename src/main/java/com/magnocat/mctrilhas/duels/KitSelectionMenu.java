package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.utils.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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

        FileConfiguration kitConfig = plugin.getDuelManager().getKitConfig();

        for (DuelKit kit : kits.values()) {
            ConfigurationSection kitSection = kitConfig.getConfigurationSection("kits." + kit.getId());
            if (kitSection == null) continue;

            Material iconMaterial = Material.getMaterial(kitSection.getString("icon", "BARRIER"));
            ItemStack item = new ItemStack(iconMaterial != null ? iconMaterial : Material.BARRIER);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(kit.getDisplayName());

                List<String> lore = new ArrayList<>();
                kitSection.getStringList("description").forEach(line -> lore.add(ChatColor.translateAlternateColorCodes('&', line)));
                lore.add(" ");
                lore.add(ChatColor.YELLOW + "Clique para selecionar este kit.");
                meta.setLore(lore);

                // Armazena o ID do kit no item de forma invisível para o jogador.
                NamespacedKey key = new NamespacedKey(plugin, "duel_kit_id");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, kit.getId());

                item.setItemMeta(meta);
            }
            gui.addItem(item);
        }

        challenger.openInventory(gui);
    }
}