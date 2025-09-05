package com.magnocat.mctrilhas.web;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class WebDataManager {

    private final MCTrilhasPlugin plugin;
    private final File webDataFolder;

    public WebDataManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        // Assume que a pasta web do TinyServer está em 'plugins/TinyServer/web/'
        // E criamos uma subpasta 'data' para nossos arquivos.
        File tinyServerWebRoot = new File(plugin.getDataFolder().getParentFile(), "TinyServer/web");
        this.webDataFolder = new File(tinyServerWebRoot, "data");
        if (!webDataFolder.exists()) {
            webDataFolder.mkdirs();
        }
    }

    /**
     * Gera e salva o arquivo JSON do ranking de jogadores.
     * @param fileName O nome do arquivo JSON a ser gerado (ex: "leaderboard-daily.json").
     * @param futureCounts O CompletableFuture que fornecerá os dados do ranking.
     */
    private void generateLeaderboardJson(String fileName, CompletableFuture<Map<UUID, Integer>> futureCounts) {
        futureCounts.thenAcceptAsync(badgeCounts -> {
            if (badgeCounts == null || badgeCounts.isEmpty()) {
                // Se não houver dados, gera um JSON vazio para não quebrar a página.
                writeJsonToFile(fileName, "[]");
                return;
            }

            // Constrói o JSON manualmente para não adicionar dependências pesadas.
            StringBuilder jsonBuilder = new StringBuilder("[\n");
            AtomicInteger rank = new AtomicInteger(1);

            // Ordena o mapa por contagem de insígnias (decrescente) e limita ao top 10.
            badgeCounts.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(10)
                    .forEach(entry -> {
                        UUID uuid = entry.getKey();
                        int count = entry.getValue();
                        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                        String playerName = player.getName() != null ? player.getName() : "Desconhecido";

                        if (jsonBuilder.length() > 2) { // Adiciona vírgula antes de cada objeto, exceto o primeiro.
                            jsonBuilder.append(",\n");
                        }

                        jsonBuilder.append("  {\n");
                        jsonBuilder.append("    \"rank\": ").append(rank.getAndIncrement()).append(",\n");
                        jsonBuilder.append("    \"name\": \"").append(escapeJson(playerName)).append("\",\n");
                        jsonBuilder.append("    \"badges\": ").append(count).append("\n");
                        jsonBuilder.append("  }");
                    });

            jsonBuilder.append("\n]");
            writeJsonToFile(fileName, jsonBuilder.toString());
        }).exceptionally(ex -> {
            plugin.getLogger().severe("Erro ao gerar dados para o ranking '" + fileName + "': " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private void writeJsonToFile(String fileName, String jsonContent) {
        File file = new File(webDataFolder, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonContent);
            plugin.getLogger().info("Arquivo '" + fileName + "' gerado com sucesso.");
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível salvar o arquivo '" + fileName + "': " + e.getMessage());
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public void scheduleUpdates() {
        // Ranking Diário: atualiza a cada 30 minutos (36000 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> generateLeaderboardJson("leaderboard-daily.json", plugin.getPlayerDataManager().getDailyBadgeCountsAsync()), 20L, 36000L);
        // Ranking Mensal: atualiza a cada 4 horas (28800 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> generateLeaderboardJson("leaderboard-monthly.json", plugin.getPlayerDataManager().getMonthlyBadgeCountsAsync()), 40L, 28800L);
        // Ranking Geral (Anual/Total): atualiza a cada 24 horas (1728000 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> generateLeaderboardJson("leaderboard-all-time.json", plugin.getPlayerDataManager().getAllTimeBadgeCountsAsync()), 60L, 1728000L);
    }

    /**
     * Força a geração de todos os arquivos de ranking imediatamente.
     */
    public void forceGenerateAllRankings() {
        plugin.getLogger().info("Geração manual de rankings iniciada por um administrador.");
        generateLeaderboardJson("leaderboard-daily.json", plugin.getPlayerDataManager().getDailyBadgeCountsAsync());
        generateLeaderboardJson("leaderboard-monthly.json", plugin.getPlayerDataManager().getMonthlyBadgeCountsAsync());
        generateLeaderboardJson("leaderboard-all-time.json", plugin.getPlayerDataManager().getAllTimeBadgeCountsAsync());
    }
}