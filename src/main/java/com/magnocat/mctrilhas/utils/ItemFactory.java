package com.magnocat.mctrilhas.utils;

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
 * Uma classe utilitária para criar ItemStacks a partir de arquivos de configuração.
 */
@SuppressWarnings("deprecation") // Suprime avisos para métodos obsoletos como setDisplayName, setLore e ChatColor
public final class ItemFactory {

    // Constantes para as chaves de configuração para evitar "strings mágicas"
    private static final String KEY_NAME = "name";
    private static final String KEY_LORE = "lore";
    private static final String KEY_MATERIAL = "material";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_ENCHANTS = "enchantments";

    /**
     * Esta é uma classe utilitária e não pode ser instanciada.
     */
    private ItemFactory() {}

    /**
     * Cria um ItemStack a partir de uma seção de configuração, com contexto para logs.
     *
     * @param itemSection A ConfigurationSection contendo os dados do item.
     * @param contextId O ID da insígnia ou outro contexto para fins de log.
     * @param logger A instância do logger para reportar avisos.
     * @return O ItemStack criado, ou null se o material for inválido.
     */
    public static ItemStack createFromConfig(ConfigurationSection itemSection, String contextId, Logger logger) {
        if (itemSection == null) {
            return null;
        }

        String materialName = itemSection.getString(KEY_MATERIAL);
        if (materialName == null) {
            logger.warning("A configuração do item para o contexto '" + contextId + "' não possui a chave 'material'.");
            return null;
        }

        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) {
            logger.warning("Material inválido '" + materialName + "' para o contexto '" + contextId + "'.");
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

    /**
     * Cria um ItemStack a partir de uma seção de configuração genérica.
     * Esta é uma sobrecarga de conveniência que usa um contexto padrão e o logger global.
     *
     * @param itemSection A ConfigurationSection contendo os dados do item.
     * @return O ItemStack criado, ou null se a configuração for inválida.
     */
    public static ItemStack createFromConfig(ConfigurationSection itemSection) {
        // Chama o criador principal com um contexto genérico e o logger global do Minecraft.
        return createFromConfig(itemSection, "generic-item", Logger.getLogger("Minecraft"));
    }

    /**
     * Cria um ItemStack simples com nome e lore, sem precisar de uma seção de configuração.
     *
     * @param material O material do item.
     * @param name O nome de exibição do item (será colorido).
     * @param lore A descrição (lore) do item (será colorida).
     * @return O ItemStack criado.
     */
    public static ItemStack createSimple(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            if (lore != null && !lore.isEmpty()) {
                List<String> coloredLore = lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).collect(Collectors.toList());
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}