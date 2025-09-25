package com.magnocat.mctrilhas.hud;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.duels.PlayerDuelStats;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.pet.PetData;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o sistema de HUD (Heads-Up Display) que exibe informações
 * aos jogadores através de uma BossBar.
 */
public class HUDManager {

    private final MCTrilhasPlugin plugin;
    private final Map<UUID, BossBar> activeHUDs = new ConcurrentHashMap<>();
    private BukkitTask updaterTask;

    /**
     * Construtor do gerenciador de HUD.
     * @param plugin A instância principal do plugin.
     */
    public HUDManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        startUpdater();
    }

    /**
     * Ativa ou desativa o HUD para um jogador.
     * @param player O jogador para o qual o HUD será alternado.
     */
    public void toggleHUD(Player player) {
        UUID playerUUID = player.getUniqueId();
        if (activeHUDs.containsKey(playerUUID)) {
            removeHUD(player);
            String message = plugin.getConfig().getString("hud-settings.messages.disabled", "&aHUD de estatísticas desativado.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        } else {
            showHUD(player);
            String message = plugin.getConfig().getString("hud-settings.messages.enabled", "&aHUD de estatísticas ativado.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    /**
     * Mostra o HUD para um jogador, criando e configurando a BossBar.
     * @param player O jogador que verá o HUD.
     */
    private void showHUD(Player player) {
        UUID playerUUID = player.getUniqueId();
        String loadingText = plugin.getConfig().getString("hud-settings.messages.loading", "Carregando estatísticas...");
        BarColor color = BarColor.valueOf(plugin.getConfig().getString("hud-settings.color", "BLUE").toUpperCase());
        BarStyle style = BarStyle.valueOf(plugin.getConfig().getString("hud-settings.style", "SOLID").toUpperCase());

        BossBar bossBar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', loadingText), color, style);
        bossBar.addPlayer(player);
        activeHUDs.put(playerUUID, bossBar);
        updateHUD(player); // Atualiza imediatamente
    }

    /**
     * Remove o HUD de um jogador.
     * @param player O jogador cujo HUD será removido.
     */
    private void removeHUD(Player player) {
        UUID playerUUID = player.getUniqueId();
        BossBar bossBar = activeHUDs.remove(playerUUID);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    /**
     * Limpa o HUD de um jogador quando ele sai do servidor para evitar memory leaks.
     * @param player O jogador que saiu.
     */
    public void cleanupOnQuit(Player player) {
        removeHUD(player);
    }

    /**
     * Inicia a tarefa assíncrona que atualiza todos os HUDs ativos periodicamente.
     */
    private void startUpdater() {
        long updateInterval = plugin.getConfig().getLong("hud-settings.update-interval-ticks", 40L);
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
        }.runTaskTimerAsynchronously(plugin, 0L, updateInterval);
    }

    /**
     * Atualiza o texto e o progresso do HUD de um jogador específico.
     * @param player O jogador cujo HUD será atualizado.
     */
    private void updateHUD(Player player) {
        BossBar bossBar = activeHUDs.get(player.getUniqueId());
        if (bossBar == null) return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        PlayerDuelStats duelStats = plugin.getPlayerDataManager().getPlayerDuelStats(player.getUniqueId());
        int elo = (duelStats != null) ? duelStats.getElo() : PlayerDuelStats.DEFAULT_ELO;

        // --- Lógica do Pet ---
        PetData petData = playerData.getPetData();
        String petInfo;
        String petHappinessInfo;
        double petXpProgress = 0.0;

        if (petData != null && petData.isOwned()) {
            int currentXp = (int) petData.getExperience();
            int nextLevelXp = petData.getExperienceToNextLevel();
            petInfo = String.format("%s Lvl %d", petData.getName(), petData.getLevel());
            if (petData.getLevel() < PetData.MAX_LEVEL && nextLevelXp > 0) {
                petXpProgress = Math.max(0.0, Math.min(1.0, (double) currentXp / nextLevelXp));
            } else {
                petXpProgress = 1.0; // Barra cheia no nível máximo
            }
            petHappinessInfo = String.format("%.0f%%", petData.getHappiness());
        } else {
            petInfo = "Nenhum";
            petHappinessInfo = "N/A";
        }

        Rank rank = playerData.getRank();
        double balance = (plugin.getEconomy() != null) ? plugin.getEconomy().getBalance(player) : 0;

        final String hudFormat = plugin.getConfig().getString("hud-settings.format", "&bRanque: &e{rank} &8| &bELO: &e{elo} &8| &bTotens: &e{totems} &8| &dPet: &e{pet_info} &7- &dFelicidade: &e{pet_felicidade}");

        final String hudText = hudFormat
                .replace("{rank}", rank.getDisplayName())
                .replace("{elo}", String.valueOf(elo))
                .replace("{totems}", String.format("%,.0f", balance))
                .replace("{pet_info}", petInfo)
                .replace("{pet_felicidade}", petHappinessInfo);

        final double finalPetXpProgress = petXpProgress;
        // Usa runTask para garantir que a modificação da BossBar ocorra na thread principal
        new BukkitRunnable() {
            @Override
            public void run() {
                bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', hudText));
                bossBar.setProgress(finalPetXpProgress);
            }
        }.runTask(plugin);
    }

    /**
     * Para a tarefa de atualização do HUD e limpa todas as BossBars ativas.
     * Chamado no onDisable do plugin para evitar barras "fantasmas" após um /reload.
     */
    public void stop() {
        if (updaterTask != null) {
            updaterTask.cancel();
        }
        // Limpa todas as boss bars ativas para evitar que fiquem na tela após um /reload
        for (BossBar bossBar : activeHUDs.values()) {
            bossBar.removeAll();
        }
        activeHUDs.clear();
    }
}