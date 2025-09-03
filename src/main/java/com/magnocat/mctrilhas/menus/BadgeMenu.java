package com.magnocat.mctrilhas.menus;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.Badge;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Gerencia a criação e exibição do menu de GUI de insígnias.
 */
@SuppressWarnings("deprecation")
public class BadgeMenu {

    private final MCTrilhasPlugin plugin;
    // O título do inventário é usado para identificá-lo em listeners.
    public static final String MENU_TITLE_PREFIX = "Insígnias de ";
    private final NumberFormat numberFormat;

    public BadgeMenu(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        // Usar NumberFormat é mais limpo e localizado para formatação de números.
        this.numberFormat = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
        this.numberFormat.setGroupingUsed(true);
    }

    /**
     * Abre o menu de insígnias para um jogador específico, visualizado por outro jogador.
     * @param viewer O jogador que verá o menu.
     * @param targetUUID O UUID do jogador cujas insígnias estão sendo exibidas.
     * @param targetName O nome do jogador cujas insígnias estão sendo exibidas.
     */
    public void open(Player viewer, UUID targetUUID, String targetName) {
        // Tenta obter os dados do jogador do cache. Se não estiverem lá, carrega-os.
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(targetUUID);
        if (playerData == null) {
            plugin.getPlayerDataManager().loadPlayerData(targetUUID);
            playerData = plugin.getPlayerDataManager().getPlayerData(targetUUID);
            // Se ainda assim não for possível carregar, o jogador provavelmente nunca entrou ou há um erro.
            if (playerData == null) {
                viewer.sendMessage(ChatColor.RED + "Não foi possível carregar os dados do jogador " + targetName + ".");
                return;
            }
        }

        List<Badge> badges = plugin.getBadgeManager().getAllBadges();
        // Calcula o tamanho do inventário para caber todas as insígnias, arredondando para o múltiplo de 9 mais próximo.
        // Máximo de 54 slots (6 linhas).
        int inventorySize = Math.min(54, (int) (Math.ceil(badges.size() / 9.0) * 9));
        if (inventorySize == 0) inventorySize = 9; // Garante um tamanho mínimo.

        Inventory inv = Bukkit.createInventory(null, inventorySize, MENU_TITLE_PREFIX + targetName);

        // O PlayerData final precisa ser acessível dentro do forEach.
        final PlayerData finalPlayerData = playerData;
        badges.forEach(badge -> {
            ItemStack badgeItem = createBadgeItem(badge, finalPlayerData);
            inv.addItem(badgeItem);
        });

        viewer.openInventory(inv);
        // Adiciona um feedback sonoro para o jogador.
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
    }

    /**
     * Cria o ItemStack que representa uma insígnia na GUI.
     * @param badge A insígnia a ser representada.
     * @param playerData Os dados do jogador para verificar o progresso e status.
     * @return O ItemStack configurado.
     */
    private ItemStack createBadgeItem(Badge badge, PlayerData playerData) {
        Material iconMaterial;
        try {
            // Garante que o ícone não seja nulo antes de tentar usá-lo.
            String iconName = badge.getIcon();
            if (iconName == null || iconName.isEmpty()) {
                throw new IllegalArgumentException("Nome do ícone está vazio.");
            }
            iconMaterial = Material.valueOf(iconName.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            plugin.getLogger().warning("Ícone inválido ou não definido para a insígnia '" + badge.getId() + "'. Usando BARRIER como padrão.");
            iconMaterial = Material.BARRIER;
        }

        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();

        // É uma boa prática verificar se o meta não é nulo.
        if (meta == null) {
            return item;
        }

        boolean hasBadge = playerData.hasBadge(badge.getId());

        // Define o nome do item, com cor baseada no status (conquistada ou não).
        meta.setDisplayName((hasBadge ? ChatColor.GOLD : ChatColor.GRAY) + badge.getName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_AQUA + badge.getDescription());
        lore.add(" "); // Linha em branco para espaçamento

        if (hasBadge) {
            lore.add(ChatColor.GREEN + "✔ Conquistada!");
        } else {
            double progress = playerData.getProgress(badge.getType());
            double required = badge.getRequirement();
            String progressFormatted = numberFormat.format(progress);
            String requiredFormatted = numberFormat.format(required);

            lore.add(ChatColor.YELLOW + "Progresso: " + ChatColor.WHITE + progressFormatted + " / " + requiredFormatted);
            lore.add(ChatColor.RED + "✖ Não conquistada");
        }

        lore.add(" ");
        lore.add(ChatColor.DARK_GRAY + "Tipo: " + badge.getType().getName());

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Cria o ItemStack que representa o botão de fechar a GUI.
     * @return O ItemStack configurado.
     */
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Fechar");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Clique para fechar o menu.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
