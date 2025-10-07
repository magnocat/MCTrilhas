package com.magnocat.mctrilhas.duels;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.utils.PlayerStateManager;

/**
 * Representa uma única partida de duelo em andamento.
 */
public class DuelGame {

    private final MCTrilhasPlugin plugin;
    private final DuelArena arena;
    private final Player player1;
    private final Player player2;
    private final DuelKit kit;
    private final List<Player> spectators = new ArrayList<>();

    private final PlayerStateManager stateManager;
    private GameState state = GameState.STARTING;
    private BukkitTask countdownTask;
    private BukkitTask matchTimerTask;
    private BossBar timerBossBar;
    private int remainingTimeSeconds;

    public DuelGame(MCTrilhasPlugin plugin, DuelArena arena, Player player1, Player player2, DuelKit kit) {
        this.plugin = plugin;
        this.arena = arena;
        this.player1 = player1;
        this.player2 = player2;
        this.kit = kit;
        this.stateManager = new PlayerStateManager();
        start();
    }

    private void start() {
        // 1. Salvar estado dos jogadores
        stateManager.saveState(player1);
        stateManager.saveState(player2);

        // 2. Preparar jogadores
        preparePlayer(player1);
        preparePlayer(player2);

        // 3. Teleportar para a arena
        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());

        // 4. Iniciar contagem regressiva
        startCountdown();
    }

    private void preparePlayer(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
        player.setGameMode(GameMode.ADVENTURE); // Previne que quebrem a arena
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }

    private void startCountdown() {
        int countdownSeconds = plugin.getConfig().getInt("duel-settings.countdown-seconds", 5);
        String countdownMessage = plugin.getConfig().getString("duel-settings.messages.countdown", "&aO duelo começa em &e{time}&a...");

        this.countdownTask = new BukkitRunnable() {
            private int time = countdownSeconds;

            @Override
            public void run() {
                if (time > 0) {
                    String message = ChatColor.translateAlternateColorCodes('&', countdownMessage.replace("{time}", String.valueOf(time)));
                    player1.sendTitle(message, "", 0, 25, 0);
                    player2.sendTitle(message, "", 0, 25, 0);
                    time--;
                } else {
                    // A luta começa!
                    state = GameState.FIGHTING;
                    String fightMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("duel-settings.messages.fight", "&c&lLutem!"));
                    player1.sendTitle(fightMessage, "", 0, 40, 10);
                    player2.sendTitle(fightMessage, "", 0, 40, 10);

                    // Aplica o kit e permite o combate
                    kit.applyTo(player1);
                    kit.applyTo(player2);
                    player1.setGameMode(GameMode.SURVIVAL);
                    player2.setGameMode(GameMode.SURVIVAL);

                    // Inicia o timer da partida, se configurado
                    startMatchTimer();

                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void end(Player winner, Player loser) {
        if (state != GameState.FIGHTING) return;
        state = GameState.ENDING;

        if (matchTimerTask != null) matchTimerTask.cancel();
        if (countdownTask != null) countdownTask.cancel();

        // Notificações
        String winMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("duel-settings.messages.win", "&aVocê venceu o duelo contra &e{loser}&a!"));
        String lossMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("duel-settings.messages.loss", "&cVocê foi derrotado por &e{winner}&c."));
        String broadcastMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("duel-settings.messages.broadcast-end", "&e{winner} &7venceu o duelo contra &e{loser}&7."));

        winner.sendMessage(winMessage.replace("{loser}", loser.getName()));
        loser.sendMessage(lossMessage.replace("{winner}", winner.getName()));

        Bukkit.broadcastMessage(broadcastMessage.replace("{winner}", winner.getName()).replace("{loser}", loser.getName()));

        // Lógica de ELO
        updateElo(winner, loser);

        // Registra que houve uma atividade de duelo para as recompensas semanais
        plugin.getDuelRewardManager().recordDuelActivity();

        // Atraso para o jogador poder ver a tela de morte antes de ser teleportado
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskLater(plugin, 60L); // 3 segundos de atraso
    }

    public void endAsDraw(String reason, Player initiator) {
        if (state == GameState.ENDING) return;
        state = GameState.ENDING;

        if (matchTimerTask != null) matchTimerTask.cancel();
        if (countdownTask != null) countdownTask.cancel();

        String formattedReason = ChatColor.translateAlternateColorCodes('&', reason.replace("{player}", initiator.getName()));
        player1.sendMessage(formattedReason);
        player2.sendMessage(formattedReason);

        cleanup();
    }

    private void updateElo(Player winner, Player loser) {
        PlayerData winnerData = plugin.getPlayerDataManager().getPlayerData(winner.getUniqueId());
        PlayerData loserData = plugin.getPlayerDataManager().getPlayerData(loser.getUniqueId());

        if (winnerData == null || loserData == null) return;

        PlayerDuelStats winnerStats = winnerData.getDuelStats();
        PlayerDuelStats loserStats = loserData.getDuelStats();

        int eloChange = EloCalculator.calculateEloChange(winnerStats.getElo(), loserStats.getElo());

        winnerStats.incrementWins();
        winnerStats.addElo(eloChange);

        loserStats.incrementLosses();
        loserStats.addElo(-eloChange);

        winner.sendMessage(ChatColor.GREEN + "ELO: " + winnerStats.getElo() + " (+" + eloChange + ")");
        loser.sendMessage(ChatColor.RED + "ELO: " + loserStats.getElo() + " (-" + eloChange + ")");

        // Adiciona a recompensa de 5 Totens para o vencedor
        if (plugin.getEconomy() != null) {
            double winReward = 5.0; // Valor fixo como discutido
            plugin.getEconomy().depositPlayer(winner, winReward);
            winner.sendMessage(ChatColor.GOLD + "+ " + (int)winReward + " Totens pela vitória!");
        } else {
            plugin.logWarn("Vault não encontrado. Recompensa de duelo para " + winner.getName() + " não pôde ser entregue.");
        }

        // Salva os dados
        plugin.getPlayerDataManager().savePlayerData(winnerData);
        plugin.getPlayerDataManager().savePlayerData(loserData);

        // Otimização: Atualiza os caches de ranking da API sob demanda, apenas quando uma partida termina.
        // Isso garante que o site sempre tenha os dados mais recentes.
        if (plugin.getHttpApiManager() != null) {
            plugin.getHttpApiManager().updateAllLeaderboardCaches();
        }
    }

    private void cleanup() {
        // Restaura estado dos jogadores
        stateManager.restoreState(player1);
        stateManager.restoreState(player2);

        // Restaura a HUD geral para os jogadores, se eles a tinham ativa.
        plugin.getHudManager().restoreHUD(player1);
        plugin.getHudManager().restoreHUD(player2);

        // Remove espectadores
        new ArrayList<>(spectators).forEach(this::removeSpectator);

        // Remove a BossBar do timer
        if (timerBossBar != null) {
            timerBossBar.removeAll();
            timerBossBar = null;
        }

        // Notifica o DuelManager para limpar o jogo
        plugin.getDuelManager().endDuel(this);
    }

    // --- Getters e Métodos de Utilidade ---
    public boolean isParticipant(Player player) {
        return player.equals(player1) || player.equals(player2);
    }

    public Player getOpponent(Player player) {
        if (player.equals(player1)) return player2;
        if (player.equals(player2)) return player1;
        return null;
    }

    public void addSpectator(Player spectator) {
        if (isParticipant(spectator) || spectators.contains(spectator)) {
            spectator.sendMessage(ChatColor.RED + "Você já está envolvido neste duelo.");
            return;
        }

        stateManager.saveState(spectator);
        spectators.add(spectator);
        plugin.getDuelManager().addSpectatorToMap(spectator.getUniqueId(), this);

        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.teleport(arena.getSpectatorSpawn());
        spectator.sendMessage(ChatColor.GREEN + "Você está assistindo ao duelo entre " + player1.getName() + " e " + player2.getName() + ".");
        spectator.sendMessage(ChatColor.GRAY + "Use /duelo sair para parar de assistir.");
    }

    public void removeSpectator(Player spectator) {
        if (!spectators.remove(spectator)) {
            return; // Não era um espectador deste jogo
        }

        stateManager.restoreState(spectator);
        plugin.getDuelManager().removeSpectatorFromMap(spectator.getUniqueId());

        spectator.sendMessage(ChatColor.YELLOW + "Você parou de assistir ao duelo.");
        // O restoreState já lida com o teleporte de volta.
    }

    private void startMatchTimer() {
        int matchDurationMinutes = plugin.getConfig().getInt("duel-settings.match-duration-minutes", 5);
        if (matchDurationMinutes <= 0) {
            return; // Timer desativado
        }

        this.remainingTimeSeconds = matchDurationMinutes * 60;
        this.timerBossBar = Bukkit.createBossBar("Tempo Restante: " + formatTime(remainingTimeSeconds), BarColor.BLUE, BarStyle.SOLID);
        this.timerBossBar.addPlayer(player1);
        this.timerBossBar.addPlayer(player2);

        // Esconde a HUD geral dos jogadores para evitar sobreposição de BossBars.
        // Ela será restaurada no método cleanup().
        plugin.getHudManager().hideHUD(player1);
        plugin.getHudManager().hideHUD(player2);

        this.matchTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (remainingTimeSeconds > 0) {
                    remainingTimeSeconds--;
                    timerBossBar.setTitle("Tempo Restante: " + formatTime(remainingTimeSeconds));
                    timerBossBar.setProgress((double) remainingTimeSeconds / (matchDurationMinutes * 60));

                    // Muda a cor da barra nos últimos segundos para criar tensão
                    if (remainingTimeSeconds <= 10) {
                        timerBossBar.setColor(BarColor.RED);
                    } else if (remainingTimeSeconds <= 30) {
                        timerBossBar.setColor(BarColor.YELLOW);
                    }
                } else {
                    // O tempo acabou!
                    String drawMessage = plugin.getConfig().getString("duel-settings.messages.draw", "&eO tempo acabou! O duelo terminou em empate.");
                    endAsDraw(drawMessage, player1); // O iniciador aqui é irrelevante
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
    public DuelArena getArena() { return arena; }
    public GameState getState() { return state; }

    public enum GameState {
        STARTING,
        FIGHTING,
        ENDING
    }
}