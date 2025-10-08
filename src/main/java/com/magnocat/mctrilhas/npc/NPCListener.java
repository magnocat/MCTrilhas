package com.magnocat.mctrilhas.npc;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

/**
 * Listener para capturar interações dos jogadores com os NPCs.
 * <p>
 * Este listener é o ponto de entrada para toda a lógica de diálogo e
 * início de quests.
 */
public class NPCListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public NPCListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        // Garante que os módulos de NPC e Diálogo estejam ativos
        if (plugin.getNpcManager() == null || plugin.getDialogueManager() == null) {
            return;
        }

        Npc npc = plugin.getNpcManager().getNpcByEntityId(event.getRightClicked().getUniqueId());

        if (npc != null) {
            // Cancela o evento padrão (ex: abrir GUI de troca de villager)
            event.setCancelled(true);

            // Verifica se o NPC tem um diálogo inicial definido.
            if (npc.startDialogueId() != null && !npc.startDialogueId().isEmpty()) {
                plugin.getDialogueManager().startDialogue(player, npc.startDialogueId());
            } else {
                // Mensagem padrão se não houver diálogo configurado.
                player.sendMessage(ChatColor.YELLOW + "[" + npc.name() + "]: " + ChatColor.WHITE + "Olá, " + player.getName() + "!");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        DialogueManager dialogueManager = plugin.getDialogueManager();

        if (dialogueManager == null) return;

        String inputType = dialogueManager.getAwaitedInputType(player);
        if (inputType == null) {
            return; // O jogador não está no modo de entrada de texto.
        }

        // Cancela o evento para que a mensagem não apareça no chat global.
        event.setCancelled(true);

        String message = event.getMessage();

        // Remove o jogador da lista de espera para que ele possa usar o chat normalmente.
        dialogueManager.removePlayerFromInputWait(player);

        if (message.equalsIgnoreCase("cancelar")) {
            player.sendMessage(ChatColor.YELLOW + "Operação cancelada.");
            return;
        }

        if ("set_custom_name".equals(inputType)) {
            PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
            if (playerData != null) {
                playerData.setCustomName(message);
                player.sendMessage(ChatColor.GREEN + "Entendido! A partir de agora, vou te chamar de " + message + ".");
            }
        }
    }
}