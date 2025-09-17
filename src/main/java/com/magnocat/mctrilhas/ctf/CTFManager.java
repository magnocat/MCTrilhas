package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gerencia todo o sistema de Capture The Flag (CTF).
 * Responsável por carregar arenas, gerenciar filas e iniciar/terminar partidas.
 */
public class CTFManager {

    private final MCTrilhasPlugin plugin;
    private final List<CTFArena> loadedArenas = new ArrayList<>();
    private final List<CTFGame> activeGames = new ArrayList<>();
    private final Map<UUID, CTFGame> playerGameMap = new HashMap<>();
    private final Map<UUID, ArenaBuilder> arenaBuilders = new HashMap<>();
    private final Queue<UUID> playerQueue = new LinkedList<>();
    private File arenaConfigFile;
    private FileConfiguration arenaConfig;

    public CTFManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Carrega as arenas do arquivo ctf_arenas.yml.
     * Este método deve ser chamado no onEnable do plugin.
     */
    public void loadArenas() {
        createArenaConfigFile(); // Garante que o arquivo exista e esteja carregado

        loadedArenas.clear(); // Limpa a lista antes de recarregar

        ConfigurationSection arenasSection = arenaConfig.getConfigurationSection("arenas");
        if (arenasSection == null) {
            plugin.getLogger().warning("[CTF] Nenhuma seção 'arenas' encontrada em ctf_arenas.yml. Nenhuma arena será carregada.");
            return;
        }

        for (String arenaId : arenasSection.getKeys(false)) {
            ConfigurationSection currentArenaSection = arenasSection.getConfigurationSection(arenaId);
            if (currentArenaSection != null) {
                CTFArena arena = CTFArena.fromConfig(arenaId, currentArenaSection);
                if (arena != null) {
                    loadedArenas.add(arena);
                }
            }
        }

        plugin.getLogger().info("[CTF] " + loadedArenas.size() + " arena(s) carregada(s) com sucesso.");
    }

    /**
     * Cria o arquivo de configuração das arenas se ele não existir e o carrega.
     */
    private void createArenaConfigFile() {
        arenaConfigFile = new File(plugin.getDataFolder(), "ctf_arenas.yml");
        if (!arenaConfigFile.exists()) {
            // Copia o arquivo padrão de dentro do .jar para a pasta do plugin
            plugin.saveResource("ctf_arenas.yml", false);
            plugin.getLogger().info("[CTF] Arquivo ctf_arenas.yml criado com a configuração padrão.");
        }

        arenaConfig = YamlConfiguration.loadConfiguration(arenaConfigFile);
    }

    /**
     * Retorna uma lista imutável das arenas carregadas.
     * @return A lista de arenas.
     */
    public List<CTFArena> getArenas() {
        return Collections.unmodifiableList(loadedArenas);
    }

    /**
     * Adiciona um jogador à fila do CTF.
     * @param player O jogador a ser adicionado.
     */
    public void addPlayerToQueue(Player player) {
        if (playerGameMap.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Você já está em uma partida de CTF!");
            return;
        }
        if (playerQueue.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Você já está na fila do CTF.");
            return;
        }
        playerQueue.add(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Você entrou na fila para uma partida de Capture a Bandeira!");
        plugin.logInfo("[CTF] " + player.getName() + " entrou na fila. Total: " + playerQueue.size());

        tryToStartGame();
    }

    /**
     * Lida com a saída de um jogador, seja da fila ou de uma partida em andamento.
     * @param player O jogador a ser removido.
     */
    public void handlePlayerLeave(Player player) {
        CTFGame game = playerGameMap.get(player.getUniqueId());
        if (game != null) {
            // O jogador está em uma partida
            game.handlePlayerQuit(player);
            playerGameMap.remove(player.getUniqueId());
        } else if (playerQueue.remove(player.getUniqueId())) {
            // O jogador estava na fila
            player.sendMessage(ChatColor.RED + "Você saiu da fila do CTF.");
            plugin.logInfo("[CTF] " + player.getName() + " saiu da fila. Total: " + playerQueue.size());
        } else {
            player.sendMessage(ChatColor.YELLOW + "Você não está em nenhuma partida ou fila de CTF.");
        }
    }

    /**
     * Tenta iniciar uma nova partida se houver jogadores suficientes na fila e uma arena disponível.
     */
    private void tryToStartGame() {
        Optional<CTFArena> availableArena = loadedArenas.stream()
                .filter(arena -> !isArenaInUse(arena))
                .filter(arena -> playerQueue.size() >= arena.getMinPlayers())
                .findFirst();

        if (availableArena.isPresent()) {
            CTFArena arena = availableArena.get();

            int playersForGame = Math.min(playerQueue.size(), arena.getMaxPlayers());
            // Garante que o número de jogadores seja par para balancear os times
            if (playersForGame % 2 != 0) {
                playersForGame--;
            }

            if (playersForGame < arena.getMinPlayers()) {
                return; // Não há jogadores suficientes após o ajuste para número par.
            }

            List<Player> participants = new ArrayList<>();
            for (int i = 0; i < playersForGame; i++) {
                UUID playerUUID = playerQueue.poll();
                if (playerUUID != null) {
                    Player p = Bukkit.getPlayer(playerUUID);
                    if (p != null) {
                        participants.add(p);
                    }
                }
            }

            plugin.logInfo("[CTF] Iniciando partida na arena '" + arena.getName() + "' com " + participants.size() + " jogadores.");
            CTFGame newGame = new CTFGame(plugin, arena, participants);
            activeGames.add(newGame);
            for (Player p : participants) {
                playerGameMap.put(p.getUniqueId(), newGame);
            }
        }
    }

    /**
     * Finaliza uma partida, removendo-a da lista de jogos ativos e limpando os dados dos jogadores.
     * @param game O jogo a ser finalizado.
     */
    public void endGame(CTFGame game) {
        activeGames.remove(game);
        // Limpa os jogadores do mapa de jogos
        game.getTeams().values().forEach(team ->
                team.getPlayers().forEach(playerUUID -> playerGameMap.remove(playerUUID))
        );
        plugin.logInfo("[CTF] Partida na arena '" + game.getArena().getName() + "' finalizada.");
    }

    /**
     * Retorna a instância do jogo em que um jogador está participando.
     * @param player O jogador.
     * @return A instância de CTFGame, ou null se o jogador não estiver em uma partida.
     */
    public CTFGame getGameForPlayer(Player player) {
        return playerGameMap.get(player.getUniqueId());
    }

    /**
     * Verifica se uma arena já está sendo usada em uma partida ativa.
     * @param arena A arena a ser verificada.
     * @return true se a arena estiver em uso, false caso contrário.
     */
    private boolean isArenaInUse(CTFArena arena) {
        return activeGames.stream().anyMatch(game -> game.getArena().getId().equals(arena.getId()));
    }

    // --- Métodos de Administração de Arenas ---

    public void startArenaCreation(Player admin, String arenaId) {
        if (arenaBuilders.containsKey(admin.getUniqueId())) {
            admin.sendMessage(ChatColor.RED + "Você já está criando uma arena. Use '/ctf admin cancel' para cancelar.");
            return;
        }
        if (arenaConfig.contains("arenas." + arenaId)) {
            admin.sendMessage(ChatColor.RED + "Uma arena com o ID '" + arenaId + "' já existe.");
            return;
        }

        ArenaBuilder builder = new ArenaBuilder(arenaId);
        arenaBuilders.put(admin.getUniqueId(), builder);
        admin.sendMessage(ChatColor.GREEN + "Iniciando criação da arena '" + arenaId + "'.");
        admin.sendMessage(ChatColor.YELLOW + "Vá para os locais desejados e use '/ctf admin set <tipo>'.");
        showArenaCreationStatus(admin);
    }

    public void setArenaLocation(Player admin, String locationType) {
        ArenaBuilder builder = arenaBuilders.get(admin.getUniqueId());
        if (builder == null) {
            admin.sendMessage(ChatColor.RED + "Você não está criando uma arena. Use '/ctf admin create <id>' para começar.");
            return;
        }

        List<String> validTypes = Arrays.asList("lobby", "redspawn", "bluespawn", "redflag", "blueflag");
        if (!validTypes.contains(locationType.toLowerCase())) {
            admin.sendMessage(ChatColor.RED + "Tipo de local inválido. Use um dos seguintes: " + String.join(", ", validTypes));
            return;
        }

        builder.setLocation(locationType, admin.getLocation());
        admin.sendMessage(ChatColor.GREEN + "Local '" + locationType + "' definido para a sua posição atual.");
        showArenaCreationStatus(admin);
    }

    public void saveArena(Player admin) {
        ArenaBuilder builder = arenaBuilders.get(admin.getUniqueId());
        if (builder == null) {
            admin.sendMessage(ChatColor.RED + "Você não está criando uma arena.");
            return;
        }
        if (!builder.isComplete()) {
            admin.sendMessage(ChatColor.RED + "A arena não está completa. Defina todos os pontos antes de salvar.");
            showArenaCreationStatus(admin);
            return;
        }

        String basePath = "arenas." + builder.getId();
        arenaConfig.set(basePath + ".name", "&f" + builder.getName()); // Nome padrão
        arenaConfig.set(basePath + ".min-players", 2);
        arenaConfig.set(basePath + ".max-players", 16);
        arenaConfig.set(basePath + ".score-to-win", 3);
        arenaConfig.set(basePath + ".game-duration-seconds", 600);
        arenaConfig.set(basePath + ".countdown-seconds", 10);
        arenaConfig.set(basePath + ".flag-reset-seconds", 30);

        for (Map.Entry<String, Location> entry : builder.getLocations().entrySet()) {
            arenaConfig.set(basePath + ".locations." + entry.getKey(), formatLocation(entry.getValue()));
        }

        try {
            arenaConfig.save(arenaConfigFile);
            admin.sendMessage(ChatColor.GREEN + "Arena '" + builder.getId() + "' salva com sucesso!");
            arenaBuilders.remove(admin.getUniqueId());
            loadArenas(); // Recarrega as arenas para que a nova já esteja disponível
        } catch (IOException e) {
            admin.sendMessage(ChatColor.RED + "Ocorreu um erro ao salvar a arena. Verifique o console.");
            plugin.getLogger().severe("Não foi possível salvar o arquivo ctf_arenas.yml: " + e.getMessage());
        }
    }

    public void cancelArenaCreation(Player admin) {
        if (arenaBuilders.remove(admin.getUniqueId()) != null) {
            admin.sendMessage(ChatColor.YELLOW + "Criação de arena cancelada.");
        } else {
            admin.sendMessage(ChatColor.RED + "Você não está criando nenhuma arena.");
        }
    }

    public void showArenaCreationStatus(Player admin) {
        ArenaBuilder builder = arenaBuilders.get(admin.getUniqueId());
        if (builder == null) {
            admin.sendMessage(ChatColor.RED + "Você não está criando uma arena.");
            return;
        }
        admin.sendMessage(builder.getStatus());
    }

    private String formatLocation(Location loc) {
        if (loc == null) return "";
        return String.format(Locale.US, "%s,%.2f,%.2f,%.2f,%.2f,%.2f",
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch()
        );
    }
}