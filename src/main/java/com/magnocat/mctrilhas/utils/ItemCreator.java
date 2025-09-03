package com.magnocat.mctrilhas.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation") // Suppress warnings for deprecated methods like setDisplayName, setLore, and ChatColor
public class ItemCreator {

    public static ItemStack createItemFromConfig(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String materialName = section.getString("material");
        if (materialName == null) {
            return null;
        }

        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) {
            return null;
        }

        int amount = section.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            if (section.contains("name")) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("name")));
            }
            if (section.contains("lore")) {
                List<String> lore = section.getStringList("lore").stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .collect(Collectors.toList());
                meta.setLore(lore);
            }
            if (section.contains("enchantments")) {
                section.getStringList("enchantments").forEach(enchStr -> {
                    String[] parts = enchStr.split(":");
                    Enchantment ench = Enchantment.getByName(parts[0].toUpperCase());
                    if (ench != null) {
                        meta.addEnchant(ench, Integer.parseInt(parts[1]), true);
                    }
                });
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}