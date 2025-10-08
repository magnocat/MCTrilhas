package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeMenu;
import com.magnocat.mctrilhas.ctf.CTFMenu;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import com.magnocat.mctrilhas.badges.Badge;
import com.magnocat.mctrilhas.npc.DialogueMenu;
import org.bukkit.block.Biome;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.io.File;
import java.util.List;

/**
 * Listener para interações com menus de GUI.
 */
@SuppressWarnings("deprecation")
public class MenuListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public MenuListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Garante que quem clicou foi um jogador.
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        String viewTitle = event.getView().getTitle();

        // Verifica se o inventário clicado é um dos nossos menus.
        if (!viewTitle.startsWith(BadgeMenu.MENU_TITLE_PREFIX) &&
            !viewTitle.equals(DialogueMenu.MENU_TITLE) &&
            !viewTitle.equals(CTFMenu.MENU_TITLE)) {
            return;
        }

        // Impede que os jogadores peguem itens da GUI.
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();

        // Verifica se o item clicado é nulo ou um item de placeholder/decoração.
        if (clickedItem == null || clickedItem.getType() == Material.AIR || clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE || clickedItem.getType() == Material.BOOK) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        // Lógica para o menu de insígnias (paginação)
        if (viewTitle.startsWith(BadgeMenu.MENU_TITLE_PREFIX)) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey actionKey = new NamespacedKey(plugin, "gui_action");
            NamespacedKey pageKey = new NamespacedKey(plugin, "gui_target_page");

            if (container.has(actionKey, PersistentDataType.STRING) && container.has(pageKey, PersistentDataType.INTEGER)) {
                String action = container.get(actionKey, PersistentDataType.STRING);

                if ("page_previous".equals(action) || "page_next".equals(action)) {
                    // Extrai o nome do jogador alvo do título do inventário.
                    int targetPage = container.get(pageKey, PersistentDataType.INTEGER);
                    String tempTitle = viewTitle.substring(BadgeMenu.MENU_TITLE_PREFIX.length());
                    String targetName = tempTitle.substring(0, tempTitle.lastIndexOf(" ("));

                    // Reabre o menu na página correta.
                    if (plugin.getBadgeMenu() != null) {
                        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);
                        plugin.getBadgeMenu().open(player, target.getUniqueId(), target.getName(), targetPage);
                    }
                }
            }
        }
        // Lógica para o menu de diálogo
        else if (viewTitle.equals(DialogueMenu.MENU_TITLE)) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey actionKey = new NamespacedKey(plugin, "dialogue_action");

            if (container.has(actionKey, PersistentDataType.STRING)) {
                String action = container.get(actionKey, PersistentDataType.STRING);
                if (plugin.getDialogueManager() != null) {
                    // Lógica especial para o menu de reposição de mapas
                    if ("replace_badge_map_menu".equals(action)) {
                        player.closeInventory();
                        openReplaceMapMenu(player);
                        return;
                    }
                    handleDialogueAction(player, action);
                }
            }
        }
        // Lógica para o menu do CTF
        else if (viewTitle.equals(CTFMenu.MENU_TITLE)) {
            String itemName = ChatColor.stripColor(meta.getDisplayName());
            if (itemName.equalsIgnoreCase("Entrar na Fila")) {
                player.performCommand("ctf join");
            } else if (itemName.equalsIgnoreCase("Sair da Fila")) {
                player.performCommand("ctf leave");
            }
            player.closeInventory();
        }
    }

    private void handleDialogueAction(Player player, String action) {
        // Fecha o inventário para a maioria das ações, exceto para as que abrem outro diálogo.
        if (!action.startsWith("dialogue:")) {
            player.closeInventory();
        }

        if (action.startsWith("dialogue:")) {
            if (plugin.getDialogueManager() != null) {
                String nextDialogueId = action.substring("dialogue:".length());
                plugin.getDialogueManager().startDialogue(player, nextDialogueId);
            }
        } else if (action.startsWith("ranked_dialogue:")) {
            if (plugin.getDialogueManager() != null && plugin.getPlayerDataManager() != null) {
                String prefix = action.substring("ranked_dialogue:".length());
                PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                Rank playerRank = (playerData != null) ? playerData.getRank() : Rank.VISITANTE;

                // Tenta encontrar um diálogo específico para o ranque do jogador.
                String rankSpecificId = prefix + playerRank.name().toLowerCase();
                if (plugin.getDialogueManager().getDialogue(rankSpecificId) != null) {
                    plugin.getDialogueManager().startDialogue(player, rankSpecificId);
                } else {
                    // Se não encontrar, procura por um diálogo padrão de fallback.
                    String defaultId = prefix + "default";
                    if (plugin.getDialogueManager().getDialogue(defaultId) != null) {
                        plugin.getDialogueManager().startDialogue(player, defaultId);
                    } else {
                        // Se nem o padrão existir, informa o jogador.
                        player.sendMessage(ChatColor.YELLOW + "[Chefe Magno]: " + ChatColor.WHITE + "Hmm, ainda não tenho uma dica sobre isso para o seu nível de experiência.");
                        plugin.logWarn("Nenhum diálogo ranqueado ou padrão encontrado para o prefixo: '" + prefix + "'");
                    }
                }
            }
        } else if (action.startsWith("timed_dialogue:")) {
            if (plugin.getDialogueManager() != null) {
                String prefix = action.substring("timed_dialogue:".length());
                long worldTime = player.getWorld().getTime();
                String timeOfDay = (worldTime >= 0 && worldTime < 12300) ? "day" : "night"; // 12300 é aproximadamente o pôr do sol

                String timeSpecificId = prefix + timeOfDay;
                if (plugin.getDialogueManager().getDialogue(timeSpecificId) != null) {
                    plugin.getDialogueManager().startDialogue(player, timeSpecificId);
                } else {
                    // Fallback para um diálogo padrão se o específico não existir
                    String defaultId = prefix + "default";
                    if (plugin.getDialogueManager().getDialogue(defaultId) != null) {
                        plugin.getDialogueManager().startDialogue(player, defaultId);
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "[Chefe Magno]: " + ChatColor.WHITE + "Hmm, não tenho nenhuma dica para este momento do dia.");
                        plugin.logWarn("Nenhum diálogo de tempo ou padrão encontrado para o prefixo: '" + prefix + "'");
                    }
                }
            }
        } else if (action.startsWith("biome_dialogue:")) {
            if (plugin.getDialogueManager() != null) {
                String prefix = action.substring("biome_dialogue:".length());
                Biome biome = player.getWorld().getBiome(player.getLocation());
                String biomeName = biome.name().toLowerCase();

                String biomeSpecificId = prefix + biomeName;
                if (plugin.getDialogueManager().getDialogue(biomeSpecificId) != null) {
                    plugin.getDialogueManager().startDialogue(player, biomeSpecificId);
                } else {
                    String defaultId = prefix + "default";
                    if (plugin.getDialogueManager().getDialogue(defaultId) != null) {
                        plugin.getDialogueManager().startDialogue(player, defaultId);
                    } else {
                        player.sendMessage(ChatColor.YELLOW + "[Chefe Magno]: " + ChatColor.WHITE + "Este é um lugar interessante, mas ainda não tenho dicas específicas sobre ele.");
                        plugin.logWarn("Nenhum diálogo de bioma ou padrão encontrado para o prefixo: '" + prefix + "'");
                    }
                }
            }
        } else if (action.startsWith("locate_biome:")) {
            String biomeId = action.substring("locate_biome:".length());
            FileConfiguration biomeConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "biome_locations.yml"));
            String path = "locations." + biomeId;
            if (biomeConfig.isConfigurationSection(path)) {
                String displayName = biomeConfig.getString(path + ".display-name", "este bioma");
                String coords = biomeConfig.getString(path + ".coordinates", "local desconhecido");
                player.sendMessage(ChatColor.GOLD + "[Chefe Magno]: " + ChatColor.WHITE + "Claro! Encontrei um(a) " + displayName + " nas coordenadas: " + ChatColor.AQUA + coords);
            } else {
                player.sendMessage(ChatColor.YELLOW + "[Chefe Magno]: " + ChatColor.WHITE + "Hmm, ainda não mapeei a localização deste bioma. A exploração é parte da aventura!");
            }
        } else if (action.startsWith("grant_badge:")) {
            String badgeId = action.substring("grant_badge:".length());
            boolean success = plugin.getPlayerDataManager().grantBadgeAndReward(player, badgeId);
            if (!success) {
                player.sendMessage(ChatColor.YELLOW + "[Chefe Magno]: " + ChatColor.WHITE + "Parece que você já tem esta insígnia, escoteiro!");
            }
        } else if (action.startsWith("replace_badge_map:")) {
            String badgeId = action.substring("replace_badge_map:".length());
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

            // Dupla verificação de segurança
            if (playerData != null && playerData.hasBadge(badgeId)) {
                ItemStack mapItem = plugin.getMapRewardManager().createMapReward(player, badgeId);
                player.getInventory().addItem(mapItem);
                player.sendMessage(ChatColor.GOLD + "[Chefe Magno]: " + ChatColor.WHITE + "Aqui está uma cópia do seu troféu. Cuide bem dele!");
            } else {
                player.sendMessage(ChatColor.RED + "Ocorreu um erro. Parece que você não possui mais esta insígnia.");
            }
        } else if (action.startsWith("command:player:")) {
            String command = action.substring("command:player:".length());
            player.performCommand(command);
        } else if (action.startsWith("input:")) {
            String inputType = action.substring("input:".length());
            if ("set_custom_name".equals(inputType) && plugin.getDialogueManager() != null) {
                plugin.getDialogueManager().awaitPlayerInput(player, "set_custom_name");
                player.sendMessage(ChatColor.GOLD + "[Chefe Magno]:" + ChatColor.WHITE + " Claro! Como você gostaria de ser chamado? Digite seu nome no chat.");
                player.sendMessage(ChatColor.GRAY + "(Digite 'cancelar' para desistir)");
            }
        } else if (action.equalsIgnoreCase("close")) {
            // Apenas fecha o inventário, o que já foi feito no início.
        } else {
            player.sendMessage(ChatColor.GRAY + "[DEBUG] Ação '" + action + "' ainda não implementada.");
        }
    }

    /**
     * Abre um menu dinâmico que lista apenas as insígnias que o jogador já conquistou,
     * permitindo que ele escolha qual mapa-troféu deseja repor.
     * @param player O jogador para quem o menu será aberto.
     */
    private void openReplaceMapMenu(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Não foi possível carregar seus dados.");
            return;
        }

        Inventory menu = Bukkit.createInventory(null, 54, "Repor Mapa-Troféu");
        int slot = 0;

        for (String badgeId : playerData.getEarnedBadgesMap().keySet()) {
            Badge badge = plugin.getBadgeManager().getBadge(badgeId);
            if (badge != null && slot < 54) {
                ItemStack item = new ItemStack(Material.FILLED_MAP);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + badge.name());
                    meta.setLore(List.of(ChatColor.GRAY + "Clique para receber uma cópia", ChatColor.GRAY + "deste troféu."));

                    NamespacedKey actionKey = new NamespacedKey(plugin, "dialogue_action");
                    meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "replace_badge_map:" + badge.id());
                    item.setItemMeta(meta);
                }
                menu.setItem(slot++, item);
            }
        }
        player.openInventory(menu);
    }
}
