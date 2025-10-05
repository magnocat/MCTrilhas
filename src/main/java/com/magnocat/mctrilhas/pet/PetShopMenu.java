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
        gui.setItem(11, createPetItem(Material.BONE, "Lobo", "Um companheiro leal e forte em combate.", "&6Habilidade: &fGuarda-Costas\n&7Ataca seus alvos.\n&6Passiva: &fUivo do Líder\n&7Concede Força I temporária.", hasPet, hasRank));
        gui.setItem(13, createPetItem(Material.COD, "Gato", "Um amigo ágil que espanta Creepers.", "&6Habilidade: &fAlerta Felino\n&7Alerta sobre monstros próximos.\n&6Passiva: &fVisão Felina\n&7Concede Visão Noturna no escuro.", hasPet, hasRank));
        gui.setItem(15, createPetItem(Material.CARROT, "Porco", "Um ajudante que coleta itens para você.", "&6Habilidade: &fFaro Fino\n&7Coleta itens caídos no chão.\n&6Passiva: &fCaçador de Trufas\n&7Pode encontrar itens no chão.", hasPet, hasRank));
        gui.setItem(17, createPetItem(Material.FEATHER, "Papagaio", "Um amigo que te dá uma visão de longo alcance.", "&6Habilidade: &fOlho de Águia\n&7Concede super zoom ao se agachar.", hasPet, hasRank));
        gui.setItem(19, createPetItem(Material.AMETHYST_SHARD, "Allay", "Uma criatura mágica que coleta itens para você.", "&6Habilidade: &fColetor Inteligente\n&7Coleta itens do tipo que você segura.\n&6Passiva: &fHarmonia Musical\n&7Ganha velocidade perto de Note Blocks.", hasPet, hasRank));
        gui.setItem(21, createPetItem(Material.WHITE_WOOL, "Ovelha", "Uma companheira dócil que fornece lã colorida.", "&6Habilidade: &fLã Camaleônica\n&7Pode ser tosquiada para obter lã.\n&6Passiva: &fAura Calmante\n&7Concede Regeneração fora de combate.", hasPet, hasRank));
        gui.setItem(23, createPetItem(Material.BUCKET, "Vaca", "Uma amiga que fornece leite e purifica você.", "&6Habilidade: &fFonte de Leite\n&7Pode ser ordenhada com um balde.\n&6Passiva: &fAura Purificante\n&7Remove efeitos negativos de você.", hasPet, hasRank));
        gui.setItem(25, createPetItem(Material.EGG, "Galinha", "Uma companheira que te salva de quedas.", "&6Habilidade: &fBotar Ovo\n&7Bota um ovo ao ser estimulada.\n&6Passiva: &fQueda Suave\n&7Amortece suas quedas.", hasPet, hasRank));

        // --- Pets Futuros (Em Breve) ---
        gui.setItem(33, createComingSoonItem(Material.SCUTE, "Tatu", "Um protetor blindado."));

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