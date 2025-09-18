package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class AdminPrivacyListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public AdminPrivacyListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerUuid = player.getUniqueId().toString();

        List<String> hiddenUuids = plugin.getConfig().getStringList("privacy-settings.hide-from-leaderboards");
        boolean configChanged = false;

        if (player.isOp()) {
            // Se o jogador é OP e não está na lista, adiciona.
            if (!hiddenUuids.contains(playerUuid)) {
                hiddenUuids.add(playerUuid);
                configChanged = true;
                plugin.logInfo("Admin " + player.getName() + " adicionado à lista de privacidade.");
            }

            // Ativa o /vanish automaticamente para o admin, se configurado.
            if (plugin.getConfig().getBoolean("privacy-settings.auto-vanish-on-join", false)) {
                // Executa o comando com um pequeno atraso (1 tick) para garantir que outros plugins (como o Essentials) já tenham carregado os dados do jogador.
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.performCommand("vanish");
                }, 1L);
            }
        } else {
            // Se o jogador NÃO é OP e está na lista, remove.
            configChanged = hiddenUuids.remove(playerUuid);
            if (configChanged) plugin.logInfo("Ex-admin " + player.getName() + " removido da lista de privacidade.");
        }

        if (configChanged) {
            plugin.getConfig().set("privacy-settings.hide-from-leaderboards", hiddenUuids);
            plugin.saveConfig();
        }
    }
}