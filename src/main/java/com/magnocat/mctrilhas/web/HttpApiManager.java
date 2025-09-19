package com.magnocat.mctrilhas.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.security.CodeSource;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sun.net.httpserver.HttpExchange;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerCTFStats;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.utils.SecurityUtils;
import com.sun.net.httpserver.HttpServer;

public class HttpApiManager {

    private final MCTrilhasPlugin plugin;
    private HttpServer server;
    private final Gson gson = new GsonBuilder().create();
    private Algorithm jwtAlgorithm;

    private final List<Map<String, Object>> activityHistory = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY_POINTS = 144; // 144 pontos * 10 min = 24 horas

    // Caches para os rankings
    private final Map<String, Integer> dailyLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> monthlyLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> allTimeLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> ctfWinsLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> ctfKillsLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> ctfCapturesLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, CachedPlayerResponse> playerResponseCache = new ConcurrentHashMap<>();

    public HttpApiManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("web-api.enabled", false)) {
            plugin.logInfo("API web integrada está desativada na configuração.");
            return;
        }

        extractWebResources();

        setupJwt();

        setupAdminCredentials();

        scheduleActivitySnapshot();

        int port = plugin.getConfig().getInt("web-api.port", 22222);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Contexto para a API de dados dinâmicos
            server.createContext("/api/v1/data", this::handleApiDataRequest);

            // Contexto para a API de login do admin
            server.createContext("/api/v1/admin/login", this::handleAdminLoginRequest);

            // Contexto para um endpoint de admin protegido por JWT
            server.createContext("/api/v1/admin/status", this::handleAdminStatusRequest);

            // Contexto para a lista de jogadores online (protegido por JWT)
            server.createContext("/api/v1/admin/players/online", this::handleAdminOnlinePlayersRequest);

            // Contexto para a API de dados de um jogador específico via token
            server.createContext("/api/v1/player", this::handlePlayerDataRequest);

            // Define o diretório raiz de onde os arquivos web serão servidos.
            Path webRoot = new File(plugin.getDataFolder(), "web").toPath();

            // Usa o novo handler seguro para servir todos os arquivos do diretório web.
            server.createContext("/", new SecureHttpFileHandler(webRoot));

            server.setExecutor(null); // usa o executor padrão
            server.start();
            plugin.logInfo("Servidor da API web iniciado na porta: " + port);

            // Agenda a atualização do cache dos rankings
            long updateInterval = 20L * 60 * 5; // 5 minutos
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::updateAllLeaderboardCaches, 20L * 10, updateInterval);

        } catch (IOException e) {
            plugin.logSevere("Falha ao iniciar o servidor da API web: " + e.getMessage());
        }
    }

    /**
     * Extrai todos os recursos da pasta 'web' do JAR para a pasta de dados do plugin.
     * Isso garante que todos os arquivos do site (HTML, CSS, JS, imagens) estejam disponíveis no disco.
     * A extração não sobrescreve arquivos existentes, permitindo personalização.
     */
    private void extractWebResources() {
        plugin.logInfo("Verificando e extraindo recursos da web...");
        CodeSource src = plugin.getClass().getProtectionDomain().getCodeSource();
        if (src != null) {
            try {
                URL jar = src.getLocation();
                ZipInputStream zip = new ZipInputStream(jar.openStream());
                while (true) {
                    ZipEntry e = zip.getNextEntry();
                    if (e == null) break;
                    String name = e.getName();
                    if (name.startsWith("web/")) {
                        plugin.saveResource(name, false); // false = não sobrescrever
                    }
                }
                plugin.logInfo("Recursos da web extraídos com sucesso.");
            } catch (IOException e) {
                plugin.logSevere("Falha ao extrair recursos da web: " + e.getMessage());
            }
        } else {
            plugin.logWarn("Não foi possível encontrar a fonte do JAR para extrair os recursos da web.");
        }
    }

    /**
     * Verifica as credenciais de administrador no config.yml.
     * Se a senha estiver em texto plano (identificado pela ausência de um 'salt'),
     * ela é criptografada com hash e um novo 'salt' é gerado e salvo.
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
     * Configura o segredo para a assinatura de tokens JWT.
     * Se o segredo não estiver definido no config.yml, um novo é gerado e salvo.
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
                    return playerData;
                })
                .collect(Collectors.toList());

        sendJsonResponse(exchange, 200, gson.toJson(playersList));
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

    /**
     * Verifica o token JWT do cabeçalho de autorização.
     * @param exchange O objeto da requisição HTTP.
     * @return O token decodificado se for válido, ou null caso contrário. Envia a resposta de erro diretamente.
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

        // --- Lógica de Cache ---
        long cacheDuration = plugin.getConfig().getLong("web-api.player-data-cache-seconds", 60) * 1000;
        CachedPlayerResponse cachedResponse = playerResponseCache.get(token);

        if (cachedResponse != null && (System.currentTimeMillis() - cachedResponse.timestamp < cacheDuration)) {
            // Cache válido, envia a resposta armazenada
            sendJsonResponse(exchange, 200, cachedResponse.jsonResponse);
            return;
        }
        // --- Fim da Lógica de Cache ---

        UUID playerUUID = plugin.getPlayerDataManager().getPlayerUUIDByToken(token);
        if (playerUUID == null) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Token inválido ou jogador não encontrado.\"}");
            return;
        }

        // Tenta obter os dados do cache (para jogadores online).
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);

        // Se não estiver no cache, o jogador está offline. Carrega os dados de forma segura,
        // sem interferir no cache de jogadores online.
        if (playerData == null) {
            playerData = plugin.getPlayerDataManager().loadOfflinePlayerData(playerUUID);
        }

        if (playerData == null) {
             sendJsonResponse(exchange, 500, "{\"error\":\"Não foi possível carregar os dados do jogador.\"}");
             return;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        PlayerCTFStats ctfStats = plugin.getPlayerDataManager().getPlayerCTFStats(playerUUID);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("name", offlinePlayer.getName());
        responseData.put("uuid", playerUUID.toString());
        responseData.put("rank", playerData.getRank().name());
        responseData.put("playtimeHours", playerData.getActivePlaytimeTicks() / 72000);
        responseData.put("totems", plugin.getEconomy() != null ? plugin.getEconomy().getBalance(offlinePlayer) : 0);
        responseData.put("ctfStats", ctfStats); // O objeto já é serializável pelo Gson
        
        // Adiciona o progresso atual das insígnias
        Map<String, Double> badgeProgress = new HashMap<>();
        playerData.getProgressMap().forEach((badgeType, progress) -> {
            badgeProgress.put(badgeType.name(), progress);
        });
        responseData.put("badgeProgress", badgeProgress);

        // Adiciona os requisitos de progresso para cada insígnia
        Map<String, Double> badgeRequirements = new HashMap<>();
        for (com.magnocat.mctrilhas.badges.BadgeType badgeType : com.magnocat.mctrilhas.badges.BadgeType.values()) {
            String configKey = plugin.getBadgeConfigManager().getBadgeConfigKey(badgeType.name());
            if (configKey != null) {
                double required = plugin.getBadgeConfigManager().getBadgeConfig().getDouble("badges." + configKey + ".required-progress", 0.0);
                if (required > 0) {
                    badgeRequirements.put(badgeType.name(), required);
                }
            }
        }
        responseData.put("badgeRequirements", badgeRequirements);

        String jsonResponse = gson.toJson(responseData);
        // Armazena a nova resposta no cache
        playerResponseCache.put(token, new CachedPlayerResponse(jsonResponse));

        sendJsonResponse(exchange, 200, jsonResponse);
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
        if (server != null) {
            server.stop(0);
            plugin.logInfo("Servidor da API web parado.");
        }
    }

    private void updateAllLeaderboardCaches() {
        // Otimização: Se não houver jogadores online, não há necessidade de atualizar os rankings.
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            plugin.logInfo("Nenhum jogador online. Pulando atualização dos caches de ranking.");
            return;
        }

        plugin.logInfo("Atualizando caches dos rankings para a API Web...");
        // Atualiza rankings de insígnias
        plugin.getPlayerDataManager().getDailyBadgeCountsAsync().thenAccept(counts -> dailyLeaderboardCache.putAll(formatLeaderboard(counts)));
        plugin.getPlayerDataManager().getMonthlyBadgeCountsAsync().thenAccept(counts -> monthlyLeaderboardCache.putAll(formatLeaderboard(counts)));
        plugin.getPlayerDataManager().getAllTimeBadgeCountsAsync().thenAccept(counts -> allTimeLeaderboardCache.putAll(formatLeaderboard(counts)));
        // Atualiza rankings de CTF
        updateCtfLeaderboardCaches();
    }

    private void updateCtfLeaderboardCaches() {
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

    private Map<String, Object> generateApiDataFromCache() {
        Map<String, Object> apiData = new HashMap<>();
        apiData.put("serverStatus", Map.of("onlinePlayers", Bukkit.getOnlinePlayers().size(), "maxPlayers", Bukkit.getMaxPlayers()));
        apiData.put("leaderboards", Map.of("allTime", allTimeLeaderboardCache, "monthly", monthlyLeaderboardCache, "daily", dailyLeaderboardCache));
        apiData.put("ctfLeaderboards", Map.of("wins", ctfWinsLeaderboardCache, "kills", ctfKillsLeaderboardCache, "captures", ctfCapturesLeaderboardCache));
        apiData.put("activityHistory", activityHistory);
        apiData.put("lastUpdated", System.currentTimeMillis());
        return apiData;
    }

    private Map<String, Integer> formatLeaderboard(Map<UUID, Integer> rawData) {
        return rawData.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10) // Limita ao top 10
                .collect(Collectors.toMap(entry -> Bukkit.getOfflinePlayer(entry.getKey()).getName(), Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Integer> sortAndLimit(Map<UUID, PlayerCTFStats> statsMap, java.util.function.Function<PlayerCTFStats, Integer> valueExtractor) {
        return statsMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, PlayerCTFStats>comparingByValue((s1, s2) -> Integer.compare(valueExtractor.apply(s2), valueExtractor.apply(s1))))
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
}