package com.magnocat.mctrilhas.chat;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ouve os eventos de chat para implementar a funcionalidade de chat por proximidade.
 */
public class ProximityChatListener implements Listener {

    private final MCTrilhasPlugin plugin;
    private final ProximityChatManager chatManager;

    public ProximityChatListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.chatManager = plugin.getProximityChatManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!chatManager.isEnabled()) {
            return;
        }

        Player sender = event.getPlayer();
        String message = event.getMessage();
        int radius = chatManager.getRadius();
        int radiusSquared = radius * radius; // Usar o quadrado da distância é mais performático

        // Filtra os destinatários para incluir apenas jogadores dentro do raio
        Set<Player> recipients = event.getRecipients().stream()
                .filter(recipient -> recipient.getWorld().equals(sender.getWorld()) &&
                                     recipient.getLocation().distanceSquared(sender.getLocation()) <= radiusSquared)
                .collect(Collectors.toSet());

        // Limpa os destinatários originais e adiciona apenas os que estão próximos
        event.getRecipients().clear();
        event.getRecipients().addAll(recipients);

        // Formata a mensagem para indicar que é um chat de proximidade
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', chatManager.getFormat())
                .replace("{player}", "%1$s").replace("{message}", "%2$s");
        event.setFormat(formattedMessage);
    }
}