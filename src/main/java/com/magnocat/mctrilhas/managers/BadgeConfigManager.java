package com.magnocat.mctrilhas.managers;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @deprecated Esta classe é obsoleta e será removida em futuras versões.
 * <p>
 * Originalmente, esta classe gerenciava um arquivo `badges.yml` separado.
 * Agora que todas as configurações estão centralizadas no `config.yml`, esta
 * classe se tornou uma camada redundante de acesso.
 * <p>
 * **Substituições:**
 * <ul>
 *     <li>Para obter dados de insígnias, use {@link com.magnocat.mctrilhas.badges.BadgeManager}.</li>
 *     <li>Para acesso geral ao `config.yml`, use {@code plugin.getConfig()}.</li>
 * </ul>
 */
@Deprecated
public class BadgeConfigManager {

    private final MCTrilhasPlugin plugin;
    private FileConfiguration badgeConfig = null;
    private File badgeConfigFile = null;

    public BadgeConfigManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        // O nome do arquivo foi padronizado para config.yml.
        this.badgeConfigFile = new File(plugin.getDataFolder(), "config.yml");
        saveDefaultConfig();
        reloadBadgeConfig();
    }

    public void reloadBadgeConfig() {
        if (badgeConfigFile == null) {
            // Garante que o arquivo seja o config.yml.
            badgeConfigFile = new File(plugin.getDataFolder(), "config.yml");
        }
        badgeConfig = YamlConfiguration.loadConfiguration(badgeConfigFile);

        // Carrega os padrões do config.yml empacotado no JAR.
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream != null) {
            badgeConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream)));
        }
    }

    public FileConfiguration getBadgeConfig() {
        if (badgeConfig == null) {
            reloadBadgeConfig();
        }
        return badgeConfig;
    }

    /**
     * Encontra a chave de configuração para uma insígnia, ignorando maiúsculas/minúsculas.
     * @param badgeId O ID da insígnia (ex: "MINING").
     * @return A chave real do config.yml (ex: "MINING") ou null se não for encontrada.
     */
    public String getBadgeConfigKey(String badgeId) {
        if (badgeConfig == null || !badgeConfig.isConfigurationSection("badges")) {
            return null;
        }
        // Procura por uma chave que corresponda ao badgeId, ignorando maiúsculas/minúsculas.
        for (String key : badgeConfig.getConfigurationSection("badges").getKeys(false)) {
            if (key.equalsIgnoreCase(badgeId)) {
                return key; // Retorna a chave exata como está no arquivo (ex: "MINING").
            }
        }
        return null; // Retorna null se nenhuma chave correspondente for encontrada.
    }

    public void saveDefaultConfig() {
        if (badgeConfigFile == null) {
            badgeConfigFile = new File(plugin.getDataFolder(), "config.yml");
        }
        if (!badgeConfigFile.exists()) {
            // Salva o config.yml padrão se ele não existir.
            plugin.saveResource("config.yml", false);
        }
    }
}