package com.magnocat.mctrilhas.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class UpdateChecker {

    private final MCTrilhasPlugin plugin;
    private final String githubRepo; // Formato "usuario/repositorio"

    public UpdateChecker(MCTrilhasPlugin plugin, String githubRepo) {
        this.plugin = plugin;
        this.githubRepo = githubRepo;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.github.com/repos/" + githubRepo + "/releases/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (connection.getResponseCode() != 200) {
                    plugin.getLogger().warning("Não foi possível verificar por atualizações. Código de resposta: " + connection.getResponseCode());
                    return;
                }

                JsonObject releaseInfo = JsonParser.parseReader(new InputStreamReader(connection.getInputStream())).getAsJsonObject();
                String latestVersion = releaseInfo.get("tag_name").getAsString().replace("v", "");
                String currentVersion = plugin.getDescription().getVersion();

                if (isNewerVersion(latestVersion, currentVersion)) {
                    plugin.getLogger().info("Uma nova versão está disponível: " + latestVersion + " (Você está na " + currentVersion + ")");

                    String downloadUrl = releaseInfo.getAsJsonArray("assets").get(0).getAsJsonObject().get("browser_download_url").getAsString();
                    String fileName = releaseInfo.getAsJsonArray("assets").get(0).getAsJsonObject().get("name").getAsString();

                    downloadUpdate(downloadUrl, fileName);
                } else {
                    plugin.getLogger().info("Você está com a versão mais recente do MCTrilhas (" + currentVersion + ").");
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao verificar por atualizações: " + e.getMessage());
            }
        });
    }

    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        String[] latestParts = latestVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        int length = Math.max(latestParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;

            if (latestPart > currentPart) return true;
            if (latestPart < currentPart) return false;
        }
        return false; // Versões são iguais
    }

    private void downloadUpdate(String downloadUrl, String fileName) {
        try (InputStream in = new URL(downloadUrl).openStream();
             ReadableByteChannel rbc = Channels.newChannel(in);
             FileOutputStream fos = new FileOutputStream(new File(plugin.getDataFolder().getParentFile(), fileName))) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            plugin.getLogger().info("A nova versão (" + fileName + ") foi baixada para a pasta 'plugins'.");
            plugin.getLogger().info("Por favor, reinicie o servidor para aplicar a atualização.");
        } catch (Exception e) {
            plugin.getLogger().severe("Falha ao baixar a atualização: " + e.getMessage());
        }
    }
}