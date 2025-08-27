package com.magnocat.godmode.listeners;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.concurrent.TimeUnit;

public class PlayerJoinListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final GodModePlugin plugin;

    public PlayerJoinListener(GodModePlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Notificação de recompensa diária
        // Usamos runTaskLater para garantir que o jogador já esteja totalmente carregado e possa receber mensagens.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ConfigurationSection dailyRewardSection = plugin.getConfig().getConfigurationSection("daily-reward");
            if (dailyRewardSection == null || !dailyRewardSection.getBoolean("enabled", false)) {
                return; // Sistema de recompensa diária desativado ou não configurado
            }

            long lastClaim = playerDataManager.getLastDailyRewardTime(player.getUniqueId());
            if (System.currentTimeMillis() - lastClaim >= TimeUnit.HOURS.toMillis(24)) {
                String notification = dailyRewardSection.getString("messages.login-notification", "&a[!] Sua recompensa diária está disponível! Use &e/daily&a para coletar.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', notification));
            }
        }, 100L); // Atraso de 5 segundos (100 ticks) para garantir que o jogador esteja pronto
    }
}