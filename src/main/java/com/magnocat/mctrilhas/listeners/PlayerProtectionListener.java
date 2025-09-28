package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Listener para proteger o servidor contra ações de jogadores com o ranque VISITANTE.
 * Impede que novos jogadores modifiquem o mundo antes de serem promovidos.
 */
public class PlayerProtectionListener implements Listener {

    private final MCTrilhasPlugin plugin;
    // Lista de comandos que um visitante PODE usar. Todos os outros serão bloqueados.
    private static final List<String> ALLOWED_VISITOR_COMMANDS = Arrays.asList(
            "scout", "trilhas", "insignia", "escoteiro", "ranque", "rank", "familia", "family", "ajuda", "help", "regras", "rules"
    );

    public PlayerProtectionListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Verifica se o jogador é um visitante antes de permitir uma ação.
     * @param player O jogador a ser verificado.
     * @return true se o jogador for um visitante, false caso contrário.
     */
    private boolean isVisitor(Player player) {
        // Usamos o PlayerDataManager para obter o ranque, que é a fonte mais confiável.
        return plugin.getPlayerDataManager().getRank(player.getUniqueId()) == Rank.VISITANTE;
    }

    /**
     * Envia uma mensagem padrão para o visitante informando que a ação é restrita.
     * @param player O jogador que receberá a mensagem.
     */
    private void sendRestrictionMessage(Player player) {
        player.sendMessage(ChatColor.RED + "Como visitante, você não pode interagir com o mundo. Peça para um membro te apadrinhar ou faça sua aplicação no site!");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isVisitor(event.getPlayer())) {
            event.setCancelled(true);
            sendRestrictionMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isVisitor(event.getPlayer())) {
            event.setCancelled(true);
            sendRestrictionMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) {
            return;
        }

        // Lista de blocos interativos que devem ser protegidos
        switch (event.getClickedBlock().getType()) {
            case CHEST:
            case TRAPPED_CHEST:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case BARREL:
            case SHULKER_BOX:
                if (isVisitor(event.getPlayer())) {
                    event.setCancelled(true);
                    sendRestrictionMessage(event.getPlayer());
                }
                break;
            default:
                // Permite a interação com outros blocos (botões, portas, etc.)
                break;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVisitorCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isVisitor(player)) {
            String commandLabel = event.getMessage().substring(1).split(" ")[0].toLowerCase();

            if (!ALLOWED_VISITOR_COMMANDS.contains(commandLabel)) {
                event.setCancelled(true);
                sendRestrictionMessage(player);
            }
        }
    }
}