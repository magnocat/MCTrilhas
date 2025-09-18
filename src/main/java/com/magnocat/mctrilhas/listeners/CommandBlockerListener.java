package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandBlockerListener implements Listener {

    private final MCTrilhasPlugin plugin;
    private final List<String> allowedCommands;
    private final String blockMessage;
    private final boolean isEnabled;

    public CommandBlockerListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.isEnabled = plugin.getConfig().getBoolean("command-blocker.enabled", false);
        this.allowedCommands = plugin.getConfig().getStringList("command-blocker.allowed-commands");
        this.blockMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("command-blocker.block-message", "&cEste comando não está disponível."));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!isEnabled) {
            return;
        }

        Player player = event.getPlayer();

        // Admins com esta permissão podem usar qualquer comando.
        if (player.hasPermission("mctrilhas.admin.bypass")) {
            return;
        }

        String commandLabel = event.getMessage().substring(1).split(" ")[0].toLowerCase();

        // 1. Verifica se o comando pertence ao nosso plugin (MCTrilhas).
        org.bukkit.command.PluginCommand pluginCommand = plugin.getServer().getPluginCommand(commandLabel);
        if (pluginCommand != null && pluginCommand.getPlugin().equals(plugin)) {
            return; // Permite o comando, pois é do nosso plugin.
        }

        // 2. Verifica se o comando está na lista de comandos externos permitidos (ex: /msg, /tell do Essentials).
        if (allowedCommands.contains(commandLabel)) {
            return; // Permite o comando.
        }

        // 3. Se não for de nenhum dos casos acima, o comando é bloqueado.
        event.setCancelled(true);
        player.sendMessage(blockMessage);
    }
}