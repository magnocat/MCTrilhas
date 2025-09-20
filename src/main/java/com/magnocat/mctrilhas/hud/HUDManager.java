package com.magnocat.mctrilhas.hud;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HUDManager {

    private final MCTrilhasPlugin plugin;
    private final Map<UUID, BossBar> activeHUDs = new ConcurrentHashMap<>();
    private BukkitTask updaterTask;

    public HUDManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        startUpdater();
    }

    public void toggleHUD(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (activeHUDs.containsKey(playerUUID)) {
            removeHUD(player);
            player.sendMessage("§aHUD de estatísticas desativado.");
        } else {
            showHUD(player);
            player.sendMessage("§aHUD de estatísticas ativado.");
        }
    }

    private void showHUD(Player player) {
        UUID playerUUID = player.getUniqueId();
        BossBar bossBar = Bukkit.createBossBar("Carregando estatísticas...", BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        activeHUDs.put(playerUUID, bossBar);
        updateHUD(player); // Atualiza imediatamente
    }

    private void removeHUD(Player player) {
        UUID playerUUID = player.getUniqueId();
        BossBar bossBar = activeHUDs.remove(playerUUID);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void cleanupOnQuit(Player player) {
        removeHUD(player);
    }

    private void startUpdater() {
        this.updaterTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activeHUDs.keySet()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        updateHUD(player);
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 40L); // Atualiza a cada 2 segundos
    }

    private void updateHUD(Player player) {
        BossBar bossBar = activeHUDs.get(player.getUniqueId());
        if (bossBar == null) return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        Rank rank = playerData.getRank();
        double balance = (plugin.getEconomy() != null) ? plugin.getEconomy().getBalance(player) : 0;
        int badges = playerData.getEarnedBadgesMap().size();

        String hudText = String.format(
            "§bRanque: §e%s §8| §bTotens: §e%,.0f §8| §bInsígnias: §e%d",
            rank.getDisplayName(),
            balance,
            badges
        );

        // Usa runTask para garantir que a modificação da BossBar ocorra na thread principal
        new BukkitRunnable() {
            @Override
            public void run() {
                bossBar.setTitle(hudText);
            }
        }.runTask(plugin);
    }

    public void stop() {
        if (updaterTask != null) {
            updaterTask.cancel();
        }
    }
}