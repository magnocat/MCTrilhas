package com.magnocat.mctrilhas.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerCTFStats;
import com.magnocat.mctrilhas.data.PlayerData;
import com.sun.net.httpserver.HttpServer;

public class HttpApiManager {

    private final MCTrilhasPlugin plugin;
    private HttpServer server;
    private final Gson gson = new GsonBuilder().create();

    private final List<Map<String, Object>> activityHistory = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY_POINTS = 144; // 144 pontos * 10 min = 24 horas

    // Caches para os rankings
    private final Map<String, Integer> dailyLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> monthlyLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> allTimeLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> ctfWinsLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> ctfKillsLeaderboardCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> ctfCapturesLeaderboardCache = new ConcurrentHashMap<>();

    public HttpApiManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("web-api.enabled", false)) {
            plugin.logInfo("API web integrada está desativada na configuração.");
            return;
        }

        scheduleActivitySnapshot();

        int port = plugin.getConfig().getInt("web-api.port", 22222);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Contexto para a API de dados dinâmicos
            server.createContext("/api/v1/data", this::handleApiDataRequest);

            // Contexto para a API de login do admin
            server.createContext("/api/v1/admin/login", this::handleAdminLoginRequest);

            // Contexto para a API de dados de um jogador específico via token
            server.createContext("/api/v1/player", this::handlePlayerDataRequest);

            // Define o diretório raiz de onde os arquivos web serão servidos.
            Path webRoot = new File(plugin.getDataFolder(), "web").toPath();

            // Extrai os arquivos web do .jar para a pasta de dados do plugin, garantindo que existam.
            // O 'true' garante que eles sejam sobrescritos se houver uma nova versão no plugin.
            plugin.saveResource("web/index.html", true);
            plugin.saveResource("web/admin/login.html", true);
            plugin.saveResource("web/admin/player_dashboard.html", true);
            plugin.saveResource("web/404.html", true);

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

    private void handleApiDataRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
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

    private void handleAdminLoginRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
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

            // Obtém credenciais do config.yml
            String requiredUser = plugin.getConfig().getString("web-api.admin-credentials.username", "admin");
            String requiredPass = plugin.getConfig().getString("web-api.admin-credentials.password", "changeme");

            if (requiredUser.equals(providedUser) && requiredPass.equals(providedPass)) {
                // Sucesso! Futuramente, aqui geraríamos um token de sessão.
                sendJsonResponse(exchange, 200, "{\"success\":true, \"message\":\"Login bem-sucedido!\"}");
            } else {
                // Falha
                sendJsonResponse(exchange, 401, "{\"success\":false, \"error\":\"Usuário ou senha inválidos.\"}");
            }
        } catch (Exception e) {
            plugin.logSevere("Erro ao processar requisição de login: " + e.getMessage());
            sendJsonResponse(exchange, 500, "{\"success\":false, \"error\":\"Erro interno do servidor.\"}");
        }
    }

    private void handlePlayerDataRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
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

        UUID playerUUID = plugin.getPlayerDataManager().getPlayerUUIDByToken(token);
        if (playerUUID == null) {
            sendJsonResponse(exchange, 404, "{\"error\":\"Token inválido ou jogador não encontrado.\"}");
            return;
        }

        // Carrega os dados do jogador. Funciona para online e offline.
        // Se o jogador estiver online, pega do cache. Se offline, carrega do arquivo.
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
        if (playerData == null) {
            // Se o jogador estiver offline, os dados não estarão no cache principal.
            // Precisamos carregá-los temporariamente.
            // A forma mais simples é usar o próprio sistema de carregamento, mas sem manter no cache.
            // Por enquanto, vamos assumir que o sistema de cache de token é suficiente.
            // Se for um problema, precisaremos de um método para carregar dados offline.
            // Para simplificar, vamos carregar e descarregar.
            plugin.getPlayerDataManager().loadPlayerData(playerUUID);
            playerData = plugin.getPlayerDataManager().getPlayerData(playerUUID);
            plugin.getPlayerDataManager().unloadPlayerData(playerUUID); // Remove do cache online
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
        
        // Calcula o progresso das insígnias
        Map<String, Double> badgeProgress = new HashMap<>();
        playerData.getProgressMap().forEach((badgeType, progress) -> {
            badgeProgress.put(badgeType.name(), progress);
        });
        responseData.put("badgeProgress", badgeProgress);

        sendJsonResponse(exchange, 200, gson.toJson(responseData));
    }

    private void sendJsonResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
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
}