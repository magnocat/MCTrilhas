package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.UUID;

/**
 * Listener para aplicar penalidades a padrinhos quando seus afilhados são banidos.
 */
public class PunishmentListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public PunishmentListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        // A forma moderna de detectar um ban é verificar o motivo do kick.
        // Usamos PlayerKickEvent.Cause.BANNED que é a causa específica para banimentos.
        if (event.getCause() != PlayerKickEvent.Cause.BANNED) {
            return; // Ignora o evento se não for um banimento.
        }

        // Executa a lógica de forma assíncrona para não atrasar o evento de banimento.
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID bannedPlayerUUID = event.getPlayer().getUniqueId();

            // Carrega os dados do jogador banido de forma segura (offline).
            PlayerData bannedPlayerData = plugin.getPlayerDataManager().loadOfflinePlayerData(bannedPlayerUUID);

            if (bannedPlayerData == null || bannedPlayerData.getGodfatherUUID() == null) {
                return; // O jogador não tem dados ou não foi apadrinhado.
            }

            UUID godfatherUUID = bannedPlayerData.getGodfatherUUID();
            OfflinePlayer godfatherOffline = Bukkit.getOfflinePlayer(godfatherUUID);

            // Carrega os dados do padrinho (pode estar online ou offline).
            PlayerData godfatherData = plugin.getPlayerDataManager().getPlayerData(godfatherUUID);
            if (godfatherData == null) {
                godfatherData = plugin.getPlayerDataManager().loadOfflinePlayerData(godfatherUUID);
            }

            if (godfatherData == null) {
                plugin.logWarn("Não foi possível carregar os dados do padrinho " + godfatherUUID + " para aplicar a penalidade.");
                return;
            }

            // Aplica a penalidade
            double penaltyAmount = plugin.getConfig().getDouble("sponsorship-penalty.totem-amount", 5000.0);
            if (plugin.getEconomy() != null && plugin.getEconomy().isEnabled()) { // Verifica se a economia está ativa
                plugin.getEconomy().withdrawPlayer(godfatherOffline, penaltyAmount);
            }

            // Salva os dados do padrinho (se ele estiver offline, isso garante que a penalidade seja aplicada).
            // Se estiver online, os dados serão salvos no logout, mas salvar aqui é uma garantia extra.
            plugin.getPlayerDataManager().savePlayerData(godfatherData);

            // Notifica o padrinho se ele estiver online.
            Player godfatherOnline = godfatherOffline.getPlayer();
            if (godfatherOnline != null && godfatherOnline.isOnline()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    godfatherOnline.sendMessage(ChatColor.RED + "§lAVISO DE RESPONSABILIDADE");
                    godfatherOnline.sendMessage(ChatColor.YELLOW + "Seu afilhado, " + event.getPlayer().getName() + ", foi banido do servidor.");
                    godfatherOnline.sendMessage(ChatColor.YELLOW + "Como seu padrinho, você foi penalizado em " + ChatColor.RED + (int) penaltyAmount + " Totens.");
                    godfatherOnline.sendMessage(ChatColor.GRAY + "Lembre-se da responsabilidade de guiar novos membros.");
                });
            }
        });
    }
}