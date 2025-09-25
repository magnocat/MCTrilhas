package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.lang.reflect.Field;
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
        gui.setItem(11, createPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzI2NTIzN2U3ZDM2N2U5OTZlOTQyYjY3MjFkYjNlYjI3YjYxM2YwZDIwNDk0ZGMzMmI0ZGE1NmM0YjI5ZGMxZiJ9fX0=", "Lobo", "Um companheiro leal e forte em combate.", "&6Habilidade: &fGuarda-Costas\n&7Ataca monstros que te atacam\n&7ou que você ataca.", hasPet, hasRank));
        gui.setItem(13, createPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjdlY2M3MTRmZTRlYjhiNDk5ZDQzYjY4Y2MzY2YxM2ZlZDA3YjM1ZWE2NTE0YTYxYjM3YjQxY2FjYjE0YjY5In19fQ==", "Gato", "Um amigo ágil que pode alertá-lo sobre perigos.", "&6Habilidade: &fAlerta Felino\n&7Emite um som e uma partícula\n&7quando monstros se aproximam.", hasPet, hasRank));
        gui.setItem(15, createPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjIxNjY4ZWY3Y2I3OWRkOWMyMmNlM2QxZjNmNGMyOGMyMjU5NDg3M2JjMzNjMWU3Y2Q3YjY3Y2Y4ZmIyZDEifX19", "Porco", "Um ajudante que pode coletar itens para você.", "&6Habilidade: &fFaro Fino\n&7Coleta itens caídos no chão\n&7e os entrega para você.", hasPet, hasRank));

        player.openInventory(gui);
    }

    private ItemStack createPetHead(String texture, String petType, String description, String ability, boolean alreadyOwns, boolean hasRequiredRank) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
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

            // Aplica a textura customizada à cabeça
            if (meta instanceof SkullMeta) {
                SkullMeta skullMeta = (SkullMeta) meta;
                GameProfile profile = new GameProfile(java.util.UUID.randomUUID(), null);
                profile.getProperties().put("textures", new Property("textures", texture));
                try {
                    Field profileField = skullMeta.getClass().getDeclaredField("profile");
                    profileField.setAccessible(true);
                    profileField.set(skullMeta, profile);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            item.setItemMeta(meta);
        }

        return item;
    }
}