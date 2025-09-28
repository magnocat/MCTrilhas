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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.Base64;
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
        gui.setItem(11, createPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzI2NTIzN2U3ZDM2N2U5OTZlOTQyYjY3MjFkYjNlYjI3YjYxM2YwZDIwNDk0ZGMzMmI0ZGE1NmM0YjI5ZGMxZiJ9fX0=", "Lobo", "Um companheiro leal e forte em combate.", "&6Habilidade: &fGuarda-Costas\n&7Ataca monstros que te atacam\n&7ou que você ataca.", hasPet, hasRank));
        gui.setItem(13, createPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjdlY2M3MTRmZTRlYjhiNDk5ZDQzYjY4Y2MzY2YxM2ZlZDA3YjM1ZWE2NTE0YTYxYjM3YjQxY2FjYjE0YjY5In19fQ==", "Gato", "Um amigo ágil que pode alertá-lo sobre perigos.", "&6Habilidade: &fAlerta Felino\n&7Emite um som e uma partícula\n&7quando monstros se aproximam.", hasPet, hasRank));
        gui.setItem(15, createPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjIxNjY4ZWY3Y2I3OWRkOWMyMmNlM2QxZjNmNGMyOGMyMjU5NDg3M2JjMzNjMWU3Y2Q3YjY3Y2Y4ZmIyZDEifX19", "Porco", "Um ajudante que pode coletar itens para você.", "&6Habilidade: &fFaro Fino\n&7Coleta itens caídos no chão\n&7e os entrega para você.", hasPet, hasRank));

        // --- Pets Futuros (Em Breve) ---
        gui.setItem(20, createComingSoonPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQ0ZTMzMzFiYjI0MmY1YTJkYWI3N2VlNWY4YjI5MDk4NDU5YjNmY2Y5ZGUyMjY5OTRmODg5NjRkYjNiYjYwZCJ9fX0=", "Papagaio", "Um amigo colorido que pode imitar sons e sentar no seu ombro."));
        gui.setItem(22, createComingSoonPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQzMjQwMjU5NzYxY2YxZWM3MDQxZTUwM2YyYjM5Y2M5YjU5NzYxYjE0ZjEwZTAwYjI3Y2QxM2ZlZjM5YTYzMyJ9fX0=", "Allay", "Uma criatura mágica que ajuda a coletar itens para você."));
        gui.setItem(24, createComingSoonPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2E5Y2Y3YjU0ZGU0ZGI0Y2E5N2Y1Y2U5MjI4YjY3ODg5YjY3Y2E3Y2Y0M2Y0ZDA0NjY2YjY4ZTFkYjQ1MyJ9fX0=", "Ovelha", "Uma companheira dócil que pode fornecer lã."));
        gui.setItem(29, createComingSoonPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWQ2YzZlZGE5NDJmN2Y1ZjcxYmFhYTAwNTdlMDFmZWI0MGU0M2I4ZDUxZTk5YjI4MThhZDE2YjBkZjgxMmY4In19fQ==", "Vaca", "Uma amiga que pode fornecer leite."));
        gui.setItem(31, createComingSoonPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTYzODZjMGFmNzgzZWNlMTg3MGI0NDZlM2M0M2NlM2YxYmY5ZGUzYjM2MTY4YjY5N2YxYjM3Y2FkM2Q2YjQifX19", "Galinha", "Pode botar ovos periodicamente."));
        gui.setItem(33, createComingSoonPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQyN2E1ODc1Y2U4NjI1OWMyODVjYjQwYjM5ZjY5NmI0M2MyY2I4M2M3YjY5M2YyY2Y3Y2YxM2Y1YjYyYjMyMiJ9fX0=", "Tatu", "Pode se enrolar para se defender e fornecer escamas."));

        // --- Pets VIP Futuros (Em Breve) ---
        gui.setItem(38, createVipComingSoonPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDQ2ZDIzZDM4M2ExMDQxNGI0M2I3YjQxM2Y0YjQyYjYyYjJlYjYxM2I4ZWM3YjI5Zjk0YmU3Y2UyYjM3YjIifX19", "Urso Polar", "Um protetor poderoso e imponente."));
        gui.setItem(40, createVipComingSoonPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Q1NDE1NDFkYmI4N2ExN2ZhNGJmZDUxN2M3OTM0YjQyMDIxZTM0YjA5YmU0MGE3YjVjYmY0ODQyYmMyYzAifX19", "Aranha", "Uma companheira que pode escalar paredes."));
        gui.setItem(42, createVipComingSoonPetHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzA0MGZlODM5YjY0MTg4Yjc2NmQxYjY5ZGM3YjYxY2M3YjQ4YjE4MmYxM2E0YjU5ZjEwY2ZkZGMzYjYwZCJ9fX0=", "Tartaruga", "Uma amiga resistente que oferece proteção."));

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
                // Método moderno e seguro para aplicar texturas, sem reflexão.
                // Isso resolve o erro de 'IllegalArgumentException' em versões recentes do Paper.
                SkullMeta skullMeta = (SkullMeta) meta;
                PlayerProfile profile = Bukkit.createPlayerProfile(java.util.UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                // A textura é um JSON codificado em Base64. Nós o decodificamos e o aplicamos.
                String decodedTexture = new String(Base64.getDecoder().decode(texture));
                textures.setSkin(decodedTexture);
                profile.setTextures(textures);
                skullMeta.setPlayerProfile(profile);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createComingSoonPetHead(String texture, String petType, String description) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
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

            // Aplica a textura customizada à cabeça
            if (meta instanceof SkullMeta) {
                // Método moderno e seguro para aplicar texturas.
                SkullMeta skullMeta = (SkullMeta) meta;
                PlayerProfile profile = Bukkit.createPlayerProfile(java.util.UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                String decodedTexture = new String(Base64.getDecoder().decode(texture));
                textures.setSkin(decodedTexture);
                profile.setTextures(textures);
                skullMeta.setPlayerProfile(profile);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createVipComingSoonPetHead(String texture, String petType, String description) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
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

            // Aplica a textura customizada à cabeça
            if (meta instanceof SkullMeta) {
                // Método moderno e seguro para aplicar texturas.
                SkullMeta skullMeta = (SkullMeta) meta;
                PlayerProfile profile = Bukkit.createPlayerProfile(java.util.UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                String decodedTexture = new String(Base64.getDecoder().decode(texture));
                textures.setSkin(decodedTexture);
                profile.setTextures(textures);
                skullMeta.setPlayerProfile(profile);
            }
            item.setItemMeta(meta);
        }

        return item;
    }
}