package com.magnocat.mctrilhas.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.magnocat.mctrilhas.MCTrilhasPlugin;

public class WebDataManager {

    private final MCTrilhasPlugin plugin;
    private final File webDataFolder;

    public WebDataManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        // OBS: O padrão do TinyServer é 'htdocs'. Verifique se 'web' é a pasta correta no seu setup.
        // File tinyServerWebRoot = new File(plugin.getDataFolder().getParentFile(), "TinyServer/htdocs");
        // E criamos uma subpasta 'data' para nossos arquivos.
        File tinyServerWebRoot = new File(plugin.getDataFolder().getParentFile(), "TinyServer/htdocs");
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
        // Gravador de contagem de jogadores: roda a cada 10 minutos (12000 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::recordPlayerCount, 100L, 12000L);
        // Limpeza do arquivo de estatísticas: roda a cada 6 horas (43200 ticks * 10 = 432000)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupPlayerStatsFile, 1200L, 432000L);
    }

    /**
     * Força a geração de todos os arquivos de ranking imediatamente.
     */
    public void forceGenerateAllRankings() {
        plugin.getLogger().info("Geração manual de rankings iniciada por um administrador.");
        generateLeaderboardJson("leaderboard-daily.json", plugin.getPlayerDataManager().getDailyBadgeCountsAsync());
        generateLeaderboardJson("leaderboard-monthly.json", plugin.getPlayerDataManager().getMonthlyBadgeCountsAsync());
        generateLeaderboardJson("leaderboard-all-time.json", plugin.getPlayerDataManager().getAllTimeBadgeCountsAsync());
        // Também grava a contagem de jogadores atual
        recordPlayerCount();
    }

    /**
     * Grava a contagem de jogadores atual anexando-a ao arquivo CSV.
     * Esta é uma operação rápida, ideal para ser executada com frequência.
     */
    public void recordPlayerCount() {
        File statsFile = new File(webDataFolder, "player_stats.csv");
        String line = System.currentTimeMillis() + "," + Bukkit.getOnlinePlayers().size() + "\n";
        try {
            // Apenas anexa a nova linha. Muito mais rápido.
            Files.write(statsFile.toPath(), line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível anexar dados ao player_stats.csv: " + e.getMessage());
        }
    }

    /**
     * Limpa o arquivo de estatísticas de jogadores, removendo entradas com mais de 24 horas.
     * Este método é projetado para ser chamado com menos frequência do que recordPlayerCount.
     */
    private void cleanupPlayerStatsFile() {
        File statsFile = new File(webDataFolder, "player_stats.csv");
        if (!statsFile.exists()) {
            return;
        }

        long cutoffTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        List<String> validLines = new java.util.ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(statsFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    long timestamp = Long.parseLong(line.split(",")[0]);
                    if (timestamp >= cutoffTimestamp) {
                        validLines.add(line);
                    }
                } catch (Exception e) { /* Ignora linhas malformadas */ }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Não foi possível ler player_stats.csv para limpeza: " + e.getMessage());
            return; // Aborta se não conseguir ler.
        }

        // Escreve apenas as linhas válidas de volta, limpando o arquivo.
        try {
            Files.write(statsFile.toPath(), validLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            plugin.getLogger().info("Arquivo 'player_stats.csv' limpo com sucesso.");
        } catch (IOException e) {
            plugin.getLogger().severe("Não foi possível escrever em player_stats.csv após a limpeza: " + e.getMessage());
        }
    }
}