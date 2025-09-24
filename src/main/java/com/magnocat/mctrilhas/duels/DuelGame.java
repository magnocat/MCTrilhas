package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia uma única partida de duelo entre dois jogadores.
 */
public class DuelGame implements Listener {

    private final MCTrilhasPlugin plugin;
    private final DuelArena arena;
    private final Player player1;
    private final Player player2;
    private final DuelManager duelManager;
    private final DuelKit kit;
    private PlayerState player1State;
    private PlayerState player2State;
    private final List<UUID> spectators = new ArrayList<>();
    private final Map<UUID, PlayerState> spectatorStates = new HashMap<>();
    private BukkitTask matchTimerTask;

    private enum GameState {
        STARTING,
        FIGHTING,
        ENDING
    }

    private GameState gameState;

    public DuelGame(MCTrilhasPlugin plugin, DuelArena arena, Player player1, Player player2, DuelKit kit) {
        this.plugin = plugin;
        this.arena = arena;
        this.player1 = player1;
        this.player2 = player2;
        this.duelManager = plugin.getDuelManager();
        this.kit = kit;
        this.gameState = GameState.STARTING;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startCountdown();
    }

    private void startCountdown() {
        // Salva o estado atual dos jogadores antes de modificar qualquer coisa.
        this.player1State = new PlayerState(player1);
        this.player2State = new PlayerState(player2);

        // Limpa o inventário e aplica o kit de duelo.

        kit.apply(player1);
        kit.apply(player2);

        player1.teleport(arena.getPos1());
        player2.teleport(arena.getPos2());

        FileConfiguration config = plugin.getConfig();
        int countdownSeconds = config.getInt("duel-settings.countdown-seconds", 5);
        String countdownMessage = ChatColor.translateAlternateColorCodes('&', config.getString("duel-settings.messages.countdown", "&aO duelo começa em &e{time}&a..."));
        String fightMessage = ChatColor.translateAlternateColorCodes('&', config.getString("duel-settings.messages.fight", "&c&lLutem!"));
        int matchDurationMinutes = config.getInt("duel-settings.match-duration-minutes", 5);

        new BukkitRunnable() {
            int countdown = countdownSeconds;
            @Override
            public void run() {
                if (countdown > 0) {
                    String message = countdownMessage.replace("{time}", String.valueOf(countdown));
                    player1.sendTitle(message, "", 0, 22, 0);
                    player2.sendTitle(message, "", 0, 22, 0);
                    countdown--;
                } else {
                    player1.sendTitle(fightMessage, "", 0, 40, 10);
                    player2.sendTitle(fightMessage, "", 0, 40, 10);
                    gameState = GameState.FIGHTING;

                    // Inicia o timer da partida, se houver um
                    if (matchDurationMinutes > 0) {
                        matchTimerTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                endAsDraw(config.getString("duel-settings.messages.draw", "&eO tempo acabou! O duelo terminou em empate."), null);
                            }
                        }.runTaskLater(plugin, matchDurationMinutes * 60 * 20L);
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void endGame(Player winner, Player loser) {
        if (gameState == GameState.ENDING) return;
        gameState = GameState.ENDING;
        cancelMatchTimer();

        // Registra que houve uma partida de duelo válida (com vencedor) nesta semana.
        plugin.getDuelRewardManager().recordDuelActivity();

        // --- LÓGICA DE ELO ---
        PlayerDuelStats winnerStats = plugin.getPlayerDataManager().getPlayerDuelStats(winner.getUniqueId());
        PlayerDuelStats loserStats = plugin.getPlayerDataManager().getPlayerDuelStats(loser.getUniqueId());

        int oldWinnerElo = winnerStats.getElo();
        int oldLoserElo = loserStats.getElo();

        int[] newRatings = EloCalculator.calculateNewRatings(oldWinnerElo, oldLoserElo);
        winnerStats.setElo(newRatings[0]);
        loserStats.setElo(newRatings[1]);

        winnerStats.incrementWins();
        loserStats.incrementLosses();

        plugin.getPlayerDataManager().savePlayerDuelStats(winner.getUniqueId(), winnerStats);
        plugin.getPlayerDataManager().savePlayerDuelStats(loser.getUniqueId(), loserStats);

        int winnerEloChange = winnerStats.getElo() - oldWinnerElo;
        int loserEloChange = loserStats.getElo() - oldLoserElo;

        FileConfiguration config = plugin.getConfig();
        String winMessage = ChatColor.translateAlternateColorCodes('&', config.getString("duel-settings.messages.win", "&aVocê venceu o duelo contra &e{loser}&a!").replace("{loser}", loser.getName()))
                + String.format(" %s(ELO: %d %s%d)", ChatColor.GRAY, winnerStats.getElo(), (winnerEloChange >= 0 ? ChatColor.GREEN + "+" : ChatColor.RED), winnerEloChange);
        String lossMessage = ChatColor.translateAlternateColorCodes('&', config.getString("duel-settings.messages.loss", "&cVocê foi derrotado por &e{winner}&c.").replace("{winner}", winner.getName()))
                + String.format(" %s(ELO: %d %s%d)", ChatColor.GRAY, loserStats.getElo(), (loserEloChange >= 0 ? ChatColor.GREEN + "+" : ChatColor.RED), loserEloChange);

        winner.sendMessage(winMessage);
        loser.sendMessage(lossMessage);

        cleanupGame();
    }

    /**
     * Termina o jogo como um empate, sem registrar vitória ou derrota.
     * @param reason A razão do empate (ex: tempo esgotado, desistência).
     * @param forfeiter O jogador que desistiu, ou null se for por tempo.
     */
    public void endAsDraw(String reason, Player forfeiter) {
        if (gameState == GameState.ENDING) return;
        gameState = GameState.ENDING;
        cancelMatchTimer();

        String finalReason = ChatColor.translateAlternateColorCodes('&', reason);
        if (forfeiter != null) {
            finalReason = finalReason.replace("{player}", forfeiter.getName());
        }

        if (player1 != null && player1.isOnline()) player1.sendMessage(finalReason);
        if (player2 != null && player2.isOnline()) player2.sendMessage(finalReason);

        cleanupGame();
    }

    private void cleanupGame() {
        // Atraso para o jogador ver a mensagem de morte antes de ser teleportado
        new BukkitRunnable() {
            @Override
            public void run() {
                // Restaura o estado original dos jogadores e os teleporta para o hub.
                if (player1 != null && player1.isOnline()) {
                    player1State.restore(player1);
                    player1.teleport(player1State.getLocation());
                }
                if (player2 != null && player2.isOnline()) {
                    player2State.restore(player2);
                    player2.teleport(player2State.getLocation());
                }

                // Restaura o estado de todos os espectadores e os teleporta para o hub.
                // Itera sobre uma cópia para evitar ConcurrentModificationException.
                for (UUID spectatorUUID : new ArrayList<>(spectators)) {
                    Player spectator = Bukkit.getPlayer(spectatorUUID);
                    if (spectator != null && spectator.isOnline()) {
                        PlayerState spectatorState = spectatorStates.get(spectatorUUID);
                        if (spectatorState != null) {
                            spectatorState.restore(spectator);
                        }
                        spectator.teleport(spectatorState.getLocation());
                    }
                }

                duelManager.endDuel(DuelGame.this);
                HandlerList.unregisterAll(DuelGame.this); // De-registra os listeners deste jogo.
            }
        }.runTaskLater(plugin, 60L); // 3 segundos de atraso
    }

    private void cancelMatchTimer() {
        if (matchTimerTask != null && !matchTimerTask.isCancelled()) {
            matchTimerTask.cancel();
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntity().equals(player1) || event.getEntity().equals(player2)) {
            Player loser = event.getEntity();
            Player winner = loser.equals(player1) ? player2 : player1;
            String broadcastMessage = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("duel-settings.messages.broadcast-end", "&e{winner} &7venceu o duelo contra &e{loser}&7."))
                    .replace("{winner}", winner.getName()).replace("{loser}", loser.getName());
            event.setDeathMessage(broadcastMessage);
            endGame(winner, loser);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (gameState == GameState.ENDING) return;

        // Se um dos duelistas desconecta
        if (event.getPlayer().equals(player1) || event.getPlayer().equals(player2)) {
            Player loser = event.getPlayer();
            Player winner = loser.equals(player1) ? player2 : player1;
            String forfeitMessage = plugin.getConfig().getString("duel-settings.messages.forfeit", "&e{player} &7desistiu do duelo. A partida terminou em empate.");
            // Um quit é tratado como um empate por desistência.
            endAsDraw(forfeitMessage, loser);
            return;
        }

        // Se um espectador desconecta
        UUID quitterUUID = event.getPlayer().getUniqueId();
        if (spectators.contains(quitterUUID)) {
            spectators.remove(quitterUUID);
            spectatorStates.remove(quitterUUID);
            duelManager.removeSpectatorFromMap(quitterUUID);
        }
    }

    public void addSpectator(Player spectator) {
        if (isParticipant(spectator) || spectators.contains(spectator.getUniqueId())) {
            spectator.sendMessage(ChatColor.RED + "Você já está envolvido neste duelo.");
            return;
        }

        spectatorStates.put(spectator.getUniqueId(), new PlayerState(spectator));
        spectators.add(spectator.getUniqueId());
        duelManager.addSpectatorToMap(spectator.getUniqueId(), this);

        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.teleport(arena.getSpectatorSpawn());
        spectator.sendMessage(ChatColor.GREEN + "Você está assistindo ao duelo entre " + player1.getName() + " e " + player2.getName() + ".");
        spectator.sendMessage(ChatColor.YELLOW + "Use /duelo sair para parar de assistir.");
    }

    public void removeSpectator(Player spectator) {
        UUID spectatorUUID = spectator.getUniqueId();
        PlayerState state = spectatorStates.remove(spectatorUUID);
        if (state != null) {
            state.restore(spectator);
            spectators.remove(spectatorUUID);
            duelManager.removeSpectatorFromMap(spectatorUUID);
            plugin.teleportToHub(spectator);
            spectator.sendMessage(ChatColor.YELLOW + "Você parou de assistir ao duelo.");
        }
    }

    public boolean isParticipant(Player player) { return player.equals(player1) || player.equals(player2); }
    public DuelArena getArena() { return arena; }
    public Player getPlayer1() { return player1; }
    public Player getPlayer2() { return player2; }
}