package com.magnocat.mctrilhas.web;

// Java Standard Library
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerCTFStats;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.duels.PlayerDuelStats;
import com.magnocat.mctrilhas.ranks.Rank;
import com.magnocat.mctrilhas.utils.SecurityUtils;
import com.sun.management.OperatingSystemMXBean;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import net.kyori.adventure.text.Component;

/**
 * Gerencia o servidor web integrado e a API RESTful do plugin.
 * <p>
 * Esta classe é responsável por:
 * <ul>
 * <li>Iniciar e parar um servidor HTTP leve.</li>
 * <li>Servir arquivos estáticos (HTML, CSS, JS) para os painéis web.</li>
 * <li>Extrair e atualizar automaticamente os recursos da web a cada nova versão
 * do plugin.</li>
 * <li>Fornecer endpoints de API públicos e protegidos para dados do servidor e
 * de jogadores.</li>
 * <li>Gerenciar a autenticação de administradores via JWT (JSON Web
 * Tokens).</li>
 * <li>Manter caches de dados para rankings e estatísticas para garantir
 * respostas rápidas da API.</li>
 * <li>Agendar tarefas assíncronas para atualizar os caches periodicamente.</li>
 * </ul>
 */
public class HttpApiManager {

    // --- Core Components ---
    private final MCTrilhasPlugin plugin;
    private HttpServer server;
    private final Gson gson = new GsonBuilder().create();
    private Algorithm jwtAlgorithm;

    // --- Constants ---
    private static final int MAX_HISTORY_POINTS = 144; // 144 pontos * 10 min = 24 horas
    private static final int MAX_CHAT_HISTORY = 100;

    // --- Caches ---
    private final List<Map<String, Object>> activityHistory = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> dailyLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> monthlyLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> allTimeLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> ctfWinsLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> ctfKillsLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> ctfCapturesLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> eloLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, CachedPlayerResponse> playerResponseCache = new ConcurrentHashMap<>();
    private final Map<String, Object> economyStatsCache = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> chatHistory = new CopyOnWriteArrayList<>();

    private BukkitTask badgeCacheUpdater;
    public HttpApiManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicia o servidor web e configura todos os seus componentes. A
     * inicialização só ocorre se a API estiver habilitada no `config.yml`.
     */
    public void start() {
        if (!plugin.getConfig().getBoolean("web-api.enabled", false)) {
            plugin.logInfo("API web integrada está desativada na configuração.");
            return;
        }

        extractWebResources();

        setupJwt();

        setupAdminCredentials();

        scheduleActivitySnapshot();

        scheduleEconomyStatsUpdate();

        int port = plugin.getConfig().getInt("web-api.port", 22222);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // --- Public Endpoints ---
            server.createContext("/api/v1/data", this::handleApiDataRequest);
            server.createContext("/api/v1/player", this::handlePlayerDataRequest);

            // --- Admin Authentication ---
            server.createContext("/api/v1/admin/login", this::handleAdminLoginRequest);

            // --- Admin Endpoints (Protected by JWT) ---
            server.createContext("/api/v1/admin/status", this::handleAdminStatusRequest);
            server.createContext("/api/v1/admin/players/online", this::handleAdminOnlinePlayersRequest);
            server.createContext("/api/v1/admin/players/offline", this::handleAdminOfflinePlayersRequest);
            server.createContext("/api/v1/admin/server-metrics", this::handleAdminServerMetricsRequest);
            server.createContext("/api/v1/admin/player-action", this::handleAdminPlayerActionRequest);
            server.createContext("/api/v1/admin/player-details", this::handleAdminPlayerDetailsRequest);
            server.createContext("/api/v1/admin/badge-action", this::handleAdminBadgeActionRequest);
            server.createContext("/api/v1/admin/rank-action", this::handleAdminRankActionRequest);
            server.createContext("/api/v1/admin/player-inventory", this::handleAdminPlayerInventoryRequest);
            server.createContext("/api/v1/admin/broadcast-message", this::handleAdminBroadcastRequest);
            server.createContext("/api/v1/admin/economy-stats", this::handleAdminEconomyStatsRequest);
            server.createContext("/api/v1/admin/game-chat", this::handleAdminGameChatRequest);
            server.createContext("/api/v1/admin/execute-command", this::handleAdminExecuteCommandRequest);
            server.createContext("/api/v1/admin/list-admins", this::handleAdminListAdminsRequest);
            server.createContext("/api/v1/admin/rank-distribution", this::handleAdminRankDistributionRequest);

            // --- Static File Server ---
            // Define o diretório raiz de onde os arquivos web serão servidos.
            Path webRoot = new File(plugin.getDataFolder(), "web").toPath();

            // Usa o novo handler seguro para servir todos os arquivos do diretório web.
            server.createContext("/", new SecureHttpFileHandler(webRoot));

            server.setExecutor(null); // usa o executor padrão
            server.start();
            plugin.logInfo("Servidor da API web iniciado na porta: " + port);

        } catch (IOException e) {
            plugin.logSevere("Falha ao iniciar o servidor da API web: " + e.getMessage());
        }
    }

    /**
     * Extrai todos os recursos da pasta 'web' do JAR para a pasta de dados do
     * plugin. Isso garante que todos os arquivos do site (HTML, CSS, JS,
     * imagens) estejam disponíveis no disco. A extração não sobrescreve
     * arquivos existentes, permitindo personalização.
     */
    private void extractWebResources() {
        String currentBuildTimestamp = getBuildTimestampFromJar();
        String lastKnownTimestamp = getLastKnownBuildTimestamp();

        if (currentBuildTimestamp != null && currentBuildTimestamp.equals(lastKnownTimestamp)) {
            plugin.logInfo("Versão do plugin não alterada. Pulando sincronização dos recursos da web.");
            return;
        }

        plugin.logInfo("Nova versão do plugin detectada. Sincronizando recursos da web...");
        try {
            File webDir = new File(plugin.getDataFolder(), "web");

            // Deleta o diretório web antigo para garantir uma instalação limpa e remover arquivos obsoletos.
            if (webDir.exists()) {
                deleteDirectory(webDir);
            }

            // Extrai os novos recursos do JAR.
            CodeSource src = plugin.getClass().getProtectionDomain().getCodeSource();
            if (src != null) {
                URL jar = src.getLocation();
                try (ZipInputStream zip = new ZipInputStream(jar.openStream())) {
                    ZipEntry e;
                    while ((e = zip.getNextEntry()) != null) {
                        String name = e.getName();
                        if (name.startsWith("web/")) {
                            plugin.saveResource(name, false);
                        }
                    }
                }
                plugin.logInfo("Recursos da web sincronizados com sucesso.");
                // Salva o novo timestamp após a sincronização bem-sucedida
                saveBuildTimestamp(currentBuildTimestamp);
            } else {
                plugin.logWarn("Não foi possível encontrar a fonte do JAR para extrair os recursos da web.");
            }
        } catch (IOException e) {
            plugin.logSevere("Falha ao sincronizar recursos da web: " + e.getMessage());
        }
    }

    private String getBuildTimestampFromJar() {
        try (InputStream input = plugin.getResource("build.properties")) {
            if (input == null) {
                plugin.logWarn("Arquivo 'build.properties' não encontrado no JAR. A verificação de versão não funcionará.");
                return null;
            }
            Properties prop = new Properties();
            prop.load(input);
            String timestamp = prop.getProperty("build.timestamp");
            // O Maven pode deixar o valor literal se o filtering falhar, então verificamos.
            if (timestamp != null && !timestamp.startsWith("$")) {
                return timestamp;
            }
        } catch (IOException ex) {
            plugin.logWarn("Não foi possível ler o 'build.properties': " + ex.getMessage());
        }
        return null;
    }

    /**
     * Deleta um diretório e todo o seu conteúdo de forma recursiva.
     *
     * @param directory O diretório a ser deletado.
     */
    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }

    private String getLastKnownBuildTimestamp() {
        File timestampFile = new File(plugin.getDataFolder(), "web_build.info");
        if (!timestampFile.exists()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(timestampFile); java.util.Scanner scanner = new java.util.Scanner(fis)) {
            return scanner.hasNext() ? scanner.next() : null;
        } catch (IOException e) {
            plugin.logWarn("Não foi possível ler o arquivo de timestamp da web: " + e.getMessage());
            return null;
        }
    }

    private void saveBuildTimestamp(String timestamp) {
        if (timestamp == null) {
            return;
        }
        File timestampFile = new File(plugin.getDataFolder(), "web_build.info");
        try (FileWriter writer = new FileWriter(timestampFile, false)) {
            writer.write(timestamp);
        } catch (IOException e) {
            plugin.logSevere("Não foi possível salvar o arquivo de timestamp da web: " + e.getMessage());
        }
    }

    /**
     * Verifica as credenciais de administrador no config.yml. Se a senha
     * estiver em texto plano (identificado pela ausência de um 'salt'), ela é
     * criptografada com hash e um novo 'salt' é gerado e salvo.
     */
    private void setupAdminCredentials() {
        String passwordPath = "web-api.admin-credentials.password";
        String saltPath = "web-api.admin-credentials.salt";
        String salt = plugin.getConfig().getString(saltPath);

        // Se o salt estiver vazio, significa que a senha ainda está em texto plano.
        if (salt == null || salt.isEmpty()) {
            String plainPassword = plugin.getConfig().getString(passwordPath, "changeme");
            plugin.logInfo("Gerando hash seguro para a senha do administrador...");
            String newSalt = SecurityUtils.generateSalt();
            String hashedPassword = SecurityUtils.hashPassword(plainPassword, newSalt);

            plugin.getConfig().set(passwordPath, hashedPassword);
            plugin.getConfig().set(saltPath, newSalt);
            plugin.saveConfig();
            plugin.logInfo("Senha do administrador foi criptografada e salva com segurança.");
        }
    }

    /**
     * Configura o segredo para a assinatura de tokens JWT. Se o segredo não
     * estiver definido no config.yml, um novo é gerado e salvo.
     */
    private void setupJwt() {
        String secretPath = "web-api.jwt-secret";
        String secret = plugin.getConfig().getString(secretPath);

        if (secret == null || secret.isEmpty()) {
            plugin.logInfo("Gerando novo segredo JWT...");
            SecureRandom random = new SecureRandom();
            byte[] secretBytes = new byte[64]; // 512 bits
            random.nextBytes(secretBytes);
            secret = Base64.getEncoder().encodeToString(secretBytes);

            plugin.getConfig().set(secretPath, secret);
            plugin.saveConfig();
            plugin.logInfo("Segredo JWT gerado e salvo com segurança.");
        }

        this.jwtAlgorithm = Algorithm.HMAC256(secret);
    }

    private void handleApiDataRequest(HttpExchange exchange) throws IOException {
        // Configura headers para permitir o acesso de qualquer origem (CORS)
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            return;
        }

        // --- Verificação da Chave de API ---
        String requiredKey = plugin.getConfig().getString("web-api.api-key", "").trim();
        if (!requiredKey.isEmpty()) {
            String providedKey = exchange.getRequestHeaders().getFirst("X-API-Key");
            if (!requiredKey.equals(providedKey)) {
                String error = "{\"error\":\"Chave de API inválida ou não fornecida.\"}";
                exchange.sendResponseHeaders(403, error.length()); // Forbidden
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
                return;
            }
        }

        // Gera os dados da API usando os caches
        Map<String, Object> apiData = generateApiDataFromCache();
        String jsonResponse = gson.toJson(apiData);
        byte[] responseBytes = jsonResponse.getBytes("UTF-8");

        exchange.sendResponseHeaders(200, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void handleAdminLoginRequest(HttpExchange exchange) throws IOException {
        // Habilita CORS para o painel de administração
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        // Responde a requisições OPTIONS para o pre-flight do CORS
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1); // No Content
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use POST.\"}");
            return;
        }

        try {
            // Lê o corpo da requisição (JSON)
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            @SuppressWarnings("unchecked")
            Map<String, String> loginData = gson.fromJson(requestBody, Map.class);
            String providedUser = loginData.get("username");
            String providedPass = loginData.get("password");

            // Obtém credenciais seguras do config.yml
            String requiredUser = plugin.getConfig().getString("web-api.admin-credentials.username", "admin");
            String hashedPassword = plugin.getConfig().getString("web-api.admin-credentials.password");
            String salt = plugin.getConfig().getString("web-api.admin-credentials.salt");

            if (hashedPassword == null || salt == null || salt.isEmpty()) {
                // Se a senha não estiver hasheada, algo deu muito errado na inicialização.
                plugin.logSevere("As credenciais de administrador não estão configuradas corretamente (hash/salt ausentes).");
                sendJsonResponse(exchange, 500, "{\"success\":false, \"error\":\"Erro de configuração do servidor.\"}");
                return;
            }

            // Gera o hash da senha fornecida usando o salt armazenado
            String providedHash = SecurityUtils.hashPassword(providedPass, salt);

            if (requiredUser.equals(providedUser) && hashedPassword.equals(providedHash)) {
                // Sucesso! Gera um token JWT para a sessão do administrador.
                long expirationHours = plugin.getConfig().getLong("web-api.jwt-expiration-hours", 24);
                String token = JWT.create()
                        .withIssuer("MCTrilhas")
                        .withSubject(requiredUser) // Identifica o usuário
                        .withIssuedAt(Instant.now())
                        .withExpiresAt(Instant.now().plus(expirationHours, ChronoUnit.HOURS))
                        .sign(jwtAlgorithm);
                sendJsonResponse(exchange, 200, "{\"success\":true, \"token\":\"" + token + "\"}");
            } else {
                // Falha
                sendJsonResponse(exchange, 401, "{\"success\":false, \"error\":\"Usuário ou senha inválidos.\"}");
            }
        } catch (Exception e) {
            plugin.logSevere("Erro ao processar requisição de login: " + e.getMessage());
            sendJsonResponse(exchange, 500, "{\"success\":false, \"error\":\"Erro interno do servidor.\"}");
        }
    }

    private void handleAdminOnlinePlayersRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Verifica se o token de admin é válido
        DecodedJWT decodedJWT = verifyAdminToken(exchange);
        if (decodedJWT == null) {
            return; // O erro já foi enviado
        }

        // Coleta os dados dos jogadores online
        List<Map<String, Object>> playersList = Bukkit.getOnlinePlayers().stream()
                .map(player -> {
                    Map<String, Object> playerData = new HashMap<>();
                    playerData.put("name", player.getName());
                    playerData.put("uuid", player.getUniqueId().toString());
                    playerData.put("ping", player.getPing());
                    playerData.put("gamemode", player.getGameMode().toString());
                    playerData.put("world", player.getWorld().getName());
                    if (player.getAddress() != null && player.getAddress().getAddress() != null) {
                        playerData.put("ip", player.getAddress().getAddress().getHostAddress());
                    } else {
                        playerData.put("ip", "N/A");
                    }
                    // Adiciona a informação se o jogador possui um token de acesso web.
                    com.magnocat.mctrilhas.data.PlayerData pData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                    boolean hasToken = pData != null && pData.getWebAccessToken() != null && !pData.getWebAccessToken().isEmpty();
                    playerData.put("hasWebToken", hasToken);
                    playerData.put("isBedrock", pData != null && pData.isBedrockPlayer());

                    return playerData;
                })
                .collect(Collectors.toList());

        sendJsonResponse(exchange, 200, gson.toJson(playersList));
    }

    private void handleAdminOfflinePlayersRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (verifyAdminToken(exchange) == null) {
            return;
        }

        Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
        final int page = Integer.parseInt(params.getOrDefault("page", "1"));
        final int pageSize = Integer.parseInt(params.getOrDefault("pageSize", "25"));
        final String searchTerm = params.getOrDefault("search", "").toLowerCase(Locale.ROOT);

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    File playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
                    File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
                    List<Map<String, String>> offlinePlayers = new ArrayList<>();

                    if (playerFiles != null) {
                        for (File playerFile : playerFiles) {
                            try {
                                UUID uuid = UUID.fromString(playerFile.getName().replace(".yml", ""));
                                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                                String playerName = offlinePlayer.getName();
                                if (playerName != null && !offlinePlayer.isOnline() && (searchTerm.isEmpty() || playerName.toLowerCase(Locale.ROOT).contains(searchTerm))) {
                                    Map<String, String> playerData = new HashMap<>();
                                    playerData.put("name", offlinePlayer.getName());
                                    playerData.put("uuid", uuid.toString());
                                    playerData.put("lastPlayed", String.valueOf(offlinePlayer.getLastPlayed()));
                                    offlinePlayers.add(playerData);
                                }
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    // Ordena por data de último login, do mais recente para o mais antigo
                    offlinePlayers.sort((p1, p2) -> Long.compare(Long.parseLong(p2.get("lastPlayed")), Long.parseLong(p1.get("lastPlayed"))));

                    // Lógica de Paginação
                    int totalPlayers = offlinePlayers.size();
                    int totalPages = (int) Math.ceil((double) totalPlayers / pageSize);
                    int fromIndex = (page - 1) * pageSize;
                    int toIndex = Math.min(fromIndex + pageSize, totalPlayers);

                    List<Map<String, String>> paginatedList = (fromIndex < totalPlayers) ? offlinePlayers.subList(fromIndex, toIndex) : new ArrayList<>();

                    Map<String, Object> response = new HashMap<>();
                    response.put("players", paginatedList);
                    response.put("totalPages", totalPages);
                    response.put("currentPage", page);
                    sendJsonResponse(exchange, 200, gson.toJson(response));
                } catch (IOException e) {
                    plugin.logSevere("Erro de IO ao listar jogadores offline via API: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleAdminStatusRequest(HttpExchange exchange) throws IOException {
        // Configura CORS para permitir cabeçalhos de autorização
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        DecodedJWT decodedJWT = verifyAdminToken(exchange);
        if (decodedJWT == null) {
            // O método verifyAdminToken já enviou a resposta de erro (401).
            return;
        }

        // Se chegamos aqui, o token é válido.
        // Criamos alguns dados que só um admin poderia ver.
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("serverVersion", Bukkit.getVersion());
        statusData.put("bukkitVersion", Bukkit.getBukkitVersion());
        statusData.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        statusData.put("maxPlayers", Bukkit.getMaxPlayers());
        statusData.put("authenticatedUser", decodedJWT.getSubject());
        statusData.put("tokenExpiresAt", decodedJWT.getExpiresAt());

        sendJsonResponse(exchange, 200, gson.toJson(statusData));
    }

    private void handleAdminServerMetricsRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Verifica se o token de admin é válido
        if (verifyAdminToken(exchange) == null) {
            return; // O erro já foi enviado
        }

        // Coleta as métricas do servidor
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        Runtime runtime = Runtime.getRuntime();

        double cpuUsage = osBean.getProcessCpuLoad() * 100;
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        double tps = Bukkit.getTPS()[0]; // Média de 1 minuto

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cpuUsage", String.format(Locale.US, "%.2f", cpuUsage));
        metrics.put("usedMemory", usedMemory);
        metrics.put("maxMemory", maxMemory);
        metrics.put("tps", String.format(Locale.US, "%.2f", tps));
        metrics.put("timestamp", System.currentTimeMillis());
        metrics.put("pluginVersion", plugin.getDescription().getVersion());

        sendJsonResponse(exchange, 200, gson.toJson(metrics));
    }

    private void handleAdminPlayerActionRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Verifica se o token de admin é válido
        if (verifyAdminToken(exchange) == null) {
            return; // O erro já foi enviado
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use POST.\"}");
            return;
        }

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            @SuppressWarnings("unchecked")
            Map<String, String> actionData = gson.fromJson(requestBody, Map.class);

            String action = actionData.get("action");
            String targetName = actionData.get("targetName");
            String reason = actionData.getOrDefault("reason", "Ação executada pelo painel de administração.");

            if (action == null || targetName == null) {
                sendJsonResponse(exchange, 400, "{\"success\":false, \"error\":\"Ação ou alvo não especificado.\"}");
                return;
            }

            // Executa o comando na thread principal do servidor para garantir a segurança.
            // A busca do jogador (potencialmente bloqueante) é feita de forma assíncrona primeiro.
            new BukkitRunnable() {
                @Override
                public void run() { // Roda de forma assíncrona
                    // Esta chamada pode bloquear, por isso está em uma thread assíncrona.
                    final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

                    // Após obter o jogador, volta para a thread principal para executar a ação.
                    new BukkitRunnable() {
                        @Override
                        public void run() { // Roda na thread principal
                            if (!targetPlayer.hasPlayedBefore()) {
                                plugin.logWarn("API action '" + action + "' para '" + targetName + "' falhou: jogador não encontrado.");
                                return;
                            }

                            String commandToRun = "";
                            if ("kick".equalsIgnoreCase(action)) {
                                commandToRun = "kick " + targetName + " " + reason;
                            } else if ("ban".equalsIgnoreCase(action)) {
                                commandToRun = "ban " + targetName + " " + reason;
                            } else if ("give-totems".equalsIgnoreCase(action) || "take-totems".equalsIgnoreCase(action)) {
                                if (plugin.getEconomy() != null) {
                                    try {
                                        double amount = Double.parseDouble(actionData.getOrDefault("amount", "0"));
                                        if ("give-totems".equalsIgnoreCase(action)) {
                                            plugin.getEconomy().depositPlayer(targetPlayer, amount);
                                        } else {
                                            plugin.getEconomy().withdrawPlayer(targetPlayer, amount);
                                        }
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }

                            if (!commandToRun.isEmpty()) {
                                plugin.logInfo("Executando comando via API: " + commandToRun);
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                            }
                        }
                    }.runTask(plugin);
                }
            }.runTaskAsynchronously(plugin);

            sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Ação '" + action + "' para " + targetName + " enviada para execução.\"}");
        } catch (Exception e) {
            plugin.logSevere("Erro ao processar ação de jogador via API: " + e.getMessage());
            sendJsonResponse(exchange, 500, "{\"success\":false, \"error\":\"Erro interno do servidor.\"}");
        }
    }

    private void handleAdminPlayerDetailsRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Verifica se o token de admin é válido
        if (verifyAdminToken(exchange) == null) {
            return; // O erro já foi enviado
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use GET.\"}");
            return;
        }

        Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
        String uuidStr = params.get("uuid");

        if (uuidStr == null || uuidStr.isEmpty()) {
            sendJsonResponse(exchange, 400, "{\"error\":\"UUID do jogador não fornecido.\"}");
            return;
        }

        // Executa a busca de dados de forma assíncrona para não bloquear a thread do servidor web.
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    UUID playerUUID = UUID.fromString(uuidStr);
                    Map<String, Object> responseData = buildPlayerDetailsMap(playerUUID);
                    if (responseData == null) {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Não foi possível encontrar ou carregar dados para o UUID fornecido.\"}");
                        return;
                    }
                    sendJsonResponse(exchange, 200, gson.toJson(responseData));
                } catch (IllegalArgumentException e) {
                    try {
                        sendJsonResponse(exchange, 400, "{\"error\":\"UUID fornecido é inválido.\"}");
                    } catch (IOException ignored) {
                    }
                } catch (IOException e) {
                    plugin.logSevere("Erro de IO ao enviar detalhes do jogador via API: " + e.getMessage());
                    // Não podemos enviar uma resposta aqui, pois a conexão pode já ter sido fechada.
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleAdminBadgeActionRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (verifyAdminToken(exchange) == null) {
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use POST.\"}");
            return;
        }

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            @SuppressWarnings("unchecked")
            Map<String, String> actionData = gson.fromJson(requestBody, Map.class);

            String action = actionData.get("action");
            String badgeId = actionData.get("badgeId");
            UUID targetUUID = UUID.fromString(actionData.get("targetUuid"));

            if (action == null || badgeId == null || targetUUID == null) {
                sendJsonResponse(exchange, 400, "{\"success\":false, \"error\":\"Dados da ação incompletos.\"}");
                return;
            }

            // Ações de insígnia requerem que o jogador esteja online para receber recompensas/mensagens.
            Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sendJsonResponse(exchange, 400, "{\"success\":false, \"error\":\"O jogador alvo precisa estar online para esta ação.\"}");
                return;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        boolean success = false;
                        if ("grant".equalsIgnoreCase(action)) {
                            success = plugin.getPlayerDataManager().grantBadgeAndReward(targetPlayer, badgeId);
                        } else if ("revoke".equalsIgnoreCase(action)) {
                            plugin.getPlayerDataManager().removeBadgeAndResetProgress(targetPlayer, badgeId);
                            success = true; // Assumimos sucesso, pois o método é void.
                        }

                        if (success) {
                            String actionText = "grant".equalsIgnoreCase(action) ? "concedida" : "revogada";
                            sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Insignia '" + badgeId + "' " + actionText + " para " + targetPlayer.getName() + ".\"}");
                        } else {
                            sendJsonResponse(exchange, 400, "{\"success\":false, \"error\":\"Ação falhou. O jogador já pode ter a insígnia ou a insígnia é inválida.\"}");
                        }
                    } catch (IOException e) {
                        plugin.logSevere("Erro de IO ao enviar resposta da ação de insígnia: " + e.getMessage());
                    }
                }
            }.runTask(plugin);

        } catch (Exception e) {
            plugin.logSevere("Erro ao processar ação de insígnia via API: " + e.getMessage());
            sendJsonResponse(exchange, 500, "{\"success\":false, \"error\":\"Erro interno do servidor.\"}");
        }
    }

    private void handleAdminRankActionRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (verifyAdminToken(exchange) == null) {
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use POST.\"}");
            return;
        }

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            @SuppressWarnings("unchecked")
            Map<String, String> actionData = gson.fromJson(requestBody, Map.class);

            final UUID targetUUID = UUID.fromString(actionData.get("targetUuid"));
            final String rankStr = actionData.get("rank");

            final Player targetPlayer = Bukkit.getPlayer(targetUUID);
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sendJsonResponse(exchange, 400, "{\"success\":false, \"error\":\"O jogador alvo precisa estar online para esta ação.\"}");
                return;
            }

            final com.magnocat.mctrilhas.ranks.Rank newRank = com.magnocat.mctrilhas.ranks.Rank.valueOf(rankStr.toUpperCase());

            // Executa a alteração de ranque na thread principal para garantir a segurança.
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(targetUUID);
                        if (playerData != null) {
                            playerData.setRank(newRank);
                            targetPlayer.sendMessage("§aSeu ranque foi alterado para §e" + newRank.getDisplayName() + "§a por um administrador.");
                            sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Ranque de " + targetPlayer.getName() + " alterado para " + newRank.name() + ".\"}");
                        } else {
                            sendJsonResponse(exchange, 500, "{\"success\":false, \"error\":\"Não foi possível encontrar os dados do jogador.\"}");
                        }
                    } catch (IOException e) {
                        plugin.logSevere("Erro de IO ao enviar resposta da alteração de ranque: " + e.getMessage());
                    }
                }
            }.runTask(plugin);

        } catch (Exception e) {
            plugin.logSevere("Erro ao processar alteração de ranque via API: " + e.getMessage());
            sendJsonResponse(exchange, 500, "{\"success\":false, \"error\":\"Erro interno do servidor: " + e.getMessage() + "\"}");
        }
    }

    private void handleAdminPlayerInventoryRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (verifyAdminToken(exchange) == null) {
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use GET.\"}");
            return;
        }

        Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
        String uuidStr = params.get("uuid");

        try {
            final UUID playerUUID = UUID.fromString(uuidStr);

            // Executa a leitura do inventário na thread principal para garantir a segurança.
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        Player player = Bukkit.getPlayer(playerUUID);

                        if (player == null || !player.isOnline()) {
                            sendJsonResponse(exchange, 404, "{\"error\":\"O jogador precisa estar online para visualizar o inventário.\"}");
                            return;
                        }

                        Map<String, List<Map<String, Object>>> inventories = new HashMap<>();
                        inventories.put("inventory", serializeInventory(player.getInventory().getContents()));
                        inventories.put("enderChest", serializeInventory(player.getEnderChest().getContents()));

                        sendJsonResponse(exchange, 200, gson.toJson(inventories));
                    } catch (IOException e) {
                        plugin.logSevere("Erro de IO ao enviar inventário do jogador via API: " + e.getMessage());
                    }
                }
            }.runTask(plugin);
        } catch (IllegalArgumentException e) {
            sendJsonResponse(exchange, 400, "{\"error\":\"UUID fornecido é inválido.\"}");
        }
    }

    private List<Map<String, Object>> serializeInventory(ItemStack[] contents) {
        List<Map<String, Object>> serializedItems = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            Map<String, Object> itemData = new HashMap<>();
            itemData.put("slot", i);
            itemData.put("material", item.getType().name());
            itemData.put("amount", item.getAmount());

            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName()) {
                    itemData.put("displayName", meta.getDisplayName());
                }
                if (meta.hasLore()) {
                    itemData.put("lore", meta.getLore());
                }
                if (meta.hasEnchants()) {
                    itemData.put("enchantments", meta.getEnchants().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getKey().toString(), Map.Entry::getValue)));
                }
            }
            serializedItems.add(itemData);
        }
        return serializedItems;
    }

    private void handleAdminBroadcastRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (verifyAdminToken(exchange) == null) {
            return;
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use POST.\"}");
            return;
        }

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            @SuppressWarnings("unchecked")
            Map<String, String> broadcastData = gson.fromJson(requestBody, Map.class);
            String message = broadcastData.get("message");

            if (message == null || message.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"success\":false, \"error\":\"A mensagem não pode ser vazia.\"}");
                return;
            }

            // Executa na thread principal do servidor
            new BukkitRunnable() {
                @Override
                public void run() {
                    String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
                    Bukkit.broadcast(Component.text(formattedMessage));
                }
            }.runTask(plugin);

            sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Anúncio enviado com sucesso.\"}");
        } catch (Exception e) {
            plugin.logSevere("Erro ao processar anúncio via API: " + e.getMessage());
            sendJsonResponse(exchange, 500, "{\"success\":false, \"error\":\"Erro interno do servidor.\"}");
        }
    }

    private void handleAdminEconomyStatsRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (verifyAdminToken(exchange) == null) {
            return;
        }

        if (plugin.getEconomy() == null) {
            sendJsonResponse(exchange, 503, "{\"error\":\"Vault ou um plugin de economia não está instalado.\"}");
            return;
        }

        sendJsonResponse(exchange, 200, gson.toJson(economyStatsCache));
    }

    private void scheduleEconomyStatsUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getEconomy() == null) {
                    plugin.logWarn("Vault/Economy não encontrado. Pulando atualização de estatísticas da economia.");
                    return;
                }
                // Otimização: Só executa a lógica pesada se houver jogadores online.
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    plugin.logInfo("Nenhum jogador online. Pulando atualização do cache de economia.");
                    return;
                }
                plugin.logInfo("Atualizando cache de estatísticas da economia...");

                List<Map<String, Object>> richestPlayers = new ArrayList<>();
                double totalEconomy = 0;

                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName() == null) {
                        continue;
                    }
                    double balance = plugin.getEconomy().getBalance(player);
                    if (balance > 0) {
                        totalEconomy += balance;
                        richestPlayers.add(Map.of(
                                "name", player.getName(),
                                "uuid", player.getUniqueId().toString(),
                                "balance", balance
                        ));
                    }
                }

                // Ordena e limita a lista
                richestPlayers.sort((p1, p2) -> Double.compare((double) p2.get("balance"), (double) p1.get("balance")));
                List<Map<String, Object>> topPlayers = richestPlayers.stream().limit(5).collect(Collectors.toList());

                economyStatsCache.put("totalTotems", totalEconomy);
                economyStatsCache.put("richestPlayers", topPlayers);
                economyStatsCache.put("lastUpdated", System.currentTimeMillis());

                plugin.logInfo("Cache de estatísticas da economia atualizado.");
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60 * 60); // 1 min de atraso, atualiza a cada hora
    }

    private void handleAdminGameChatRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (verifyAdminToken(exchange) == null) {
            return;
        }

        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use GET.\"}");
            return;
        }

        Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
        long since = 0;
        if (params.containsKey("since")) {
            try {
                since = Long.parseLong(params.get("since"));
            } catch (NumberFormatException e) {
                // Ignora o parâmetro 'since' se for inválido
            }
        }

        final long finalSince = since;
        List<Map<String, Object>> newMessages = chatHistory.stream()
                .filter(msg -> (long) msg.get("timestamp") > finalSince)
                .collect(Collectors.toList());

        sendJsonResponse(exchange, 200, gson.toJson(newMessages));
    }

    public void addChatMessage(Player player, String message) {
        if (chatHistory.size() >= MAX_CHAT_HISTORY) {
            chatHistory.remove(0);
        }
        Map<String, Object> chatMessage = new HashMap<>();
        chatMessage.put("uuid", player.getUniqueId().toString());
        chatMessage.put("name", player.getName());
        // Remove códigos de cor da mensagem para exibição limpa no painel
        chatMessage.put("message", ChatColor.stripColor(message));
        chatMessage.put("timestamp", System.currentTimeMillis());
        chatHistory.add(chatMessage);
    }

    private void handleAdminExecuteCommandRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Verifica se o token de admin é válido
        if (verifyAdminToken(exchange) == null) {
            return; // O erro já foi enviado
        }

        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use POST.\"}");
            return;
        }

        try {
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            @SuppressWarnings("unchecked")
            Map<String, String> commandData = gson.fromJson(requestBody, Map.class);
            String commandToRun = commandData.get("command");

            if (commandToRun == null || commandToRun.trim().isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"success\":false, \"error\":\"Comando não pode ser vazio.\"}");
                return;
            }

            // Executa o comando na thread principal do servidor
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.logInfo("Executando comando via Console Remoto da API: /" + commandToRun);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
                }
            }.runTask(plugin);

            sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Comando '/" + commandToRun + "' enviado para execução.\"}");
        } catch (Exception e) {
            plugin.logSevere("Erro ao processar comando via API: " + e.getMessage());
            sendJsonResponse(exchange, 500, "{\"success\":false, \"error\":\"Erro interno do servidor.\"}");
        }
    }

    /**
     * Verifica o token JWT do cabeçalho de autorização.
     *
     * @param exchange O objeto da requisição HTTP.
     * @return O token decodificado se for válido, ou null caso contrário. Envia
     * a resposta de erro diretamente.
     */
    private DecodedJWT verifyAdminToken(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJsonResponse(exchange, 401, "{\"error\":\"Token de autorização ausente ou mal formatado.\"}");
            return null;
        }

        String token = authHeader.substring(7); // Remove "Bearer "

        try {
            JWTVerifier verifier = JWT.require(jwtAlgorithm)
                    .withIssuer("MCTrilhas")
                    .build();
            return verifier.verify(token);
        } catch (JWTVerificationException exception) {
            // Token inválido (expirado, assinatura incorreta, etc.)
            sendJsonResponse(exchange, 401, "{\"error\":\"Token inválido ou expirado. Faça login novamente.\"}");
            return null;
        }
    }

    private void handlePlayerDataRequest(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, "{\"error\":\"Método não permitido. Use GET.\"}");
            return;
        }

        Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
        String token = params.get("token");

        if (token == null || token.isEmpty()) {
            sendJsonResponse(exchange, 400, "{\"error\":\"Token não fornecido.\"}");
            return;
        }

        final String finalToken = token;
        // Executa a busca de dados de forma assíncrona para não bloquear a thread do servidor web.
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // --- Lógica de Cache ---
                    long cacheDuration = plugin.getConfig().getLong("web-api.player-data-cache-seconds", 60) * 1000;
                    CachedPlayerResponse cachedResponse = playerResponseCache.get(finalToken);

                    if (cachedResponse != null && (System.currentTimeMillis() - cachedResponse.timestamp < cacheDuration)) {
                        // Cache válido, envia a resposta armazenada
                        sendJsonResponse(exchange, 200, cachedResponse.jsonResponse);
                        return;
                    }

                    UUID playerUUID = plugin.getPlayerDataManager().getPlayerUUIDByToken(finalToken);
                    if (playerUUID == null) {
                        sendJsonResponse(exchange, 404, "{\"error\":\"Token inválido ou jogador não encontrado.\"}");
                        return;
                    }

                    Map<String, Object> responseData = buildPlayerDetailsMap(playerUUID);
                    if (responseData == null) {
                        sendJsonResponse(exchange, 500, "{\"error\":\"Não foi possível carregar os dados do jogador.\"}");
                        return;
                    }

                    String jsonResponse = gson.toJson(responseData);
                    // Armazena a nova resposta no cache
                    playerResponseCache.put(finalToken, new CachedPlayerResponse(jsonResponse));

                    sendJsonResponse(exchange, 200, jsonResponse);
                } catch (IOException e) {
                    plugin.logSevere("Erro de IO ao enviar dados do jogador via API: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleAdminListAdminsRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (verifyAdminToken(exchange) == null) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    List<Map<String, String>> admins = new ArrayList<>();
                    for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                        if (player.isOp() || (player.getPlayer() != null && player.getPlayer().hasPermission("mctrilhas.admin"))) {
                            Map<String, String> adminData = new HashMap<>();
                            adminData.put("name", player.getName());
                            adminData.put("uuid", player.getUniqueId().toString());
                            adminData.put("isOnline", String.valueOf(player.isOnline()));
                            admins.add(adminData);
                        }
                    }
                    sendJsonResponse(exchange, 200, gson.toJson(admins));
                } catch (IOException e) {
                    plugin.logSevere("Erro de IO ao listar administradores via API: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleAdminRankDistributionRequest(HttpExchange exchange) throws IOException {
        // Configura CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (verifyAdminToken(exchange) == null) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Map<Rank, Integer> rankCounts = new EnumMap<>(Rank.class);
                    File playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
                    File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

                    if (playerFiles != null) {
                        for (File playerFile : playerFiles) {
                            FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(playerFile);
                            String rankStr = config.getString("rank", "VISITANTE");
                            try {
                                Rank rank = Rank.valueOf(rankStr.toUpperCase());
                                rankCounts.put(rank, rankCounts.getOrDefault(rank, 0) + 1);
                            } catch (IllegalArgumentException ignored) {
                                // Ignora ranques inválidos nos arquivos
                            }
                        }
                    }
                    sendJsonResponse(exchange, 200, gson.toJson(rankCounts));
                } catch (IOException e) {
                    plugin.logSevere("Erro de IO ao calcular distribuição de ranques via API: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private Map<String, Object> buildPlayerDetailsMap(UUID playerUUID) {
        // Tenta obter os dados do cache (para jogadores online).
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);

        // Se não estiver no cache, o jogador está offline. Carrega os dados de forma segura,
        // sem interferir no cache de jogadores online.
        if (playerData == null) {
            playerData = plugin.getPlayerDataManager().loadOfflinePlayerData(playerUUID);
        }

        if (playerData == null) {
            return null;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        PlayerCTFStats ctfStats = plugin.getPlayerDataManager().getPlayerCTFStats(playerUUID);
        PlayerDuelStats duelStats = plugin.getPlayerDataManager().getPlayerDuelStats(playerUUID);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("name", offlinePlayer.getName());
        responseData.put("uuid", playerUUID.toString());
        responseData.put("rank", playerData.getRank().name());
        responseData.put("playtimeHours", playerData.getActivePlaytimeTicks() / 72000);
        responseData.put("isBedrock", playerData.isBedrockPlayer());
        responseData.put("totems", plugin.getEconomy() != null ? plugin.getEconomy().getBalance(offlinePlayer) : 0);
        responseData.put("ctfStats", ctfStats); // O objeto já é serializável pelo Gson
        responseData.put("duelStats", duelStats); // Adiciona as estatísticas de duelo
        responseData.put("webToken", playerData.getWebAccessToken()); // Adiciona o token para o botão do painel
        responseData.put("pluginVersion", plugin.getDescription().getVersion()); // Adiciona a versão do plugin

        // Adiciona o progresso atual das insígnias
        Map<String, Double> badgeProgress = new HashMap<>();
        playerData.getProgressMap().forEach((badgeType, progress) -> {
            badgeProgress.put(badgeType.name(), progress);
        });
        responseData.put("badgeProgress", badgeProgress);

        // Adiciona as insígnias conquistadas
        // Obtém a lista de IDs das insígnias conquistadas a partir do mapa.
        List<String> earnedBadges = new ArrayList<>(playerData.getEarnedBadgesMap().keySet());
        responseData.put("earnedBadges", earnedBadges);

        // Adiciona as definições completas das insígnias, que incluem os requisitos
        List<Map<String, Object>> badgeDefinitions = plugin.getBadgeManager().getAllBadges().stream()
                .map(badge -> {
                    // Usar HashMap em vez de Map.of() para evitar problemas de inferência de tipo
                    // com tipos mistos (String, double), que podem causar erros de compilação.
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", badge.id());
                    map.put("name", badge.name());
                    map.put("requirement", badge.requirement());
                    map.put("type", badge.type().name());
                    return map;
                }).collect(Collectors.toList());
        responseData.put("badgeDefinitions", badgeDefinitions);

        return responseData;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = jsonResponse.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private Map<String, String> queryToMap(String query) {
        if (query == null) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            }
        }
        return result;
    }

    public void stop() {
        stopBadgeCacheUpdater();
        if (server != null) {
            server.stop(0);
            plugin.logInfo("Servidor da API web parado.");
        }

    }

    public void updateAllLeaderboardCaches() {
        plugin.logInfo("Atualizando caches dos rankings para a API Web...");
        // Atualiza rankings de insígnias
        plugin.getPlayerDataManager().getDailyBadgeCountsAsync().thenAccept(counts -> dailyLeaderboardCache.putAll(formatLeaderboard(counts)));
        plugin.getPlayerDataManager().getMonthlyBadgeCountsAsync().thenAccept(counts -> monthlyLeaderboardCache.putAll(formatLeaderboard(counts)));
        plugin.getPlayerDataManager().getAllTimeBadgeCountsAsync().thenAccept(counts -> allTimeLeaderboardCache.putAll(formatLeaderboard(counts)));
        // Atualiza rankings de CTF
        updateCtfLeaderboardCaches();
        updateEloLeaderboardCaches();
    }

    public void updateCtfLeaderboardCaches() {
        plugin.getPlayerDataManager().getAllPlayerCTFStatsAsync().thenAccept(allStats -> {
            ctfWinsLeaderboardCache.clear();
            ctfKillsLeaderboardCache.clear();
            ctfCapturesLeaderboardCache.clear();

            ctfWinsLeaderboardCache.putAll(sortAndLimit(allStats, PlayerCTFStats::getWins));
            ctfKillsLeaderboardCache.putAll(sortAndLimit(allStats, PlayerCTFStats::getKills));
            ctfCapturesLeaderboardCache.putAll(sortAndLimit(allStats, PlayerCTFStats::getFlagCaptures));
            plugin.logInfo("Caches de ranking do CTF atualizados.");
        });
    }

    public void updateEloLeaderboardCaches() {
        plugin.getPlayerDataManager().getAllPlayerDuelStatsAsync().thenAccept(allStats -> {
            eloLeaderboardCache.clear();
            eloLeaderboardCache.putAll(sortAndLimitElo(allStats));
            plugin.logInfo("Cache de ranking de ELO atualizado.");
        });
    }

    private Map<String, Object> generateApiDataFromCache() {
        Map<String, Object> apiData = new HashMap<>();
        apiData.put("serverStatus", Map.of("onlinePlayers", Bukkit.getOnlinePlayers().size(), "maxPlayers", Bukkit.getMaxPlayers()));
        apiData.put("leaderboards", Map.of("allTime", allTimeLeaderboardCache, "monthly", monthlyLeaderboardCache, "daily", dailyLeaderboardCache));
        apiData.put("ctfLeaderboards", Map.of("wins", ctfWinsLeaderboardCache, "kills", ctfKillsLeaderboardCache, "captures", ctfCapturesLeaderboardCache));
        apiData.put("eloLeaderboard", eloLeaderboardCache);
        apiData.put("activityHistory", activityHistory);
        apiData.put("serverInfo", Map.of("rules", plugin.getConfig().getStringList("server-rules")));
        apiData.put("lastUpdated", System.currentTimeMillis());
        return apiData;
    }

    private Map<String, Integer> formatLeaderboard(Map<UUID, Integer> rawData) {
        List<String> hiddenUuids = plugin.getConfig().getStringList("privacy-settings.hide-from-leaderboards");
        return rawData.entrySet().stream()
                .filter(entry -> !hiddenUuids.contains(entry.getKey().toString()))
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10) // Limita ao top 10
                .collect(Collectors.toMap(entry -> Bukkit.getOfflinePlayer(entry.getKey()).getName(), Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Integer> sortAndLimit(Map<UUID, PlayerCTFStats> statsMap, java.util.function.Function<PlayerCTFStats, Integer> valueExtractor) {
        List<String> hiddenUuids = plugin.getConfig().getStringList("privacy-settings.hide-from-leaderboards");
        return statsMap.entrySet().stream()
                .filter(entry -> !hiddenUuids.contains(entry.getKey().toString()))
                .sorted(Map.Entry.comparingByValue(Comparator.comparing(valueExtractor).reversed()))
                .limit(10) // Limita ao top 10
                .collect(Collectors.toMap(
                        entry -> {
                            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                            return name != null ? name : entry.getKey().toString().substring(0, 8);
                        },
                        entry -> valueExtractor.apply(entry.getValue()),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Integer> sortAndLimitElo(Map<UUID, PlayerDuelStats> statsMap) {
        List<String> hiddenUuids = plugin.getConfig().getStringList("privacy-settings.hide-from-leaderboards");
        return statsMap.entrySet().stream()
                .filter(entry -> !hiddenUuids.contains(entry.getKey().toString()))
                .sorted(Map.Entry.comparingByValue(Comparator.comparing(PlayerDuelStats::getElo).reversed()))
                .limit(10)
                .collect(Collectors.toMap(
                        entry -> {
                            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                            return name != null ? name : entry.getKey().toString().substring(0, 8);
                        },
                        entry -> entry.getValue().getElo(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private void scheduleActivitySnapshot() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activityHistory.size() >= MAX_HISTORY_POINTS) {
                    activityHistory.remove(0);
                }
                activityHistory.add(Map.of("timestamp", System.currentTimeMillis(), "players", Bukkit.getOnlinePlayers().size()));
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20 * 60 * 10); // a cada 10 minutos
    }

    /**
     * Classe interna para armazenar a resposta JSON em cache com um timestamp.
     */
    private static class CachedPlayerResponse {

        final String jsonResponse;
        final long timestamp;

        CachedPlayerResponse(String jsonResponse) {
            this.jsonResponse = jsonResponse;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Inicia a tarefa agendada para atualizar o cache de ranking de insígnias para a API web.
     */
    public void startBadgeCacheUpdater() {
        if (badgeCacheUpdater != null && !badgeCacheUpdater.isCancelled()) {
            return; // Tarefa já está rodando.
        }
        badgeCacheUpdater = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getPlayerDataManager().getDailyBadgeCountsAsync().thenAccept(counts -> dailyLeaderboardCache.putAll(formatLeaderboard(counts)));
                plugin.getPlayerDataManager().getMonthlyBadgeCountsAsync().thenAccept(counts -> monthlyLeaderboardCache.putAll(formatLeaderboard(counts)));
                plugin.getPlayerDataManager().getAllTimeBadgeCountsAsync().thenAccept(counts -> allTimeLeaderboardCache.putAll(formatLeaderboard(counts)));
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 10, 20L * 60 * 5); // Inicia após 10s, repete a cada 5 min
    }

    public void stopBadgeCacheUpdater() {
        if (badgeCacheUpdater != null) {
            badgeCacheUpdater.cancel();
            badgeCacheUpdater = null;
        }
    }
}
