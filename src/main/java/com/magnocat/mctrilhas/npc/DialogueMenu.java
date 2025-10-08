package com.magnocat.mctrilhas.npc;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Constrói e gerencia a interface gráfica (GUI) para os diálogos com NPCs.
 */
public class DialogueMenu {

    public static final String MENU_TITLE = "Diálogo"; // O título pode ser dinâmico no futuro
    private final MCTrilhasPlugin plugin;

    public DialogueMenu(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Abre a GUI de diálogo para um jogador.
     *
     * @param player O jogador que verá o diálogo.
     * @param dialogue O objeto de diálogo a ser exibido.
     */
    public void open(Player player, Dialogue dialogue) {
        // 3 linhas por padrão, pode ser dinâmico depois.
        Inventory gui = Bukkit.createInventory(null, 27, MENU_TITLE);

        // Cria o item que representa o texto do NPC
        ItemStack npcTextItem = new ItemStack(Material.BOOK);
        ItemMeta npcTextMeta = npcTextItem.getItemMeta();
        if (npcTextMeta != null) {
            npcTextMeta.setDisplayName(ChatColor.YELLOW + "Mensagem");
            List<String> lore = new ArrayList<>();

            // --- LÓGICA DE NOME PERSONALIZADO ---
            String playerName;
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

            if (playerData != null && playerData.getCustomName() != null && !playerData.getCustomName().isEmpty()) {
                // Se tem nome customizado, usa "Ranque + Nome"
                Rank rank = playerData.getRank();
                String rankName = (rank != null && rank.getDisplayName() != null) ? rank.getDisplayName() : "";
                playerName = rankName + " " + playerData.getCustomName();
            } else {
                // Lógica de fallback para nomes especiais ou nome padrão
                UUID playerUUID = player.getUniqueId();
                if (playerUUID.equals(UUID.fromString("7227ed6f-4552-4f18-b8d4-4a9f2f30898a"))) { // Miguel
                    playerName = "Miguel Baconzito";
                } else if (playerUUID.equals(UUID.fromString("dcf43f99-35bb-4722-afa0-45ae55d87460"))) { // Magno
                    playerName = "Mestre dos Magnos";
                } else {
                    playerName = player.getName();
                }
            }

            // Adiciona as linhas de texto, substituindo o placeholder pelo nome correto.
            dialogue.npcText().forEach(line -> lore.add(ChatColor.translateAlternateColorCodes('&', line.replace("{player_name}", playerName))));

            npcTextMeta.setLore(lore);
            npcTextMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            npcTextItem.setItemMeta(npcTextMeta);
        }
        gui.setItem(4, npcTextItem); // Posição central superior

        // Cria os itens para as escolhas do jogador
        for (Map.Entry<Integer, DialogueChoice> entry : dialogue.choices().entrySet()) {
            int slot = 17 + entry.getKey(); // Começa do slot 18 em diante
            if (slot >= 27) continue; // Limita ao tamanho do inventário

            DialogueChoice choice = entry.getValue();
            ItemStack choiceItem = new ItemStack(Material.PAPER);
            ItemMeta choiceMeta = choiceItem.getItemMeta();

            if (choiceMeta != null) {
                choiceMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', choice.text()));

                // Armazena a ação no item para que o MenuListener possa lê-la
                NamespacedKey actionKey = new NamespacedKey(plugin, "dialogue_action");
                choiceMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, choice.action());

                choiceItem.setItemMeta(choiceMeta);
            }
            gui.setItem(slot, choiceItem);
        }

        player.openInventory(gui);
    }
}