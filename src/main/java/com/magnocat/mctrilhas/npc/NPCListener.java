package com.magnocat.mctrilhas.npc;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
}