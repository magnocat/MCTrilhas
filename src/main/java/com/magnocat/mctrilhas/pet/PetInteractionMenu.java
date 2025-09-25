package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Constrói a interface gráfica (GUI) para interagir com um pet.
 */
public class PetInteractionMenu {

    public static final String INVENTORY_TITLE_PREFIX = "Interagir com ";
    private final MCTrilhasPlugin plugin;
    private final Pet pet;

    public PetInteractionMenu(MCTrilhasPlugin plugin, Pet pet) {
        this.plugin = plugin;
        this.pet = pet;
    }

    public void open(Player player) {
        String petName = ChatColor.stripColor(pet.petData.getName());
        Inventory gui = Bukkit.createInventory(null, 27, INVENTORY_TITLE_PREFIX + petName);

        // Item de Informações
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.AQUA + "Informações de " + petName);
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "Nível: " + ChatColor.WHITE + pet.petData.getLevel());
        infoLore.add(ChatColor.GRAY + "Felicidade: " + ChatColor.WHITE + String.format("%.0f%%", pet.petData.getHappiness()));
        if (pet.petData.getLevel() < PetData.MAX_LEVEL) {
            infoLore.add(ChatColor.GRAY + "XP: " + ChatColor.WHITE + (int) pet.petData.getExperience() + " / " + pet.petData.getExperienceToNextLevel());
        } else {
            infoLore.add(ChatColor.GRAY + "XP: " + ChatColor.GREEN + "Nível Máximo!");
        }
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);

        // Botões de Ação
        gui.setItem(4, infoItem);
        gui.setItem(11, createActionButton(Material.BONE, "Alimentar", "Aumenta a felicidade do seu pet.", "feed"));
        gui.setItem(13, createActionButton(Material.NAME_TAG, "Mudar Nome", "Custa 5.000 Totens para renomear.", "rename"));
        gui.setItem(15, createActionButton(Material.BARRIER, "Guardar Pet", "Guarda seu pet com segurança.", "release"));

        player.openInventory(gui);
    }

    private ItemStack createActionButton(Material material, String name, String description, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add(" ");
            lore.add(ChatColor.YELLOW + "Clique para usar.");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            NamespacedKey key = new NamespacedKey(plugin, "pet_action");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }
}