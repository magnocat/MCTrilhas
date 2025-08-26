package com.magnocat.godmode.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A utility class for creating ItemStacks from configuration files.
 */
public final class ItemFactory {

    // Constants for configuration keys to avoid "magic strings"
    private static final String KEY_NAME = "name";
    private static final String KEY_LORE = "lore";
    private static final String KEY_MATERIAL = "material";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_ENCHANTS = "enchantments";

    /**
     * Creates an ItemStack from a configuration section. This is a utility class and cannot be instantiated.
     */
    private ItemFactory() {}

    /**
     * Creates an ItemStack from a configuration section.
     *
     * @param itemSection The ConfigurationSection containing the item data.
     * @param badgeId The ID of the badge for logging purposes.
     * @param logger The logger instance to report warnings.
     * @return The created ItemStack, or null if the material is invalid.
     */
    public static ItemStack createRewardItem(ConfigurationSection itemSection, String badgeId, Logger logger) {
        String materialName = itemSection.getString(KEY_MATERIAL);
        if (materialName == null) {
            logger.warning("Reward item for badge '" + badgeId + "' is missing a 'material' key.");
            return null;
        }

        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) {
            logger.warning("Invalid material '" + materialName + "' for badge '" + badgeId + "'.");
            return null;
        }

        int amount = itemSection.getInt(KEY_AMOUNT, 1);
        ItemStack rewardItem = new ItemStack(material, amount);
        ItemMeta meta = rewardItem.getItemMeta();

        if (meta != null) {
            String name = itemSection.getString(KEY_NAME);
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }

            List<String> lore = itemSection.getStringList(KEY_LORE);
            if (!lore.isEmpty()) {
                List<String> coloredLore = lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
                meta.setLore(coloredLore);
            }

            List<String> enchantStrings = itemSection.getStringList(KEY_ENCHANTS);
            for (String enchString : enchantStrings) {
                String[] parts = enchString.split(":");
                if (parts.length != 2) continue;
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
                if (enchantment == null) continue;
                try {
                    int level = Integer.parseInt(parts[1]);
                    meta.addEnchant(enchantment, level, true);
                } catch (NumberFormatException ignored) {}
            }
            rewardItem.setItemMeta(meta);
        }
        return rewardItem;
    }
}