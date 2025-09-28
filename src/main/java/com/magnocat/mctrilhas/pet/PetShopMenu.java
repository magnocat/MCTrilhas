package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
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
        // Aumentamos o inventário para 6 linhas (54 slots) para caber todos os pets
        Inventory gui = Bukkit.createInventory(null, 54, INVENTORY_TITLE);

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        boolean hasPet = playerData != null && playerData.getPetData() != null && playerData.getPetData().isOwned();
        boolean hasRank = playerData != null && playerData.getRank().ordinal() >= Rank.ESCOTEIRO.ordinal();

        // --- Pets Disponíveis ---
        gui.setItem(11, createPetItem(Material.BONE, "Lobo", "Um companheiro leal e forte em combate.", "&6Habilidade: &fGuarda-Costas\n&7Ataca monstros que te atacam\n&7ou que você ataca.", hasPet, hasRank));
        gui.setItem(13, createPetItem(Material.COD, "Gato", "Um amigo ágil que pode alertá-lo sobre perigos.", "&6Habilidade: &fAlerta Felino\n&7Emite um som e uma partícula\n&7quando monstros se aproximam.", hasPet, hasRank));
        gui.setItem(15, createPetItem(Material.CARROT, "Porco", "Um ajudante que pode coletar itens para você.", "&6Habilidade: &fFaro Fino\n&7Coleta itens caídos no chão\n&7e os entrega para você.", hasPet, hasRank));

        // --- Pets Futuros (Em Breve) ---
        gui.setItem(20, createComingSoonItem(Material.FEATHER, "Papagaio", "Um amigo colorido que pode imitar sons e sentar no seu ombro."));
        gui.setItem(22, createComingSoonItem(Material.AMETHYST_SHARD, "Allay", "Uma criatura mágica que ajuda a coletar itens para você."));
        gui.setItem(24, createComingSoonItem(Material.WHITE_WOOL, "Ovelha", "Uma companheira dócil que pode fornecer lã."));
        gui.setItem(29, createComingSoonItem(Material.BUCKET, "Vaca", "Uma amiga que pode fornecer leite."));
        gui.setItem(31, createComingSoonItem(Material.EGG, "Galinha", "Pode botar ovos periodicamente."));
        gui.setItem(33, createComingSoonItem(Material.SCUTE, "Tatu", "Pode se enrolar para se defender e fornecer escamas."));

        // --- Pets VIP Futuros (Em Breve) ---
        gui.setItem(38, createVipComingSoonItem(Material.SNOWBALL, "Urso Polar", "Um protetor poderoso e imponente."));
        gui.setItem(40, createVipComingSoonItem(Material.SPIDER_EYE, "Aranha", "Uma companheira que pode escalar paredes."));
        gui.setItem(42, createVipComingSoonItem(Material.TURTLE_HELMET, "Tartaruga", "Uma amiga resistente que oferece proteção."));

        player.openInventory(gui);
    }

    private ItemStack createPetItem(Material material, String petType, String description, String ability, boolean alreadyOwns, boolean hasRequiredRank) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + petType);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add(" ");
            // Adiciona a descrição da habilidade, quebrando as linhas
            for (String line : ability.split("\n")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
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
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Esconde o texto "+1 Attack Damage"

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

    private ItemStack createComingSoonItem(Material material, String petType, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + "" + ChatColor.BOLD + petType);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + description);
            lore.add(" ");
            lore.add(ChatColor.AQUA + "✨ Em Breve...");
            meta.setLore(lore);

            // Adiciona um brilho para destacar que é especial
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createVipComingSoonItem(Material material, String petType, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + petType);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + description);
            lore.add(" ");
            lore.add(ChatColor.LIGHT_PURPLE + "✨ Exclusivo para VIPs (Em Breve)");
            meta.setLore(lore);

            // Adiciona um brilho para destacar que é especial
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }
}