package com.magnocat.godmode.updater;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class UpdateChecker {

    private final GodModePlugin plugin;
    private final String repository; // e.g., "magnocat/GodMode-MCTrilhas"

    public UpdateChecker(GodModePlugin plugin, String repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    /**
     * Checks for a new version on GitHub asynchronously.
     */
    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            try {
                plugin.getLogger().info("Verificando por novas atualizações...");

                URL url = new URL("https://api.github.com/repos/" + this.repository + "/releases/latest");

                String jsonContent;
                try (Scanner scanner = new Scanner(url.openStream())) {
                    jsonContent = scanner.useDelimiter("\\A").next();
                }

                String latestVersionTag = getJsonValue(jsonContent, "tag_name");

                if (latestVersionTag == null) {
                    plugin.getLogger().warning("Não foi possível encontrar a versão da última release na API do GitHub.");
                    return;
                }

                String currentVersion = plugin.getDescription().getVersion();

                // Compare versions. Assumes tag is "v" + version (e.g., "v1.0.0").
                if (!latestVersionTag.equals("v" + currentVersion)) {
                    plugin.getLogger().info("Uma nova versão foi encontrada: " + latestVersionTag);
                    plugin.getLogger().info("A sua versão é: " + currentVersion);
                    plugin.getLogger().info("A nova versão será baixada e instalada na próxima reinicialização.");

                    String downloadUrl = getJsonValue(jsonContent, "browser_download_url");
                    if (downloadUrl != null && downloadUrl.endsWith(".jar")) {
                        downloadUpdate(downloadUrl);
                    } else {
                        plugin.getLogger().warning("Não foi possível encontrar o link de download do .jar na API.");
                    }
                } else {
                    plugin.getLogger().info("Você já está com a versão mais recente do plugin (" + currentVersion + ").");
                }

            } catch (IOException e) {
                plugin.getLogger().warning("Não foi possível conectar à API do GitHub para verificar atualizações: " + e.getMessage());
            } catch (Exception e) {
                plugin.getLogger().warning("Ocorreu um erro inesperado ao verificar por atualizações.");
                e.printStackTrace();
            }
        });
    }

    private String getJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        int valueStartIndex = keyIndex + searchKey.length();
        int valueEndIndex = json.indexOf("\"", valueStartIndex);
        if (valueEndIndex == -1) return null;
        return json.substring(valueStartIndex, valueEndIndex);
    }

    private void downloadUpdate(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);

        // The 'update' folder is where Spigot/Paper automatically installs plugins from on restart.
        File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
        if (!updateFolder.exists()) {
            updateFolder.mkdirs();
        }

        // The destination file. Using the plugin's name from plugin.yml ensures it's correct.
        File destinationFile = new File(updateFolder, plugin.getDescription().getName() + ".jar");

        try (InputStream in = url.openStream()) {
            // Use Java NIO for efficient and safe file copying.
            Files.copy(in, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}