package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Constrói a interface gráfica (GUI) para a loja de pets.
 */
public class PetShopMenu {

    public static final String INVENTORY_TITLE = "Loja de Pets";
    private final MCTrilhasPlugin plugin;

    public PetShopMenu(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, INVENTORY_TITLE);

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        boolean hasPet = playerData != null && playerData.getPetData() != null && playerData.getPetData().isOwned();
        boolean hasRank = playerData != null && playerData.getRank().ordinal() >= Rank.ESCOTEIRO.ordinal();

        // Adiciona os itens dos pets disponíveis
        gui.setItem(11, createPetItem(Material.BONE, "Lobo", "Um companheiro leal e forte em combate.", hasPet, hasRank));
        gui.setItem(13, createPetItem(Material.COD, "Gato", "Um amigo ágil que pode alertá-lo sobre perigos.", hasPet, hasRank));
        gui.setItem(15, createPetItem(Material.CARROT, "Porco", "Um ajudante que pode coletar itens para você.", hasPet, hasRank));

        player.openInventory(gui);
    }

    private ItemStack createPetItem(Material icon, String petType, String description, boolean alreadyOwns, boolean hasRequiredRank) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + petType);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add(" ");

            if (!hasRequiredRank) {
                lore.add(ChatColor.RED + "Requer Ranque: " + Rank.ESCOTEIRO.getDisplayName());
            } else if (alreadyOwns) {
                lore.add(ChatColor.GREEN + "Você já possui um pet!");
                lore.add(ChatColor.GRAY + "Use /scout pet invocar para chamá-lo.");
            } else {
                lore.add(ChatColor.YELLOW + "Custo: " + ChatColor.GOLD + "50.000 Totens");
                lore.add(ChatColor.GREEN + "Clique para adquirir seu primeiro pet!");
            }

            meta.setLore(lore);

            // Adiciona o tipo de pet ao item para identificação no clique,
            // mas apenas se o jogador puder comprar.
            if (hasRequiredRank && !alreadyOwns) {
                NamespacedKey key = new NamespacedKey(plugin, "pet_type");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, petType.toLowerCase());
            }

            item.setItemMeta(meta);
        }

        return item;
    }
}