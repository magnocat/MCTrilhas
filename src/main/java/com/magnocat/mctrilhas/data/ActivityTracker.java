package com.magnocat.mctrilhas.data;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Rastreia o tempo de jogo ativo dos jogadores, ignorando o tempo AFK.
 */
public class ActivityTracker {

    private final MCTrilhasPlugin plugin;
    private static final long CHECK_INTERVAL_TICKS = 1200L; // 60 segundos (60 * 20 ticks)

    public ActivityTracker(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void schedule() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkPlayerActivity(player);
                }
            }
        }.runTaskTimer(plugin, 0L, CHECK_INTERVAL_TICKS);
    }

    private void checkPlayerActivity(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return;
        }

        Location currentLocation = player.getLocation();
        Location lastLocation = data.getLastAfkCheckLocation();

        // Se o jogador não tiver uma última localização ou se ele se moveu, ele está ativo.
        if (lastLocation == null || !locationsAreSimilar(currentLocation, lastLocation)) {
            data.addActivePlaytimeTicks(CHECK_INTERVAL_TICKS);
        }

        // Atualiza a última localização para a próxima verificação.
        data.setLastAfkCheckLocation(currentLocation);
    }

    /**
     * Compara duas localizações para ver se o jogador se moveu significativamente.
     * Ignora pequenas mudanças de rotação da cabeça (pitch/yaw).
     * @return true se as localizações são no mesmo bloco, false caso contrário.
     */
    private boolean locationsAreSimilar(Location loc1, Location loc2) {
        if (loc1.getWorld() != loc2.getWorld()) return false;
        return loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }
}