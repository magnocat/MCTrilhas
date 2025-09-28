package com.magnocat.mctrilhas.menus;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.Badge;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.persistence.PersistentDataType;
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
    private static final int BADGES_PER_PAGE = 45; // 5 linhas de 9 slots
    private static final int INVENTORY_SIZE = 54; // 6 linhas
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
     * @param page O número da página a ser exibida (começando em 1).
     */
    public void open(Player viewer, UUID targetUUID, String targetName, int page) {
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
        int totalPages = (int) Math.ceil((double) badges.size() / BADGES_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        // Garante que a página solicitada seja válida.
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        String menuTitle = MENU_TITLE_PREFIX + targetName + " (" + page + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, menuTitle);

        // Calcula o subconjunto de insígnias para a página atual.
        int startIndex = (page - 1) * BADGES_PER_PAGE;
        int endIndex = Math.min(startIndex + BADGES_PER_PAGE, badges.size());
        List<Badge> pageBadges = badges.subList(startIndex, endIndex);

        // O PlayerData final precisa ser acessível dentro do forEach.
        final PlayerData finalPlayerData = playerData;
        pageBadges.forEach(badge -> {
            ItemStack badgeItem = createBadgeItem(badge, finalPlayerData);
            inv.addItem(badgeItem);
        });

        // Adiciona os botões de navegação.
        addNavigationButtons(inv, page, totalPages);

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
            String iconName = badge.icon();
            if (iconName == null || iconName.isEmpty()) {
                throw new IllegalArgumentException("Nome do ícone está vazio.");
            }
            iconMaterial = Material.valueOf(iconName.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            plugin.getLogger().warning("Ícone inválido ou não definido para a insígnia '" + badge.id() + "'. Usando BARRIER como padrão.");
            iconMaterial = Material.BARRIER;
        }

        ItemStack item = new ItemStack(iconMaterial);
        ItemMeta meta = item.getItemMeta();

        // É uma boa prática verificar se o meta não é nulo.
        if (meta == null) {
            return item;
        }

        boolean hasBadge = playerData.hasBadge(badge.id());

        // Define o nome do item, com cor baseada no status (conquistada ou não).
        meta.setDisplayName((hasBadge ? ChatColor.GOLD : ChatColor.GRAY) + badge.name());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_AQUA + badge.description());
        lore.add(" "); // Linha em branco para espaçamento

        if (hasBadge) {
            lore.add(ChatColor.GREEN + "✔ Conquistada!");
        } else {
            double progress = playerData.getProgress(badge.type());
            double required = badge.requirement();
            String progressFormatted = numberFormat.format(progress);
            String requiredFormatted = numberFormat.format(required);

            lore.add(ChatColor.YELLOW + "Progresso: " + ChatColor.WHITE + progressFormatted + " / " + requiredFormatted);
            lore.add(ChatColor.RED + "✖ Não conquistada");
        }

        lore.add(" ");
        lore.add(ChatColor.DARK_GRAY + "Tipo: " + badge.type().getDisplayName());

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Adiciona os botões de navegação (anterior, página atual, próximo) ao inventário.
     * @param inv O inventário onde os botões serão adicionados.
     * @param currentPage A página atual.
     * @param totalPages O número total de páginas.
     */
    private void addNavigationButtons(Inventory inv, int currentPage, int totalPages) {
        // Botão de página anterior (se não estiver na primeira página)
        if (currentPage > 1) {
            inv.setItem(45, createNavButton(Material.ARROW, ChatColor.YELLOW + "Página Anterior", "page_previous", currentPage - 1));
        } else {
            inv.setItem(45, createPlaceholderButton());
        }

        // Indicador da página atual
        ItemStack pageIndicator = new ItemStack(Material.BOOK);
        ItemMeta meta = pageIndicator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Página " + currentPage + " de " + totalPages);
            pageIndicator.setItemMeta(meta);
        }
        inv.setItem(49, pageIndicator);

        // Botão de próxima página (se não estiver na última página)
        if (currentPage < totalPages) {
            inv.setItem(53, createNavButton(Material.ARROW, ChatColor.YELLOW + "Próxima Página", "page_next", currentPage + 1));
        } else {
            inv.setItem(53, createPlaceholderButton());
        }
    }

    private ItemStack createNavButton(Material material, String name, String action, int targetPage) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            // Armazena a ação e a página de destino nos dados persistentes do item.
            NamespacedKey actionKey = new NamespacedKey(plugin, "gui_action");
            NamespacedKey pageKey = new NamespacedKey(plugin, "gui_target_page");
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, targetPage);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlaceholderButton() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Nome vazio para que o item não tenha texto.
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
}
