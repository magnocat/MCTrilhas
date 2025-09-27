package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.entity.Player;

import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Gerencia todo o sistema de Duelos 1v1.
 * <p>
 * Responsabilidades:
 * - Carregar e salvar arenas do arquivo `duel_arenas.yml`.
 * - Gerenciar sessões de criação de arenas para administradores.
 * - Controlar filas de jogadores para os duelos.
 * - Gerenciar as partidas de duelo ativas.
 */
public class DuelManager {

    private final MCTrilhasPlugin plugin;
    private final Map<String, DuelArena> arenas = new HashMap<>();
    private final Map<String, DuelKit> kits = new HashMap<>();
    private final Map<UUID, DuelArena> arenaCreationSessions = new HashMap<>();

    // Estruturas para gerenciar o fluxo de duelos
    private final Map<UUID, Challenge> challenges = new ConcurrentHashMap<>(); // Challenger UUID -> Challenge
    private final List<QueuedDuel> duelQueue = new ArrayList<>();
    private final List<DuelGame> activeDuels = new ArrayList<>();
    private final Map<UUID, Player> pendingKitSelection = new HashMap<>(); // Challenger -> Target
    private final Map<UUID, DuelGame> playerToGameMap = new HashMap<>(); // Mapeia um jogador (duelista ou espectador) ao seu jogo

    private File arenasFile;
    private FileConfiguration arenasConfig;
    private File kitsFile;
    private FileConfiguration kitsConfig;

    public DuelManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        this.arenasFile = new File(plugin.getDataFolder(), "duel_arenas.yml");
        this.arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        this.kitsFile = new File(plugin.getDataFolder(), "duel_kits.yml");
        this.kitsConfig = YamlConfiguration.loadConfiguration(kitsFile);

        loadArenas();
        loadKits();
        startQueueProcessor();
    }

    /**
     * Inicia uma sessão de criação de arena para um administrador.
     *
     * @param admin O jogador administrador que está criando a arena.
     * @param arenaName O nome da nova arena.
     */
    public void startArenaCreation(Player admin, String arenaName) {
        if (arenaCreationSessions.containsKey(admin.getUniqueId())) {
            admin.sendMessage(ChatColor.RED + "Você já está criando uma arena. Use '/scout admin duel save' para salvar ou '/scout admin duel cancel' para cancelar.");
            return;
        }

        if (arenas.containsKey(arenaName.toLowerCase())) {
            admin.sendMessage(ChatColor.RED + "Uma arena com o nome '" + arenaName + "' já existe.");
            return;
        }

        DuelArena newArena = new DuelArena(arenaName);
        arenaCreationSessions.put(admin.getUniqueId(), newArena);

        admin.sendMessage(ChatColor.GREEN + "Criação da arena '" + arenaName + "' iniciada!");
        admin.sendMessage(ChatColor.YELLOW + "Agora, use os seguintes comandos para configurá-la:");
        admin.sendMessage(ChatColor.AQUA + "/scout admin duel setspawn1" + ChatColor.GRAY + " - Define o ponto de spawn do jogador 1.");
        admin.sendMessage(ChatColor.AQUA + "/scout admin duel setspawn2" + ChatColor.GRAY + " - Define o ponto de spawn do jogador 2.");
        admin.sendMessage(ChatColor.AQUA + "/scout admin duel setspec" + ChatColor.GRAY + " - Define o ponto de spawn dos espectadores.");
        admin.sendMessage(ChatColor.AQUA + "/scout admin duel save" + ChatColor.GRAY + " - Salva a arena.");
    }

    /**
     * Obtém a sessão de criação de arena de um administrador.
     *
     * @param admin O jogador administrador.
     * @return O objeto DuelArena que está sendo criado, ou null se não houver sessão ativa.
     */
    public DuelArena getArenaCreationSession(Player admin) {
        return arenaCreationSessions.get(admin.getUniqueId());
    }

    public void setArenaSpawn(Player admin, int spawnPoint) {
        DuelArena sessionArena = getArenaCreationSession(admin);
        if (sessionArena == null) {
            admin.sendMessage(ChatColor.RED + "Você não está criando nenhuma arena. Use '/scout admin duel createarena <nome>' para começar.");
            return;
        }

        Location location = admin.getLocation();
        if (spawnPoint == 1) {
            sessionArena.setSpawn1(location);
        } else if (spawnPoint == 2) {
            sessionArena.setSpawn2(location);
        }

        admin.sendMessage(ChatColor.GREEN + "Ponto de spawn " + spawnPoint + " da arena '" + sessionArena.getName() + "' definido na sua localização atual.");
    }

    public void setArenaSpectatorPosition(Player admin) {
        DuelArena sessionArena = getArenaCreationSession(admin);
        if (sessionArena == null) {
            admin.sendMessage(ChatColor.RED + "Você não está criando nenhuma arena. Use '/scout admin duel createarena <nome>' para começar.");
            return;
        }

        sessionArena.setSpectatorSpawn(admin.getLocation());
        admin.sendMessage(ChatColor.GREEN + "Ponto de spawn dos espectadores para a arena '" + sessionArena.getName() + "' definido.");
    }

    public void saveArena(Player admin) {
        DuelArena sessionArena = getArenaCreationSession(admin);
        if (sessionArena == null) {
            admin.sendMessage(ChatColor.RED + "Você não está criando nenhuma arena para salvar.");
            return;
        }

        if (!sessionArena.isComplete()) {
            admin.sendMessage(ChatColor.RED + "A arena não está completa. Defina os spawns 1, 2 e o de espectador antes de salvar.");
            return;
        }

        String arenaName = sessionArena.getName();
        arenas.put(arenaName.toLowerCase(), sessionArena);

        // Salva no arquivo de configuração
        ConfigurationSection arenaSection = arenasConfig.createSection("arenas." + arenaName);
        sessionArena.saveToConfig(arenaSection);
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.logSevere("Não foi possível salvar a arena '" + arenaName + "' no arquivo duel_arenas.yml.");
            e.printStackTrace();
        }

        arenaCreationSessions.remove(admin.getUniqueId());
        admin.sendMessage(ChatColor.GREEN + "Arena '" + arenaName + "' salva com sucesso!");
    }

    public void cancelArenaCreation(Player admin) {
        if (arenaCreationSessions.remove(admin.getUniqueId()) != null) {
            admin.sendMessage(ChatColor.YELLOW + "Criação de arena cancelada.");
        } else {
            admin.sendMessage(ChatColor.RED + "Você não estava criando nenhuma arena.");
        }
    }

    public void loadArenas() {
        arenas.clear();
        ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
        if (arenasSection == null) {
            plugin.logInfo("Nenhuma arena de duelo encontrada para carregar.");
            return;
        }

        for (String arenaName : arenasSection.getKeys(false)) {
            ConfigurationSection currentArenaSection = arenasSection.getConfigurationSection(arenaName);
            if (currentArenaSection != null) {
                DuelArena arena = new DuelArena(arenaName);
                Location spawn1 = currentArenaSection.getLocation("spawn1");
                Location spawn2 = currentArenaSection.getLocation("spawn2");
                Location specSpawn = currentArenaSection.getLocation("spectatorSpawn");

                if (spawn1 != null && spawn2 != null && specSpawn != null) {
                    arena.setSpawn1(spawn1);
                    arena.setSpawn2(spawn2);
                    arena.setSpectatorSpawn(specSpawn);
                    arenas.put(arenaName.toLowerCase(), arena);
                } else {
                    plugin.logWarn("Arena '" + arenaName + "' está incompleta (faltando spawns) no arquivo duel_arenas.yml e não foi carregada.");
                }
            }
        }
        plugin.logInfo(arenas.size() + " arenas de duelo carregadas.");
    }

    public void loadKits() {
        kits.clear();
        ConfigurationSection kitsSection = kitsConfig.getConfigurationSection("kits");
        if (kitsSection == null) {
            plugin.logInfo("Nenhum kit de duelo encontrado para carregar.");
            return;
        }

        for (String kitId : kitsSection.getKeys(false)) {
            ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitId);
            DuelKit kit = DuelKit.fromConfig(kitId, kitSection);
            if (kit != null) {
                kits.put(kitId.toLowerCase(), kit);
            } else {
                plugin.logWarn("Falha ao carregar o kit de duelo '" + kitId + "'. Verifique a configuração.");
            }
        }
        plugin.logInfo(kits.size() + " kits de duelo carregados.");
    }

    public void stopTasks() {
        // Lógica para parar tarefas, se houver
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    // --- Métodos de acesso para outras classes ---

    public Map<String, DuelKit> getLoadedKits() {
        return kits;
    }

    public FileConfiguration getKitConfig() {
        return kitsConfig;
    }

    public Challenge getChallengeFor(UUID targetUUID) {
        for (Challenge challenge : challenges.values()) {
            if (challenge.target().getUniqueId().equals(targetUUID)) {
                return challenge;
            }
        }
        return null;
    }

    /**
     * Retorna uma lista de todos os jogadores que estão atualmente participando de um duelo ativo.
     * @return Uma lista de jogadores em duelo.
     */
    public List<Player> getDuelingPlayers() {
        List<Player> dueling = new ArrayList<>();
        activeDuels.forEach(game -> dueling.addAll(List.of(game.getPlayer1(), game.getPlayer2())));
        return dueling;
    }

    /**
     * Retorna uma lista de jogadores que estão online e disponíveis para serem desafiados.
     * Um jogador não está disponível se já estiver em um duelo, em uma fila ou com um desafio pendente.
     * @return Uma lista de jogadores elegíveis para desafio.
     */
    public List<Player> getChallengeablePlayers() {
        List<Player> challengeable = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID pUUID = p.getUniqueId();
            if (!playerToGameMap.containsKey(pUUID) && duelQueue.stream().noneMatch(q -> q.involves(p)) && getChallengeFor(pUUID) == null) {
                challengeable.add(p);
            }
        }
        return challengeable;
    }

    // --- Lógica de Jogo (a ser implementada) ---

    public void startChallengeProcess(Player challenger, Player target) {
        // Validações
        if (challenger.equals(target)) {
            challenger.sendMessage(ChatColor.RED + "Você não pode desafiar a si mesmo.");
            return;
        }
        if (playerToGameMap.containsKey(challenger.getUniqueId()) || playerToGameMap.containsKey(target.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + "Um dos jogadores já está em um duelo.");
            return;
        }
        if (challenges.containsKey(challenger.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + "Você já enviou um desafio. Aguarde ou cancele-o.");
            return;
        }
        if (getChallengeFor(target.getUniqueId()) != null) {
            challenger.sendMessage(ChatColor.RED + target.getName() + " já possui um desafio pendente.");
            return;
        }

        // Abre o menu de seleção de kits
        KitSelectionMenu kitMenu = new KitSelectionMenu(plugin);
        kitMenu.open(challenger);

        // Armazena temporariamente o alvo enquanto o desafiante escolhe o kit
        pendingKitSelection.put(challenger.getUniqueId(), target);

        // Adiciona uma tarefa para limpar a seleção pendente se o jogador fechar o menu ou demorar muito
        // (A lógica de timeout e cancelamento será adicionada no listener do inventário)
    }

    /**
     * Finaliza o processo de desafio após o desafiante selecionar um kit.
     * @param challenger O jogador que desafiou.
     * @param kitId O ID do kit selecionado.
     */
    public void finalizeChallenge(Player challenger, String kitId) {
        Player target = pendingKitSelection.remove(challenger.getUniqueId());
        if (target == null) {
            // O jogador pode ter demorado demais ou outro processo limpou a seleção.
            return;
        }

        Challenge challenge = new Challenge(challenger, target, kitId);
        challenges.put(challenger.getUniqueId(), challenge);

        challenger.sendMessage(ChatColor.GREEN + "Desafio enviado para " + target.getName() + "!");

        // Envia uma mensagem clicável para o alvo
        target.sendMessage(ChatColor.GOLD + "------------------------------------");
        target.sendMessage(ChatColor.YELLOW + challenger.getName() + " te desafiou para um duelo!");

        TextComponent acceptText = new TextComponent("[ACEITAR]");
        acceptText.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        acceptText.setBold(true);
        acceptText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duelo aceitar " + challenger.getName()));
        acceptText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Clique para aceitar o duelo").color(net.md_5.bungee.api.ChatColor.GRAY).create()));

        TextComponent denyText = new TextComponent("[NEGAR]");
        denyText.setColor(net.md_5.bungee.api.ChatColor.RED);
        denyText.setBold(true);
        denyText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duelo negar " + challenger.getName()));
        denyText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Clique para negar o duelo").color(net.md_5.bungee.api.ChatColor.GRAY).create()));

        target.spigot().sendMessage(acceptText, new TextComponent("  "), denyText);
        target.sendMessage(ChatColor.GRAY + "O desafio expira em " + plugin.getConfig().getInt("duel-settings.challenge-timeout-seconds", 60) + " segundos.");
        target.sendMessage(ChatColor.GOLD + "------------------------------------");
    }

    public void cancelChallengeCreation(Player challenger) {
        if (pendingKitSelection.remove(challenger.getUniqueId()) != null) {
            challenger.sendMessage(ChatColor.RED + "Você fechou o menu de seleção. Desafio cancelado.");
        }
    }

    public boolean isPlayerInKitSelection(UUID playerUUID) { return pendingKitSelection.containsKey(playerUUID); }

    public void acceptChallenge(Player target, Player challenger) {
        Challenge challenge = getChallengeFor(target.getUniqueId());

        // Validações
        if (challenge == null || !challenge.challenger().equals(challenger)) {
            target.sendMessage(ChatColor.RED + "Você não tem um desafio pendente de " + challenger.getName() + ".");
            return;
        }

        long timeout = plugin.getConfig().getLong("duel-settings.challenge-timeout-seconds", 60) * 1000L;
        if (challenge.isExpired(timeout)) {
            target.sendMessage(ChatColor.RED + "Este desafio de duelo já expirou.");
            challenges.remove(challenger.getUniqueId()); // Limpa o desafio expirado
            return;
        }

        // Remove o desafio da lista de pendentes
        challenges.remove(challenger.getUniqueId());

        DuelKit kit = kits.get(challenge.kitId().toLowerCase());
        if (kit == null) {
            challenger.sendMessage(ChatColor.RED + "Erro: O kit selecionado para o duelo não foi encontrado. O duelo foi cancelado.");
            target.sendMessage(ChatColor.RED + "Erro: O kit selecionado para o duelo não foi encontrado. O duelo foi cancelado.");
            return;
        }

        Optional<DuelArena> freeArenaOpt = findFreeArena();
        if (freeArenaOpt.isPresent()) {
            // Inicia o jogo imediatamente
            DuelGame game = new DuelGame(plugin, freeArenaOpt.get(), challenger, target, kit);
            activeDuels.add(game);
            playerToGameMap.put(challenger.getUniqueId(), game);
            playerToGameMap.put(target.getUniqueId(), game);
        } else {
            // Adiciona à fila de espera
            duelQueue.add(new QueuedDuel(challenger, target, kit));
            challenger.sendMessage(ChatColor.YELLOW + "Duelo aceito! Todas as arenas estão ocupadas. Vocês foram colocados na fila.");
            target.sendMessage(ChatColor.YELLOW + "Duelo aceito! Todas as arenas estão ocupadas. Vocês foram colocados na fila.");
        }
    }

    public void denyChallenge(Player target, Player challenger) {
        Challenge challenge = getChallengeFor(target.getUniqueId());

        // Valida se o desafio existe e se o desafiante é o correto.
        if (challenge == null || !challenge.challenger().equals(challenger)) {
            target.sendMessage(ChatColor.RED + "Você não tem um desafio pendente de " + challenger.getName() + ".");
            return;
        }

        // Remove o desafio da lista de pendentes.
        challenges.remove(challenger.getUniqueId());

        target.sendMessage(ChatColor.YELLOW + "Você negou o desafio de duelo de " + challenger.getName() + ".");
        challenger.sendMessage(ChatColor.RED + target.getName() + " negou seu desafio de duelo.");
    }

    public void startSpectating(Player spectator, Player target) {
        DuelGame game = playerToGameMap.get(target.getUniqueId());
        if (game != null) {
            game.addSpectator(spectator);
        } else {
            spectator.sendMessage(ChatColor.RED + "Este jogador não está em um duelo no momento.");
        }
    }

    public void stopSpectating(Player spectator) {
        DuelGame game = playerToGameMap.get(spectator.getUniqueId());
        if (game != null && !game.isParticipant(spectator)) {
            // O método removeSpectator já lida com a restauração do estado e o teleporte.
            game.removeSpectator(spectator);
        } else {
            spectator.sendMessage(ChatColor.RED + "Você não está assistindo a nenhum duelo.");
        }
    }

    public void forfeitDuel(Player player) {
        DuelGame game = playerToGameMap.get(player.getUniqueId());
        if (game != null && game.isParticipant(player)) {
            String forfeitMessage = plugin.getConfig().getString("duel-settings.messages.forfeit", "&e{player} &7desistiu do duelo. A partida terminou em empate.");
            game.endAsDraw(forfeitMessage, player);
        } else {
            player.sendMessage(ChatColor.RED + "Você não está em um duelo para poder desistir.");
        }
    }

    public void leaveQueue(Player player) {
        boolean removed = duelQueue.removeIf(queuedDuel -> {
            if (queuedDuel.involves(player)) {
                Player otherPlayer = queuedDuel.player1().equals(player) ? queuedDuel.player2() : queuedDuel.player1();
                if (otherPlayer.isOnline()) {
                    otherPlayer.sendMessage(ChatColor.YELLOW + player.getName() + " saiu da fila de duelos. O duelo foi cancelado.");
                }
                return true;
            }
            return false;
        });

        if (removed) {
            player.sendMessage(ChatColor.YELLOW + "Você saiu da fila de duelos.");
        } else {
            player.sendMessage(ChatColor.RED + "Você não está em nenhuma fila de duelos.");
        }
    }

    public void endDuel(DuelGame game) {
        activeDuels.remove(game);
        playerToGameMap.remove(game.getPlayer1().getUniqueId());
        playerToGameMap.remove(game.getPlayer2().getUniqueId());
        // Espectadores já são removidos do mapa pelo próprio DuelGame
    }

    public void addSpectatorToMap(UUID spectatorUUID, DuelGame game) {
        playerToGameMap.put(spectatorUUID, game);
    }

    public void removeSpectatorFromMap(UUID spectatorUUID) {
        playerToGameMap.remove(spectatorUUID);
    }

    private void startQueueProcessor() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (duelQueue.isEmpty()) {
                    return;
                }

                Optional<DuelArena> freeArenaOpt = findFreeArena();
                if (freeArenaOpt.isPresent()) {
                    QueuedDuel nextDuel = duelQueue.remove(0);
                    Player p1 = nextDuel.player1();
                    Player p2 = nextDuel.player2();

                    // Verifica se ambos os jogadores ainda estão online
                    if (p1.isOnline() && p2.isOnline()) {
                        p1.sendMessage(ChatColor.GREEN + "Uma arena foi encontrada! Iniciando seu duelo...");
                        p2.sendMessage(ChatColor.GREEN + "Uma arena foi encontrada! Iniciando seu duelo...");

                        DuelGame game = new DuelGame(plugin, freeArenaOpt.get(), p1, p2, nextDuel.kit());
                        activeDuels.add(game);
                        playerToGameMap.put(p1.getUniqueId(), game);
                        playerToGameMap.put(p2.getUniqueId(), game);
                    } else {
                        // Se um dos jogadores ficou offline, notifica o outro (se online)
                        Player onlinePlayer = p1.isOnline() ? p1 : (p2.isOnline() ? p2 : null);
                        if (onlinePlayer != null) {
                            onlinePlayer.sendMessage(ChatColor.RED + "Seu oponente ficou offline e o duelo na fila foi cancelado.");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // Verifica a cada 5 segundos
    }

    public Optional<DuelArena> findFreeArena() {
        return arenas.values().stream().filter(a -> activeDuels.stream().noneMatch(g -> g.getArena().equals(a))).findFirst();
    }

    public DuelGame getGameForPlayer(UUID uniqueId) {
        return playerToGameMap.get(uniqueId);
    }
}