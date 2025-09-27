package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.utils.ItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Representa um kit de duelo com armadura e itens.
 */
public class DuelKit {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final Material icon;
    private final ItemStack[] armor = new ItemStack[4];
    private final ItemStack[] items = new ItemStack[36];

    public DuelKit(String id, String displayName, List<String> description, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getDescription() { return description; }
    public Material getIcon() { return icon; }

    /**
     * Aplica o kit ao inventário de um jogador, limpando-o primeiro.
     * @param player O jogador que receberá o kit.
     */
    public void apply(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.clear();

        // A ordem da armadura é: boots, leggings, chestplate, helmet
        inv.setBoots(armor[0] != null ? armor[0].clone() : null);
        inv.setLeggings(armor[1] != null ? armor[1].clone() : null);
        inv.setChestplate(armor[2] != null ? armor[2].clone() : null);
        inv.setHelmet(armor[3] != null ? armor[3].clone() : null);

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                inv.setItem(i, items[i].clone());
            }
        }
        player.updateInventory();
    }

    /**
     * Cria um objeto DuelKit a partir de uma seção de configuração.
     * @param id O ID do kit.
     * @param section A ConfigurationSection do kit.
     * @return Um novo objeto DuelKit, ou null se a configuração for inválida.
     */
    public static DuelKit fromConfig(String id, ConfigurationSection section) {
        if (section == null) return null;

        String displayName = ChatColor.translateAlternateColorCodes('&', section.getString("display-name", id));
        List<String> description = section.getStringList("description").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());

        Material iconMaterial = Material.getMaterial(section.getString("icon", "BARRIER"));
        if (iconMaterial == null) iconMaterial = Material.BARRIER;

        DuelKit kit = new DuelKit(id, displayName, description, iconMaterial);

        // Carrega a armadura
        ConfigurationSection armorSection = section.getConfigurationSection("armor");
        if (armorSection != null) {
            kit.armor[0] = ItemFactory.createFromConfig(armorSection.getConfigurationSection("boots"));   // Slot 36
            kit.armor[1] = ItemFactory.createFromConfig(armorSection.getConfigurationSection("leggings")); // Slot 37
            kit.armor[2] = ItemFactory.createFromConfig(armorSection.getConfigurationSection("chestplate"));// Slot 38
            kit.armor[3] = ItemFactory.createFromConfig(armorSection.getConfigurationSection("helmet"));   // Slot 39
        }

        // Carrega os itens do inventário
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String slotStr : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotStr);
                    if (slot >= 0 && slot < kit.items.length) {
                        kit.items[slot] = ItemFactory.createFromConfig(itemsSection.getConfigurationSection(slotStr));
                    }
                } catch (NumberFormatException e) {
                    // Ignora chaves de slot inválidas
                }
            }
        }

        return kit;
    }
}