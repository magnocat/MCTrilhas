package com.magnocat.mctrilhas.duels;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia todo o sistema de duelos 1v1.
 * <p>
 * Responsabilidades:
 * <ul>
 *     <li>Carregar e gerenciar as arenas de duelo.</li>
 *     <li>Processar desafios entre jogadores.</li>
 *     <li>Iniciar e finalizar partidas de duelo.</li>
 *     <li>Manter uma lista de jogos ativos.</li>
 * </ul>
 */
public class DuelManager implements Listener {
    private final MCTrilhasPlugin plugin;
    private final List<DuelArena> loadedArenas = new ArrayList<>();
    private final List<DuelArena> availableArenas = Collections.synchronizedList(new ArrayList<>());
    private final List<DuelGame> activeGames = new ArrayList<>();
    private final Map<UUID, DuelGame> playerGameMap = new HashMap<>();
    private final Map<UUID, DuelGame> spectatorGameMap = new HashMap<>();
    private final Queue<QueuedDuel> duelQueue = new LinkedList<>();
    private final Map<UUID, ArenaCreator> arenaCreators = new HashMap<>();
    private final Map<String, DuelKit> loadedKits = new LinkedHashMap<>();
    // Armazena temporariamente o alvo do desafio enquanto o desafiante escolhe o kit.
    private final Map<UUID, UUID> challengeTargetMap = new ConcurrentHashMap<>();
    private final Map<UUID, Challenge> pendingChallenges = new ConcurrentHashMap<>();
    private BukkitTask challengeExpirationTask;
    private File arenaConfigFile;
    private FileConfiguration arenaConfig;
    private File kitConfigFile;
    private FileConfiguration kitConfig;

    private static final long CHALLENGE_TIMEOUT_SECONDS = 60;

    public DuelManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadArenas();
        loadKits();
        startChallengeExpirationTask();
    }

    /**
     * Carrega as arenas do arquivo duel_arenas.yml.
     * Este método deve ser chamado no onEnable do plugin.
     */
    public void loadArenas() {
        createArenaConfigFile();

        loadedArenas.clear();
        availableArenas.clear();

        ConfigurationSection arenasSection = arenaConfig.getConfigurationSection("arenas");
        if (arenasSection == null) {
            plugin.logInfo("[Duels] Nenhuma arena configurada em duel_arenas.yml.");
            return;
        }

        for (String arenaId : arenasSection.getKeys(false)) {
            ConfigurationSection currentArenaSection = arenasSection.getConfigurationSection(arenaId);
            DuelArena arena = DuelArena.fromConfig(arenaId, currentArenaSection);
            if (arena != null) {
                loadedArenas.add(arena);
                availableArenas.add(arena); // Inicialmente, todas as arenas estão disponíveis
            }
        }
        plugin.logInfo("[Duels] " + loadedArenas.size() + " arena(s) de duelo carregada(s). " + availableArenas.size() + " disponível(is).");
    }

    /**
     * Carrega os kits do arquivo duel_kits.yml.
     */
    public void loadKits() {
        createKitConfigFile();
        loadedKits.clear();

        ConfigurationSection kitsSection = kitConfig.getConfigurationSection("kits");
        if (kitsSection == null) {
            plugin.logInfo("[Duels] Nenhum kit configurado em duel_kits.yml.");
            return;
        }

        for (String kitId : kitsSection.getKeys(false)) {
            ConfigurationSection currentKitSection = kitsSection.getConfigurationSection(kitId);
            DuelKit kit = DuelKit.fromConfig(kitId, currentKitSection);
            if (kit != null) {
                loadedKits.put(kitId.toLowerCase(), kit);
            }
        }
        plugin.logInfo("[Duels] " + loadedKits.size() + " kit(s) de duelo carregado(s).");
    }

    /**
     * Inicia o processo de desafio, abrindo a GUI de seleção de kits.
     * @param challenger O jogador que está desafiando.
     * @param target O jogador que está sendo desafiado.
     */
    public void startChallengeProcess(Player challenger, Player target) {
        // Armazena quem é o alvo para que possamos recuperar após a seleção do kit.
        challengeTargetMap.put(challenger.getUniqueId(), target.getUniqueId());

        // Abre a GUI de seleção de kits.
        KitSelectionMenu menu = new KitSelectionMenu(plugin);
        menu.open(challenger);
    }

    /**
     * Envia um desafio de duelo de um jogador para outro.
     * @param challenger O jogador que está desafiando.
     * @param target O jogador que está sendo desafiado.
     * @param kitId O ID do kit escolhido para o duelo.
     */
    public void sendChallenge(Player challenger, Player target, String kitId) {
        DuelKit kit = getKit(kitId);
        if (kit == null) {
            challenger.sendMessage(ChatColor.RED + "O kit '" + kitId + "' não existe.");
            return;
        }

        if (challenger.equals(target)) {
            challenger.sendMessage(ChatColor.RED + "Você não pode desafiar a si mesmo!");
            return;
        }

        if (playerGameMap.containsKey(challenger.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + "Você já está em um duelo.");
            return;
        }

        if (playerGameMap.containsKey(target.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + target.getName() + " já está em um duelo.");
            return;
        }

        if (pendingChallenges.containsKey(target.getUniqueId())) {
            challenger.sendMessage(ChatColor.RED + target.getName() + " já tem um desafio pendente.");
            return;
        }

        // Verifica se os jogadores já estão na fila
        for (QueuedDuel qd : duelQueue) {
            if (qd.involves(challenger)) {
                challenger.sendMessage(ChatColor.RED + "Você já está na fila para um duelo. Use /duelo sairfila para sair.");
                return;
            }
            if (qd.involves(target)) {
                challenger.sendMessage(ChatColor.RED + target.getName() + " já está na fila para um duelo.");
                return;
            }
        }

        // Verifica se o desafiante já enviou um desafio para outra pessoa
        for (Challenge existingChallenge : pendingChallenges.values()) {
            if (existingChallenge.getChallenger().equals(challenger)) {
                challenger.sendMessage(ChatColor.RED + "Você já enviou um desafio. Aguarde a resposta ou o desafio expirar.");
                return;
            }
        }

        Challenge challenge = new Challenge(challenger, target, kit.getId());
        pendingChallenges.put(target.getUniqueId(), challenge);

        challenger.sendMessage(ChatColor.GREEN + "Você desafiou " + target.getName() + " para um duelo com o kit " + kit.getDisplayName() + ChatColor.GREEN + "! Aguardando resposta...");

        // Mensagem para o alvo
        target.sendMessage(ChatColor.GOLD + "---------------------------------");
        target.sendMessage(ChatColor.YELLOW + challenger.getName() + " te desafiou para um duelo!");
        target.sendMessage(ChatColor.YELLOW + "Kit: " + kit.getDisplayName());

        TextComponent acceptComponent = new TextComponent(ChatColor.GREEN + "" + ChatColor.BOLD + "[ACEITAR]");
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duelo aceitar " + challenger.getName()));
        acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.GREEN + "Clique para aceitar o duelo").create()));

        TextComponent denyComponent = new TextComponent(ChatColor.RED + "" + ChatColor.BOLD + "[NEGAR]");
        denyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duelo negar " + challenger.getName()));
        denyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.RED + "Clique para negar o duelo").create()));

        TextComponent message = new TextComponent(" ");
        message.addExtra(acceptComponent);
        message.addExtra("  ");
        message.addExtra(denyComponent);

        target.spigot().sendMessage(message);
        target.sendMessage(ChatColor.GRAY + "O desafio expira em " + CHALLENGE_TIMEOUT_SECONDS + " segundos.");
        target.sendMessage(ChatColor.GOLD + "---------------------------------");
    }

    /**
     * Aceita um desafio de duelo.
     * @param target O jogador que está aceitando (quem recebeu o desafio).
     * @param challenger O jogador que enviou o desafio.
     */
    public void acceptChallenge(Player target, Player challenger) {
        Challenge challenge = pendingChallenges.get(target.getUniqueId());

        if (challenge == null || !challenge.getChallenger().getUniqueId().equals(challenger.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "Você não tem um desafio pendente de " + challenger.getName() + ".");
            return;
        }

        // Remove o desafio da lista de pendentes
        pendingChallenges.remove(target.getUniqueId());

        DuelKit kit = getKit(challenge.getKitId());
        if (kit == null) {
            // Fallback para o kit padrão se o kit do desafio não existir mais
            kit = getDefaultKit();
        }

        if (availableArenas.isEmpty()) {
            QueuedDuel queuedDuel = new QueuedDuel(challenger, target, kit);
            duelQueue.add(queuedDuel);
            String queueMessage = ChatColor.YELLOW + "Todas as arenas estão ocupadas. Vocês foram colocados na fila para um duelo. Posição: " + duelQueue.size();
            challenger.sendMessage(queueMessage);
            target.sendMessage(queueMessage);
        } else {
            startDuel(challenger, target, kit);
        }
    }

    /**
     * Nega um desafio de duelo.
     * @param target O jogador que está negando (quem recebeu o desafio).
     * @param challenger O jogador que enviou o desafio.
     */
    public void denyChallenge(Player target, Player challenger) {
        Challenge challenge = pendingChallenges.get(target.getUniqueId());

        if (challenge == null || !challenge.getChallenger().getUniqueId().equals(challenger.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "Você não tem um desafio pendente de " + challenger.getName() + " para negar.");
            return;
        }

        // Remove o desafio da lista de pendentes
        pendingChallenges.remove(target.getUniqueId());

        target.sendMessage(ChatColor.YELLOW + "Você negou o desafio de " + challenger.getName() + ".");
        challenger.sendMessage(ChatColor.RED + target.getName() + " negou seu desafio de duelo.");
    }

    /**
     * Obtém o desafio pendente para um jogador específico.
     * @param targetUUID O UUID do jogador que recebeu o desafio.
     * @return O objeto Challenge, ou null se não houver desafio.
     */
    public Challenge getChallengeFor(UUID targetUUID) {
        return pendingChallenges.get(targetUUID);
    }

    private void startDuel(Player player1, Player player2, DuelKit kit) {
        if (availableArenas.isEmpty()) {
            plugin.logWarn("startDuel foi chamado mas não há arenas disponíveis. Colocando jogadores de volta na fila.");
            duelQueue.add(new QueuedDuel(player1, player2, kit)); // Adiciona no início da fila
            return;
        }
        DuelArena arena = availableArenas.remove(0);

        DuelGame newGame = new DuelGame(plugin, arena, player1, player2, kit);
        activeGames.add(newGame);
        playerGameMap.put(player1.getUniqueId(), newGame);
        playerGameMap.put(player2.getUniqueId(), newGame);
    }

    // --- Métodos para Criação de Arenas In-Game ---

    public void startArenaCreation(Player admin, String arenaId) {
        if (arenaCreators.containsKey(admin.getUniqueId())) {
            admin.sendMessage(ChatColor.RED + "Você já está criando uma arena. Use /duelo cancelarena para cancelar.");
            return;
        }
        arenaCreators.put(admin.getUniqueId(), new ArenaCreator(arenaId));
        admin.sendMessage(ChatColor.GREEN + "Criação da arena '" + arenaId + "' iniciada.");
        admin.sendMessage(ChatColor.YELLOW + "Vá para o primeiro ponto de spawn e use /duelo setpos 1");
    }

    public void setArenaPosition(Player admin, int pos) {
        ArenaCreator creator = arenaCreators.get(admin.getUniqueId());
        if (creator == null) {
            admin.sendMessage(ChatColor.RED + "Você não iniciou a criação de uma arena. Use /duelo createarena <id>.");
            return;
        }
        if (pos == 1) {
            creator.setPos1(admin.getLocation());
            admin.sendMessage(ChatColor.GREEN + "Posição 1 da arena '" + creator.getId() + "' definida.");
            admin.sendMessage(ChatColor.YELLOW + "Agora vá para o segundo ponto de spawn e use /duelo setpos 2");
        } else if (pos == 2) {
            creator.setPos2(admin.getLocation());
            admin.sendMessage(ChatColor.GREEN + "Posição 2 da arena '" + creator.getId() + "' definida.");
            admin.sendMessage(ChatColor.YELLOW + "Use /duelo savearena para salvar a arena.");
        } else {
            admin.sendMessage(ChatColor.RED + "Posição inválida. Use 1 ou 2.");
        }
    }

    public void setArenaSpectatorPosition(Player admin) {
        ArenaCreator creator = arenaCreators.get(admin.getUniqueId());
        if (creator == null) {
            admin.sendMessage(ChatColor.RED + "Você não iniciou a criação de uma arena. Use /duelo createarena <id>.");
            return;
        }
        creator.setSpectatorSpawn(admin.getLocation());
        admin.sendMessage(ChatColor.GREEN + "Posição do espectador para a arena '" + creator.getId() + "' definida.");
    }

    public void saveArena(Player admin) {
        ArenaCreator creator = arenaCreators.get(admin.getUniqueId());
        if (creator == null) {
            admin.sendMessage(ChatColor.RED + "Você não está criando nenhuma arena.");
            return;
        }
        if (!creator.isReady()) {
            admin.sendMessage(ChatColor.RED + "A arena não está completa. Defina as posições 1, 2 e a do espectador.");
            return;
        }

        // Salva a nova arena no arquivo duel_arenas.yml
        String path = "arenas." + creator.getId();
        arenaConfig.set(path + ".pos1", locationToString(creator.getPos1()));
        arenaConfig.set(path + ".pos2", locationToString(creator.getPos2()));
        arenaConfig.set(path + ".spectator-spawn", locationToString(creator.getSpectatorSpawn()));

        try {
            arenaConfig.save(arenaConfigFile);
            admin.sendMessage(ChatColor.GREEN + "Arena '" + creator.getId() + "' salva com sucesso!");
            arenaCreators.remove(admin.getUniqueId());
            // Recarrega as arenas para que a nova já esteja disponível
            loadArenas();
        } catch (IOException e) {
            admin.sendMessage(ChatColor.RED + "Erro ao salvar a arena no arquivo. Verifique o console.");
            plugin.logSevere("Não foi possível salvar a arena de duelo: " + e.getMessage());
        }
    }

    public void cancelArenaCreation(Player admin) {
        if (arenaCreators.remove(admin.getUniqueId()) != null) {
            admin.sendMessage(ChatColor.YELLOW + "Criação de arena cancelada.");
        } else {
            admin.sendMessage(ChatColor.RED + "Você não estava criando nenhuma arena.");
        }
    }

    private String locationToString(Location loc) {
        if (loc == null) return "";
        // Formata a string de localização para ser consistente com o parseLocation
        return String.format("%s,%.2f,%.2f,%.2f,%.1f,%.1f",
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch())
                .replace(",", ", "); // Adiciona um espaço após a vírgula para legibilidade
    }

    public DuelKit getKit(String id) {
        if (id == null) return getDefaultKit();
        return loadedKits.get(id.toLowerCase());
    }

    public DuelKit getDefaultKit() {
        // Tenta pegar o kit "default", se não, pega o primeiro da lista.
        DuelKit kit = loadedKits.get("default");
        if (kit != null) {
            return kit;
        }
        if (!loadedKits.isEmpty()) {
            return loadedKits.values().iterator().next();
        }
        return null; // Nenhum kit carregado
    }

    public Map<String, DuelKit> getLoadedKits() {
        return loadedKits;
    }

    /**
     * Obtém o jogo de duelo em que um jogador está participando.
     * @param player O jogador.
     * @return A instância de DuelGame, ou null se não estiver em um duelo.
     */
    public DuelGame getGameForPlayer(Player player) {
        return playerGameMap.get(player.getUniqueId());
    }

    /**
     * Finaliza um duelo, liberando a arena e os jogadores.
     * @param game O jogo a ser finalizado.
     */
    public void endDuel(DuelGame game) {
        activeGames.remove(game);
        playerGameMap.remove(game.getPlayer1().getUniqueId());
        playerGameMap.remove(game.getPlayer2().getUniqueId());

        availableArenas.add(game.getArena());
        plugin.logInfo("[Duels] Duelo na arena " + game.getArena().getId() + " finalizado. Arena agora está disponível.");

        checkDuelQueue();
    }

    public void startSpectating(Player spectator, Player target) {
        DuelGame game = getGameForPlayer(target);
        if (game == null) {
            spectator.sendMessage(ChatColor.RED + target.getName() + " não está em um duelo no momento.");
            return;
        }
        game.addSpectator(spectator);
    }

    public void stopSpectating(Player spectator) {
        DuelGame game = spectatorGameMap.get(spectator.getUniqueId());
        if (game == null) {
            spectator.sendMessage(ChatColor.RED + "Você não está assistindo a nenhum duelo.");
            return;
        }
        game.removeSpectator(spectator);
    }

    /**
     * Adiciona um espectador ao mapa de rastreamento.
     * Chamado por DuelGame.
     */
    public void addSpectatorToMap(UUID spectatorUUID, DuelGame game) {
        spectatorGameMap.put(spectatorUUID, game);
    }

    /**
     * Remove um espectador do mapa de rastreamento.
     * Chamado por DuelGame.
     */
    public void removeSpectatorFromMap(UUID spectatorUUID) {
        spectatorGameMap.remove(spectatorUUID);
    }

    /**
     * Processa a desistência de um jogador em um duelo.
     * @param forfeiter O jogador que está desistindo.
     */
    public void forfeitDuel(Player forfeiter) {
        DuelGame game = getGameForPlayer(forfeiter);
        if (game == null) {
            forfeiter.sendMessage(ChatColor.RED + "Você não está em um duelo para poder desistir.");
            return;
        }

        String forfeitMessage = plugin.getConfig().getString("duel-settings.messages.forfeit", "&e{player} &7desistiu do duelo. A partida terminou em empate.");
        game.endAsDraw(forfeitMessage, forfeiter);
    }

    public void leaveQueue(Player player) {
        boolean removed = duelQueue.removeIf(queuedDuel -> {
            if (queuedDuel.involves(player)) {
                Player other = queuedDuel.player1().equals(player) ? queuedDuel.player2() : queuedDuel.player1();
                if (other.isOnline()) {
                    other.sendMessage(ChatColor.RED + "Seu duelo em fila foi cancelado porque " + player.getName() + " saiu da fila.");
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

    private void checkDuelQueue() {
        // Usa um laço para processar a fila até encontrar um duelo válido ou a fila/arenas acabarem.
        while (!duelQueue.isEmpty() && !availableArenas.isEmpty()) {
            QueuedDuel nextDuel = duelQueue.poll(); // Pega e remove o primeiro da fila

            Player p1 = nextDuel.player1();
            Player p2 = nextDuel.player2();

            // Verifica se o duelo é válido
            if (p1 != null && p1.isOnline() && p2 != null && p2.isOnline() && !playerGameMap.containsKey(p1.getUniqueId()) && !playerGameMap.containsKey(p2.getUniqueId())) {
                p1.sendMessage(ChatColor.GREEN + "Uma arena ficou disponível! Seu duelo contra " + p2.getName() + " está começando.");
                p2.sendMessage(ChatColor.GREEN + "Uma arena ficou disponível! Seu duelo contra " + p1.getName() + " está começando.");
                startDuel(p1, p2, nextDuel.kit());
                break; // Encontrou um duelo válido e iniciou, pode sair do laço.
            } else {
                // Se o duelo não é válido, notifica o jogador restante (se houver) e o laço continua para o próximo.
                Player remainingPlayer = (p1 != null && p1.isOnline()) ? p1 : p2;
                if (remainingPlayer != null && remainingPlayer.isOnline()) {
                    remainingPlayer.sendMessage(ChatColor.RED + "Seu duelo em fila foi cancelado porque o oponente não está mais disponível.");
                }
            }
        }
    }

    /**
     * Inicia a tarefa que verifica e remove desafios de duelo expirados.
     */
    private void startChallengeExpirationTask() {
        challengeExpirationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingChallenges.isEmpty()) {
                    return;
                }

                Iterator<Map.Entry<UUID, Challenge>> iterator = pendingChallenges.entrySet().iterator();
                while (iterator.hasNext()) {
                    Challenge challenge = iterator.next().getValue();
                    if (challenge.isExpired(CHALLENGE_TIMEOUT_SECONDS * 1000)) {
                        iterator.remove(); // Remove o desafio do mapa

                        Player challenger = challenge.getChallenger();
                        Player target = challenge.getTarget();

                        // Notifica os jogadores se eles ainda estiverem online
                        if (challenger != null && challenger.isOnline()) {
                            challenger.sendMessage(ChatColor.RED + "Seu desafio de duelo para " + target.getName() + " expirou.");
                        }
                        if (target != null && target.isOnline()) {
                            target.sendMessage(ChatColor.RED + "O desafio de duelo de " + challenger.getName() + " expirou.");
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Verifica a cada segundo
    }

    public void stopTasks() {
        if (challengeExpirationTask != null && !challengeExpirationTask.isCancelled()) {
            challengeExpirationTask.cancel();
        }
    }
    private void createArenaConfigFile() {
        arenaConfigFile = new File(plugin.getDataFolder(), "duel_arenas.yml");
        if (!arenaConfigFile.exists()) {
            plugin.saveResource("duel_arenas.yml", false);
        }
        arenaConfig = YamlConfiguration.loadConfiguration(arenaConfigFile);
    }

    private void createKitConfigFile() {
        kitConfigFile = new File(plugin.getDataFolder(), "duel_kits.yml");
        if (!kitConfigFile.exists()) {
            plugin.saveResource("duel_kits.yml", false);
        }
        kitConfig = YamlConfiguration.loadConfiguration(kitConfigFile);
    }

    public FileConfiguration getKitConfig() {
        return kitConfig;
    }
}