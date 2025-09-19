package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PlayerJoinListener implements Listener {

    private final PlayerDataManager playerDataManager;
    private final MCTrilhasPlugin plugin;

    public PlayerJoinListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.playerDataManager = plugin.getPlayerDataManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Carrega os dados do jogador. Essencial para o sistema de insígnias e recompensas.
        // Esta ação é síncrona para garantir que os dados estejam disponíveis imediatamente.
        playerDataManager.loadPlayerData(player.getUniqueId());

        // 2. Verifica se este é o primeiro jogador a entrar no servidor.
        // Se for, inicia as tarefas de atualização de cache que estavam paradas.
        // A verificação é feita com 1 tick de atraso para garantir que a contagem de jogadores esteja correta.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getServer().getOnlinePlayers().size() == 1) {
                plugin.startCacheUpdateTasks();
            }
        }, 1L);

        // 3. Envia a mensagem de boas-vindas com a data do último acesso.
        sendWelcomeMessage(player);

        // 4. Agenda a verificação da recompensa diária para não sobrecarregar o login.
        scheduleDailyRewardNotification(player);

        // 5. Agenda a verificação de promoção de ranque.
        scheduleRankPromotionCheck(player);
    }

    /**
     * Envia uma mensagem de boas-vindas personalizada para o jogador.
     * @param player O jogador que entrou no servidor.
     */
    @SuppressWarnings("deprecation")
    private void sendWelcomeMessage(Player player) {
        // Atraso de 1 segundo (20 ticks) para a mensagem não se perder no meio de outras mensagens de login.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.hasPlayedBefore()) {
                long lastPlayedTimestamp = player.getLastPlayed();
                Date lastPlayedDate = new Date(lastPlayedTimestamp);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm");

                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aBem-vindo(a) de volta, &e" + player.getName() + "&a! Seu último acesso foi em &f" + dateFormat.format(lastPlayedDate) + "&a."));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aSeja bem-vindo(a) pela primeira vez ao servidor, &e" + player.getName() + "&a!"));
            }
        }, 20L);
    }

    /**
     * Agenda uma tarefa para notificar o jogador sobre a recompensa diária, se disponível.
     * @param player O jogador para notificar.
     */
    @SuppressWarnings("deprecation")
    private void scheduleDailyRewardNotification(Player player) {
        // Usamos runTaskLater para garantir que o jogador já esteja totalmente carregado e possa receber mensagens.
        // O atraso maior (5 segundos) é para que esta notificação apareça depois da mensagem de boas-vindas.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ConfigurationSection dailyRewardSection = plugin.getConfig().getConfigurationSection("daily-reward");
            if (dailyRewardSection == null || !dailyRewardSection.getBoolean("enabled", false)) {
                return; // Sistema de recompensa diária desativado ou não configurado
            }

            long lastClaim = playerDataManager.getLastDailyRewardTime(player.getUniqueId());
            long cooldownMillis = TimeUnit.HOURS.toMillis(dailyRewardSection.getInt("cooldown-hours", 24));

            if (System.currentTimeMillis() - lastClaim >= cooldownMillis) {
                String notification = dailyRewardSection.getString("messages.login-notification", "&a[!] Sua recompensa diária está disponível! Use &e/daily&a para coletar.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', notification));
            }
        }, 100L); // Atraso de 5 segundos (100 ticks)
    }

    /**
     * Agenda uma tarefa para verificar se o jogador pode ser promovido.
     * @param player O jogador a ser verificado.
     */
    private void scheduleRankPromotionCheck(Player player) {
        // Atraso de 10 segundos para garantir que todas as estatísticas do jogador estejam carregadas.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getRankManager().checkAndPromote(player);
        }, 200L); // Atraso de 10 segundos (200 ticks)
    }
}