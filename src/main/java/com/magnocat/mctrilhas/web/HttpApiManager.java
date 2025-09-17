package com.magnocat.mctrilhas.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerCTFStats;
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

            // Contexto para servir arquivos estáticos (o site)
            server.createContext("/", this::handleStaticFileRequest);

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

    private void handleStaticFileRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html"; // Rota padrão
        }

        // Redireciona para a página de login do admin.
        // Futuramente, isso será mais inteligente: se o usuário não estiver logado, vai para login.html. Se estiver, vai para index.html.
        if (path.equals("/admin") || path.equals("/admin/")) {
            path = "/admin/login.html"; // Por enquanto, sempre mostra a página de login.
        }

        // Medida de segurança simples para evitar ataques de "directory traversal"
        if (path.contains("..")) {
            exchange.sendResponseHeaders(400, -1); // Bad Request
            return;
        }

        String resourcePath = "web" + path;
        try (InputStream resourceStream = plugin.getResource(resourcePath)) {
            if (resourceStream == null) {
                exchange.sendResponseHeaders(404, -1); // Not Found
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", getMimeType(path));
            // Envia o cabeçalho com tamanho 0, pois vamos transmitir os dados em chunks.
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                // Transmite o arquivo em pedaços para economizar memória.
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = resourceStream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
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

    private String getMimeType(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot == -1) return "application/octet-stream"; // Tipo genérico

        return switch (path.substring(lastDot + 1).toLowerCase()) {
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "png", "ico" -> "image/png"; // Adicionado .ico
            case "jpg", "jpeg" -> "image/jpeg";
            case "svg" -> "image/svg+xml";
            case "json" -> "application/json"; // Adicionado .json para o manifest
            default -> "text/html"; // Padrão para .html e outros
        };
    }

    private void sendJsonResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = jsonResponse.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
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