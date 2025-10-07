package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerState;
import com.magnocat.mctrilhas.data.PlayerCTFStats;
import com.magnocat.mctrilhas.utils.ItemFactory;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.entity.Firework;
import org.bukkit.FireworkEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Representa uma única partida de Capture The Flag em andamento.
 */
public class CTFGame {

    private final MCTrilhasPlugin plugin;
    private final CTFArena arena;
    private GameState gameState;

    private final Map<TeamColor, CTFTeam> teams = new HashMap<>();
    private final Map<UUID, TeamColor> playerTeams = new HashMap<>();
    private final Set<UUID> originalParticipants = new HashSet<>(); // Novo: Guarda todos que iniciaram a partida
    private final Map<TeamColor, CTFFlag> flags = new HashMap<>();
    private final Map<UUID, PlayerState> originalPlayerStates = new HashMap<>();
    private final Map<UUID, ItemStack> temporaryHelmets = new HashMap<>();
    private final Map<UUID, CTFPlayerStats> playerStats = new HashMap<>();

    private final CTFScoreboard scoreboard;
    // Guarda qual cor foi associada a qual "slot" da arena (ex: "red" vs "blue" spawns)
    private TeamColor teamOne; // Associado aos spawns "red" da arena
    private TeamColor teamTwo; // Associado aos spawns "blue" da arena

    private BukkitTask gameTimer;
    private int timeRemaining;

    public CTFGame(MCTrilhasPlugin plugin, CTFArena arena, List<Player> players) {
        this.plugin = plugin;
        this.arena = arena;
        this.gameState = GameState.WAITING;
        this.timeRemaining = arena.getGameDurationSeconds();
        this.scoreboard = new CTFScoreboard(this);

        setupGame(players);

        startCountdown();
    }

    /**
     * Configura os times com cores aleatórias, distribui os jogadores e prepara
     * as bandeiras.
     *
     * @param players A lista de jogadores que participarão da partida.
     */
    private void setupGame(List<Player> players) {
        // 1. Escolhe duas cores aleatórias para os times
        List<TeamColor> gameColors = TeamColor.getTwoRandomTeams();
        this.teamOne = gameColors.get(0);
        this.teamTwo = gameColors.get(1);

        // 2. Cria os times e distribui os jogadores
        teams.put(teamOne, new CTFTeam(teamOne));
        teams.put(teamTwo, new CTFTeam(teamTwo));

        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            TeamColor assignedTeam = (i % 2 == 0) ? teamOne : teamTwo;
            teams.get(assignedTeam).addPlayer(player.getUniqueId());
            playerTeams.put(player.getUniqueId(), assignedTeam);
            originalParticipants.add(player.getUniqueId()); // Adiciona à lista de participantes originais
            playerStats.put(player.getUniqueId(), new CTFPlayerStats());
        }

        // 3. Configura as bandeiras associando-as aos spawns da arena
        flags.put(teamOne, new CTFFlag(plugin, this, teamOne, arena.getRedFlagLocation()));
        flags.put(teamTwo, new CTFFlag(plugin, this, teamTwo, arena.getBlueFlagLocation()));
    }

    /**
     * Prepara os jogadores para a partida, teleportando-os e salvando seu
     * estado.
     */
    private void preparePlayers() {
        for (Map.Entry<UUID, TeamColor> entry : playerTeams.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }

            savePlayerState(player);
            clearPlayer(player);

            TeamColor teamColor = entry.getValue();
            // Teleporta o jogador para o spawn correspondente ao seu time
            if (teamColor == teamOne) {
                player.teleport(arena.getRedSpawn());
            } else {
                player.teleport(arena.getBlueSpawn());
            }

            player.setGameMode(GameMode.SURVIVAL);
            scoreboard.createPlayerScoreboard(player);
        }
    }

    /**
     * Inicia a contagem regressiva antes do início da partida.
     */
    private void startCountdown() {
        this.gameState = GameState.STARTING;
        preparePlayers();

        new org.bukkit.scheduler.BukkitRunnable() {
            int countdown = arena.getCountdownSeconds();

            @Override
            public void run() {
                if (countdown > 0) {
                    String titleColor = (countdown <= 3) ? ChatColor.RED.toString() : ChatColor.YELLOW.toString();
                    broadcastTitle(titleColor + countdown, "", 0, 25, 5);
                    playSoundForAll(Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    countdown--;
                } else {
                    broadcastTitle(ChatColor.GREEN + "COMEÇOU!", "", 0, 40, 10);
                    playSoundForAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
                    startGame();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Inicia efetivamente a partida após a contagem regressiva.
     */
    public void startGame() {
        this.gameState = GameState.IN_PROGRESS;

        // Aplica os kits para todos os jogadores
        for (UUID playerUUID : playerTeams.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                applyKit(player);
            }
        }

        // Atualiza o scoreboard para todos após a configuração inicial
        scoreboard.updateForAllPlayers();

        // Inicia o timer da partida
        this.gameTimer = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.IN_PROGRESS) {
                    cancel();
                    return;
                }

                timeRemaining--;
                scoreboard.updateForAllPlayers();

                if (timeRemaining <= 0) {
                    cancel();
                    endGameByTime();
                } else if (timeRemaining <= 10 && timeRemaining > 0) {
                    // Anuncia os últimos 10 segundos
                    broadcastMessage(ChatColor.YELLOW + "A partida termina em " + timeRemaining + " segundo(s)!");
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void endGame(TeamColor winner, boolean isForfeit) {
        if (gameState == GameState.ENDING) {
            return; // Evita chamadas duplas

                }this.gameState = GameState.ENDING;
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        // Limpa os blocos de bandeira do mundo
        for (CTFFlag flag : flags.values()) {
            flag.removeFlagBlock();
        }

        // Constrói e transmite o resumo de fim de jogo ANTES de restaurar os jogadores
        broadcastEndGameSummary(winner);

        double rewardAmount;
        String formattedRewardMessage;

        if (isForfeit) {
            double fullReward = plugin.getConfig().getDouble("ctf-settings.win-reward-totems", 100);
            double forfeitPercentage = plugin.getConfig().getDouble("ctf-settings.forfeit-reward-percentage", 10.0);
            rewardAmount = fullReward * (forfeitPercentage / 100.0);

            String rewardMessageTemplate = plugin.getConfig().getString("ctf-settings.forfeit-win-message", "&6Sua equipe venceu por desistência e recebeu &e{amount} Totens&6.");
            formattedRewardMessage = ChatColor.translateAlternateColorCodes('&', rewardMessageTemplate.replace("{amount}", String.valueOf((int) rewardAmount)));
        } else {
            rewardAmount = plugin.getConfig().getDouble("ctf-settings.win-reward-totems", 0);
            String rewardMessageTemplate = plugin.getConfig().getString("ctf-settings.win-reward-message", "&6Você recebeu &e{amount} Totens&6 pela vitória!");
            formattedRewardMessage = ChatColor.translateAlternateColorCodes('&', rewardMessageTemplate.replace("{amount}", String.valueOf((int) rewardAmount)));
        }

        if (winner == null) {
            broadcastMessage(ChatColor.GOLD + "A partida terminou em empate!");
        } else {
            if (isForfeit) {
                TeamColor forfeitingTeamColor = (winner == teamOne) ? teamTwo : teamOne;
                broadcastMessage(forfeitingTeamColor.getChatColor() + "O time " + forfeitingTeamColor.getDisplayName() + " desistiu da partida.");
                broadcastMessage(winner.getChatColor() + "O time " + winner.getDisplayName() + " venceu por desistência!");
            } else {
                broadcastMessage(winner.getChatColor() + "O time " + winner.getDisplayName() + " venceu a partida!");
            }

            launchVictoryFireworks(winner);
            // Dá a recompensa para o time vencedor
            CTFTeam winningTeam = teams.get(winner);
            if (winningTeam != null) {
                Economy econ = plugin.getEconomy();
                if (econ != null && rewardAmount > 0) {
                    for (UUID playerUUID : winningTeam.getPlayers()) {
                        econ.depositPlayer(Bukkit.getOfflinePlayer(playerUUID), rewardAmount);
                    }
                }
            }
        }

        updateAndSavePlayerStats(winner);

        // Restaura o estado de todos os jogadores e os teleporta para suas localizações originais
        new BukkitRunnable() {
            @Override
            public void run() {
                // Usa a lista de participantes originais para garantir que todos sejam restaurados
                for (UUID playerUUID : originalParticipants) {
                    Player onlinePlayer = Bukkit.getPlayer(playerUUID);
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        // Restaura jogadores que estão online
                        removeGlow(onlinePlayer);
                        scoreboard.destroyPlayerScoreboard(onlinePlayer);
                        restorePlayerState(onlinePlayer); // Isso já teleporta o jogador de volta

                        if (winner != null && teams.get(winner).getPlayers().contains(playerUUID)) {
                            if (rewardAmount > 0) {
                                onlinePlayer.sendMessage(formattedRewardMessage);
                            }
                        } else if (winner != null) { // Jogador do time perdedor
                            onlinePlayer.sendMessage(ChatColor.GRAY + "O time adversário venceu. Mais sorte na próxima vez!");
                        }
                    } else {
                        // Apenas processa a restauração de estado para jogadores offline
                        restorePlayerState(playerUUID);
                    }
                }

                // Notifica o manager que o jogo acabou para limpeza
                plugin.getCtfManager().endGame(CTFGame.this);
            }
        }.runTaskLater(plugin, 60L); // Atraso de 3 segundos para os jogadores lerem as mensagens finais.
    }

    /**
     * Atualiza e salva as estatísticas permanentes de cada jogador no final da
     * partida.
     *
     * @param winner A cor do time vencedor, ou null se for empate.
     */
    private void updateAndSavePlayerStats(TeamColor winner) {
        for (Map.Entry<UUID, CTFPlayerStats> entry : playerStats.entrySet()) {
            UUID playerUUID = entry.getKey();
            CTFPlayerStats matchStats = entry.getValue();

            // Carrega as estatísticas permanentes do jogador
            PlayerCTFStats permanentStats = plugin.getPlayerDataManager().getPlayerCTFStats(playerUUID);
            boolean won = (winner != null && getPlayerTeamColor(playerUUID) == winner);
            permanentStats.addMatchStats(matchStats, won);

            plugin.getPlayerDataManager().savePlayerCTFStats(playerUUID, permanentStats);

            // Verifica os marcos históricos após atualizar as estatísticas
            // Executa na próxima tick para garantir que o arquivo já foi salvo.
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    plugin.getCtfMilestoneManager().checkAndGrantMilestones(player, permanentStats);
                }
            });
        }
    }

    /**
     * Finaliza o jogo quando o tempo acaba, determinando o vencedor pela
     * pontuação.
     */
    private void endGameByTime() {
        broadcastMessage(ChatColor.GOLD + "O tempo acabou!");

        CTFTeam teamOne = teams.get(this.teamOne);
        CTFTeam teamTwo = teams.get(this.teamTwo);

        if (teamOne.getScore() > teamTwo.getScore()) {
            endGame(teamOne.getColor(), false);
        } else if (teamTwo.getScore() > teamOne.getScore()) {
            endGame(teamTwo.getColor(), false);
        } else {
            endGame(null, false); // Empate
        }
    }

    public void handlePlayerQuit(Player player) {
        if (player.isOnline()) {
            scoreboard.destroyPlayerScoreboard(player);
            restorePlayerState(player); // Restaura inventário, etc.
        }

        TeamColor teamColor = getPlayerTeamColor(player.getUniqueId());
        if (teamColor != null) {
            for (CTFFlag flag : flags.values()) {
                if (flag.isCarried() && player.getUniqueId().equals(flag.getCarrier())) {
                    flag.drop(player.getLocation());
                    scoreboard.updateForAllPlayers();
                    broadcastMessage(ChatColor.YELLOW + "A bandeira do time " + flag.getTeamColor().getChatColor() + flag.getTeamColor().getDisplayName() + ChatColor.YELLOW + " foi derrubada pois o portador desconectou!");
                    break;
                }
            }

            CTFTeam leftTeam = teams.get(teamColor);
            broadcastMessage(teamColor.getChatColor() + player.getName() + ChatColor.YELLOW + " saiu da partida.");

            // Se o time ficou vazio, o outro time vence por desistência.
            // A verificação gameState == GameState.IN_PROGRESS impede que isso aconteça se o jogo já estiver terminando.
            if (gameState == GameState.IN_PROGRESS && leftTeam.getPlayers().isEmpty()) {
                TeamColor winningTeamColor = (teamColor == teamOne) ? teamTwo : teamOne;
                // A remoção do jogador deve acontecer DEPOIS da chamada do endGame
                // para que o resumo final tenha os dados de todos.
                playerTeams.remove(player.getUniqueId());
                leftTeam.removePlayer(player.getUniqueId());
                endGame(winningTeamColor, true);
            }
        }
    }

    public void handlePlayerDeath(Player player, PlayerDeathEvent event) {
        TeamColor teamColor = playerTeams.get(player.getUniqueId());
        if (teamColor == null) {
            return;
        }

        playerStats.get(player.getUniqueId()).incrementDeaths();

        // Mensagem de morte customizada
        Player killer = player.getKiller();
        String deathMessage;
        if (killer != null && playerTeams.containsKey(killer.getUniqueId())) {
            playerStats.get(killer.getUniqueId()).incrementKills();
            TeamColor killerTeamColor = playerTeams.get(killer.getUniqueId());
            deathMessage = teamColor.getChatColor() + player.getName() + ChatColor.YELLOW + " foi eliminado por " + killerTeamColor.getChatColor() + killer.getName() + ChatColor.YELLOW + ".";
        } else {
            deathMessage = teamColor.getChatColor() + player.getName() + ChatColor.YELLOW + " morreu.";
        }
        event.setDeathMessage(deathMessage); // Define a mensagem para o chat global

        // Verifica se o jogador estava carregando uma bandeira e a derruba
        for (CTFFlag flag : flags.values()) {
            if (flag.isCarried() && player.getUniqueId().equals(flag.getCarrier())) {
                flag.drop(player.getLocation());
                restorePlayerHelmet(player);
                removeGlow(player);
                scoreboard.updateForAllPlayers();
                broadcastMessage(ChatColor.YELLOW + "A bandeira do time " + flag.getTeamColor().getChatColor() + flag.getTeamColor().getDisplayName() + ChatColor.YELLOW + " foi derrubada!");
                break;
            }
        }

        // Agenda o respawn do jogador
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.spigot().respawn();
                    // O teleporte para o spawn será feito pelo evento de respawn para garantir que o jogador esteja pronto.
                }
            }
        }.runTaskLater(plugin, 1L); // Respawn quase imediato
    }

    public void handlePlayerRespawn(Player player, PlayerRespawnEvent event) {
        TeamColor teamColor = playerTeams.get(player.getUniqueId());
        if (teamColor == null) {
            return;
        }

        // Teleporta o jogador para o local de spawn do seu time.
        Location respawnLocation = (teamColor == teamOne) ? arena.getRedSpawn() : arena.getBlueSpawn();
        event.setRespawnLocation(respawnLocation);

        // Re-aplica o kit após o respawn
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    applyKit(player);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    public void handlePlayerMove(Player player) {
        if (gameState != GameState.IN_PROGRESS) {
            return;
        }

        TeamColor playerTeamColor = playerTeams.get(player.getUniqueId());
        if (playerTeamColor == null) {
            return;
        }

        Location playerLocation = player.getLocation();

        // --- Lógica de Interação com Bandeiras ---
        for (CTFFlag flag : flags.values()) {
            // Otimização: só checa bandeiras que estão próximas
            if (flag.getCurrentLocation() != null && flag.getCurrentLocation().distanceSquared(playerLocation) > 9) { // Raio de 3 blocos
                continue;
            }

            // 1. Tentar pegar a bandeira INIMIGA da base dela (se não estiver carregando outra)
            if (flag.isAtBase() && flag.getTeamColor() != playerTeamColor && getFlagCarriedBy(player) == null) {
                flag.pickUp(player);
                swapPlayerHelmetForFlag(player, flag);
                applyGlow(player);
                scoreboard.updateForAllPlayers();
                playSoundForAll(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                broadcastMessage(playerTeamColor.getChatColor() + player.getName() + ChatColor.YELLOW + " pegou a bandeira do time " + flag.getTeamColor().getChatColor() + flag.getTeamColor().getDisplayName() + "!");
                return; // Ação concluída
            }

            // 2. Tentar devolver a bandeira ALIADA que está no chão
            if (flag.isDropped() && flag.getTeamColor() == playerTeamColor) {
                flag.reset();
                scoreboard.updateForAllPlayers();
                playSoundForAll(Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
                broadcastMessage(playerTeamColor.getChatColor() + player.getName() + ChatColor.GREEN + " devolveu a bandeira do seu time para a base!");
                return; // Ação concluída
            }

            // 3. Tentar pegar a bandeira INIMIGA que está no chão (se não estiver carregando outra)
            if (flag.isDropped() && flag.getTeamColor() != playerTeamColor && getFlagCarriedBy(player) == null) {
                flag.pickUp(player);
                swapPlayerHelmetForFlag(player, flag);
                applyGlow(player);
                scoreboard.updateForAllPlayers();
                playSoundForAll(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                broadcastMessage(playerTeamColor.getChatColor() + player.getName() + ChatColor.YELLOW + " recuperou a bandeira " + flag.getTeamColor().getChatColor() + flag.getTeamColor().getDisplayName() + ChatColor.YELLOW + " que estava no chão!");
                return; // Ação concluída
            }
        }

        // --- Lógica de Pontuação ---
        CTFFlag carriedFlag = getFlagCarriedBy(player);
        if (carriedFlag != null) {
            CTFTeam playerTeam = teams.get(playerTeamColor);
            CTFFlag ownFlag = flags.get(playerTeamColor);

            // Verifica se o jogador está na área da sua própria base de bandeira
            if (ownFlag.getBaseLocation().distanceSquared(playerLocation) <= 9) { // Raio de 3 blocos
                // Regra clássica: só pode pontuar se a sua própria bandeira estiver na base
                if (ownFlag.isAtBase()) {
                    playerTeam.incrementScore();
                    playerStats.get(player.getUniqueId()).incrementFlagCaptures();
                    carriedFlag.reset();
                    restorePlayerHelmet(player);
                    removeGlow(player);
                    scoreboard.updateForAllPlayers();
                    playSoundForAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    broadcastMessage(playerTeamColor.getChatColor() + player.getName() + ChatColor.GOLD + " marcou um ponto para o time " + playerTeamColor.getDisplayName() + "!");

                    // Verifica se o time atingiu a pontuação para vencer
                    if (playerTeam.getScore() >= arena.getScoreToWin()) {
                        endGame(playerTeamColor, false);
                    }
                } else {
                    player.sendTitle("", ChatColor.RED + "Sua bandeira precisa estar na base para pontuar!", 10, 40, 10);
                }
            }
        }
    }

    public void broadcastMessage(String message) {
        for (UUID uuid : playerTeams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    private void broadcastEndGameSummary(TeamColor winner) {
        List<Map.Entry<UUID, CTFPlayerStats>> sortedByKills = new ArrayList<>(playerStats.entrySet());
        sortedByKills.sort((a, b) -> Integer.compare(b.getValue().getKills(), a.getValue().getKills()));

        List<Map.Entry<UUID, CTFPlayerStats>> sortedByCaptures = new ArrayList<>(playerStats.entrySet());
        sortedByCaptures.sort((a, b) -> Integer.compare(b.getValue().getFlagCaptures(), a.getValue().getFlagCaptures()));

        broadcastMessage("§e§m----------------------------------------------------");
        broadcastMessage("                     §6§lFIM DE JOGO");
        broadcastMessage(" ");

        if (winner != null) {
            broadcastMessage("          §aVencedor: " + winner.getChatColor() + "Time " + winner.getDisplayName());
        } else {
            broadcastMessage("                      §e§lEMPATE");
        }
        broadcastMessage(" ");

        // Top Killers
        broadcastMessage("  §c§lMAIS ABATES");
        for (int i = 0; i < Math.min(3, sortedByKills.size()); i++) {
            Map.Entry<UUID, CTFPlayerStats> entry = sortedByKills.get(i);
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                CTFPlayerStats stats = entry.getValue();
                TeamColor color = getPlayerTeamColor(p.getUniqueId());
                broadcastMessage(String.format("    §e%d. %s%s §7- §c%d abates",
                        i + 1,
                        color.getChatColor(),
                        p.getName(),
                        stats.getKills()));
            }
        }
        broadcastMessage(" ");

        // Top Capturers
        broadcastMessage("  §b§lMAIS CAPTURAS");
        for (int i = 0; i < Math.min(3, sortedByCaptures.size()); i++) {
            Map.Entry<UUID, CTFPlayerStats> entry = sortedByCaptures.get(i);
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && entry.getValue().getFlagCaptures() > 0) { // Só mostra se capturou pelo menos uma
                CTFPlayerStats stats = entry.getValue();
                TeamColor color = getPlayerTeamColor(p.getUniqueId());
                broadcastMessage(String.format("    §e%d. %s%s §7- §b%d capturas",
                        i + 1,
                        color.getChatColor(),
                        p.getName(),
                        stats.getFlagCaptures()));
            }
        }
        broadcastMessage("§e§m----------------------------------------------------");
    }

    /**
     * Toca um som para todos os jogadores na partida.
     *
     * @param sound O som a ser tocado.
     * @param volume O volume do som.
     * @param pitch A afinação do som.
     */
    public void playSoundForAll(Sound sound, float volume, float pitch) {
        for (UUID uuid : playerTeams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
    }

    /**
     * Envia um título (title) para todos os jogadores na partida.
     *
     * @param title O título principal.
     * @param subtitle O subtítulo.
     */
    private void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (UUID uuid : playerTeams.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
            }
        }
    }

    /**
     * Envia uma mensagem para todos os membros do time de um jogador.
     *
     * @param sender O jogador que enviou a mensagem.
     * @param message A mensagem a ser enviada.
     */
    public void sendTeamMessage(Player sender, String message) {
        TeamColor teamColor = getPlayerTeamColor(sender.getUniqueId());
        if (teamColor == null) {
            return;
        }

        CTFTeam team = getTeams().get(teamColor);
        if (team == null) {
            return;
        }

        String format = teamColor.getChatColor() + "[Time] " + sender.getName() + ": " + ChatColor.WHITE + message;
        team.broadcastMessage(format);
    }

    private void swapPlayerHelmetForFlag(Player player, CTFFlag flag) {
        PlayerInventory inv = player.getInventory();
        ItemStack currentHelmet = inv.getHelmet();
        if (currentHelmet != null && currentHelmet.getType() != Material.AIR) {
            // Guarda o capacete que o jogador estava usando (do kit)
            temporaryHelmets.put(player.getUniqueId(), currentHelmet);
        }
        // Coloca o estandarte na cabeça do jogador
        inv.setHelmet(new ItemStack(flag.getTeamColor().getBannerMaterial()));
    }

    private void restorePlayerHelmet(Player player) {
        if (player == null) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        // Pega o capacete que guardamos
        ItemStack originalHelmet = temporaryHelmets.remove(player.getUniqueId());
        if (originalHelmet != null) {
            // Devolve o capacete do kit
            inv.setHelmet(originalHelmet);
        } else {
            // Se ele não tinha nada, garante que o slot fique vazio
            inv.setHelmet(null);
        }
    }

    // --- Métodos de Efeitos Visuais ---
    /**
     * Aplica o efeito de brilho a um jogador.
     *
     * @param player O jogador que receberá o efeito.
     */
    private void applyGlow(Player player) {
        if (player != null) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true, false));
        }
    }

    /**
     * Remove o efeito de brilho de um jogador.
     *
     * @param player O jogador do qual o efeito será removido.
     */
    private void removeGlow(Player player) {
        if (player != null) {
            player.removePotionEffect(PotionEffectType.GLOWING);
        }
    }

    // --- Métodos de Gerenciamento de Estado e Kit ---
    private void savePlayerState(Player player) {
        originalPlayerStates.put(player.getUniqueId(), new PlayerState(player));
    }

    private void restorePlayerState(UUID playerUUID) {
        com.magnocat.mctrilhas.data.PlayerState state = originalPlayerStates.remove(playerUUID);
        if (state != null) {
            Player onlinePlayer = Bukkit.getPlayer(playerUUID);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                state.restore(onlinePlayer);
                // Teleporta o jogador de volta para sua localização original
                onlinePlayer.teleport(state.getLocation());
            } else {
                // Se o jogador estiver offline, a lógica de restauração (inventário, etc.)
                // precisaria ser adaptada para funcionar com OfflinePlayer, o que é complexo.
                // Por enquanto, o PlayerState já lida com a restauração ao logar.
            }
        }
    }

    private void restorePlayerState(Player player) {
        if (player == null) return;
        // Delega a lógica para o método principal, garantindo que o estado seja removido do mapa.
        restorePlayerState(player.getUniqueId());
    }

    private void clearPlayer(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
    }

    /**
     * Aplica o kit de itens configurado no config.yml ao jogador.
     *
     * @param player O jogador que receberá o kit.
     */
    private void applyKit(Player player) {
        TeamColor teamColor = playerTeams.get(player.getUniqueId());
        if (teamColor == null) {
            return;
        }

        PlayerInventory inv = player.getInventory();
        inv.clear();

        ConfigurationSection kitSection = plugin.getConfig().getConfigurationSection("ctf-settings.kit");
        if (kitSection == null) {
            plugin.logWarn("Seção 'ctf-settings.kit' não encontrada no config.yml. Nenhum kit foi aplicado.");
            return;
        }

        // Armadura
        ConfigurationSection armorSection = kitSection.getConfigurationSection("armor");
        if (armorSection != null) {
            inv.setHelmet(createTeamArmor(armorSection.getConfigurationSection("helmet"), teamColor));
            inv.setChestplate(createTeamArmor(armorSection.getConfigurationSection("chestplate"), teamColor));
            inv.setLeggings(createTeamArmor(armorSection.getConfigurationSection("leggings"), teamColor));
            inv.setBoots(createTeamArmor(armorSection.getConfigurationSection("boots"), teamColor));
        }

        // Itens
        ConfigurationSection itemsSection = kitSection.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String slotStr : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotStr);
                    ConfigurationSection itemConfig = itemsSection.getConfigurationSection(slotStr);
                    ItemStack item = ItemFactory.createFromConfig(itemConfig);
                    if (item != null) {
                        inv.setItem(slot, item);
                    }
                } catch (NumberFormatException e) {
                    plugin.logWarn("Slot inválido '" + slotStr + "' na configuração do kit CTF.");
                }
            }
        }

        player.updateInventory();
    }

    private CTFFlag getFlagCarriedBy(Player player) {
        for (CTFFlag flag : flags.values()) {
            if (flag.isCarried() && player.getUniqueId().equals(flag.getCarrier())) {
                return flag;
            }
        }
        return null;
    }

    private ItemStack createTeamArmor(ConfigurationSection armorConfig, TeamColor teamColor) {
        if (armorConfig == null) {
            return null;
        }
        ItemStack armorPiece = ItemFactory.createFromConfig(armorConfig);
        // Se a peça de armadura for de couro, colore com a cor do time.
        if (armorPiece != null && armorPiece.getItemMeta() instanceof LeatherArmorMeta) {
            LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
            meta.setColor(teamColor.getArmorColor());
            armorPiece.setItemMeta(meta);
        }
        return armorPiece;
    }

    // --- Getters ---
    public MCTrilhasPlugin getPlugin() {
        return plugin;
    }

    public CTFArena getArena() {
        return arena;
    }

    public GameState getGameState() {
        return gameState;
    }

    public Map<TeamColor, CTFTeam> getTeams() {
        return teams;
    }

    public Map<TeamColor, CTFFlag> getFlags() {
        return flags;
    }

    public TeamColor getTeamOne() {
        return teamOne;
    }

    public TeamColor getTeamTwo() {
        return teamTwo;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public Map<UUID, CTFPlayerStats> getPlayerStats() {
        return playerStats;
    }

    public CTFScoreboard getScoreboard() {
        return scoreboard;
    }

    public TeamColor getPlayerTeamColor(UUID playerUUID) {
        return playerTeams.get(playerUUID);
    }

    /**
     * Retorna uma lista de todos os jogadores online que estão nesta partida.
     *
     * @return A lista de jogadores.
     */
    public List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : playerTeams.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                players.add(p);
            }
        }
        return players;
    }

    /**
     * Lança fogos de artifício para celebrar a vitória de um time.
     * @param winningTeamColor A cor do time vencedor.
     */
    private void launchVictoryFireworks(TeamColor winningTeamColor) {
        CTFTeam winningTeam = teams.get(winningTeamColor);
        if (winningTeam == null) return;

        for (UUID playerUUID : winningTeam.getPlayers()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                // Lança 3 fogos de artifício por jogador vencedor
                for (int i = 0; i < 3; i++) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Firework fw = player.getWorld().spawn(player.getLocation(), Firework.class);
                            FireworkMeta fwm = fw.getFireworkMeta();
                            fwm.addEffect(FireworkEffect.builder().withColor(winningTeamColor.getArmorColor()).flicker(true).build());
                            fw.setFireworkMeta(fwm);
                        }
                    }.runTaskLater(plugin, i * 10L); // Lança com um pequeno atraso entre eles
                }
            }
        }
    }
}
