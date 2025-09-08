package com.magnocat.mctrilhas.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class HttpApiManager {

    private final MCTrilhasPlugin plugin;
    private HttpServer server;
    private final Gson gson = new GsonBuilder().create();
    private final Map<String, Object> cachedData = new ConcurrentHashMap<>();
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION_MS = 60 * 1000; // 1 minuto

    private final List<Map<String, Object>> activityHistory = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY_POINTS = 144; // 144 pontos * 10 min = 24 horas

    public HttpApiManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("web-api.enabled", false)) {
            plugin.getLogger().info("API web integrada está desativada na configuração.");
            return;
        }

        scheduleActivitySnapshot();

        int port = plugin.getConfig().getInt("web-api.port", 8080);
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Contexto para a API de dados dinâmicos
            server.createContext("/api/v1/data", exchange -> {
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

                // Usa dados do cache se ainda forem válidos
                if (System.currentTimeMillis() - lastCacheTime < CACHE_DURATION_MS) {
                    String jsonResponse = gson.toJson(cachedData);
                    byte[] responseBytes = jsonResponse.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                    return;
                }

                // Se o cache expirou, gera novos dados de forma assíncrona
                generateApiData().thenAccept(data -> {
                    try {
                        cachedData.clear();
                        cachedData.putAll(data);
                        lastCacheTime = System.currentTimeMillis();

                        String jsonResponse = gson.toJson(data);
                        byte[] responseBytes = jsonResponse.getBytes("UTF-8");

                        exchange.sendResponseHeaders(200, responseBytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(responseBytes);
                        }
                    } catch (IOException e) {
                        plugin.getLogger().severe("Erro ao enviar resposta da API: " + e.getMessage());
                    }
                }).exceptionally(ex -> {
                    plugin.getLogger().severe("Erro ao gerar dados para a API: " + ex.getMessage());
                    try {
                        exchange.sendResponseHeaders(500, -1); // Internal Server Error
                    } catch (IOException ignored) {}
                    return null;
                });
            });

            // Contexto para servir arquivos estáticos (o site)
            server.createContext("/", this::handleStaticFileRequest);

            server.setExecutor(null); // usa o executor padrão
            server.start();
            plugin.getLogger().info("Servidor da API web iniciado na porta: " + port);

        } catch (IOException e) {
            plugin.getLogger().severe("Falha ao iniciar o servidor da API web: " + e.getMessage());
        }
    }

    private void handleStaticFileRequest(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html"; // Rota padrão
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
            byte[] content = resourceStream.readAllBytes();
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
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

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Servidor da API web parado.");
        }
    }

    private CompletableFuture<Map<String, Object>> generateApiData() {
        CompletableFuture<Map<UUID, Integer>> allTimeFuture = plugin.getPlayerDataManager().getAllTimeBadgeCountsAsync();
        CompletableFuture<Map<UUID, Integer>> monthlyFuture = plugin.getPlayerDataManager().getMonthlyBadgeCountsAsync();
        CompletableFuture<Map<UUID, Integer>> dailyFuture = plugin.getPlayerDataManager().getDailyBadgeCountsAsync();

        return CompletableFuture.allOf(allTimeFuture, monthlyFuture, dailyFuture).thenApply(v -> {
            Map<String, Object> apiData = new HashMap<>();
            apiData.put("serverStatus", Map.of("onlinePlayers", Bukkit.getOnlinePlayers().size(), "maxPlayers", Bukkit.getMaxPlayers()));
            apiData.put("leaderboards", Map.of("allTime", formatLeaderboard(allTimeFuture.join()), "monthly", formatLeaderboard(monthlyFuture.join()), "daily", formatLeaderboard(dailyFuture.join())));
            apiData.put("activityHistory", activityHistory);
            apiData.put("lastUpdated", System.currentTimeMillis());
            return apiData;
        });
    }

    private Map<String, Integer> formatLeaderboard(Map<UUID, Integer> rawData) {
        return rawData.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(10) // Limita ao top 10
                .collect(Collectors.toMap(entry -> Bukkit.getOfflinePlayer(entry.getKey()).getName(), Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
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