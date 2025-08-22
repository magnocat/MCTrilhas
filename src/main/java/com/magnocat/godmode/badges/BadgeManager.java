package com.magnocat.godmode.badges;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class BadgeManager {
    private final Map<String, Badge> badges = new HashMap<>();

    public void loadBadges(JavaPlugin plugin) {
        badges.clear(); // Essencial para recarregar a configuração (ex: /reload)
        plugin.getLogger().info("Carregando insígnias do config.yml...");

        ConfigurationSection badgeSection = plugin.getConfig().getConfigurationSection("badges");
        if (badgeSection == null) {
            plugin.getLogger().warning("A seção 'badges' não foi encontrada no config.yml. Nenhuma insígnia será carregada.");
            return;
        }

        int loadedCount = 0;
        for (String badgeId : badgeSection.getKeys(false)) {
            // Usar getConfigurationSection para cada insígnia torna o código mais limpo
            ConfigurationSection currentBadgeConfig = badgeSection.getConfigurationSection(badgeId);
            if (currentBadgeConfig == null) continue;

            // Validação de campos obrigatórios para evitar erros
            String name = currentBadgeConfig.getString("name");
            if (name == null || name.isEmpty()) {
                plugin.getLogger().warning("A insígnia '" + badgeId + "' foi ignorada: o campo 'name' está ausente ou vazio.");
                continue;
            }
            if (!currentBadgeConfig.contains("required-progress")) {
                plugin.getLogger().warning("A insígnia '" + badgeId + "' (" + name + ") foi ignorada: o campo 'required-progress' está ausente.");
                continue;
            }

            // Carrega os dados, usando valores padrão para campos opcionais
            String description = currentBadgeConfig.getString("description", "Nenhuma descrição.");
            int rewardTotems = currentBadgeConfig.getInt("reward-totems", 0);
            String rewardItem = currentBadgeConfig.getString("reward-item", "");
            int rewardAmount = currentBadgeConfig.getInt("reward-amount", 1);
            String rewardRegion = currentBadgeConfig.getString("reward-region", "");
            int requiredProgress = currentBadgeConfig.getInt("required-progress");

            Badge badge = new Badge(badgeId, name, description, rewardTotems, rewardItem, rewardAmount, rewardRegion, requiredProgress);
            badges.put(badgeId, badge);
            loadedCount++;
        }

        plugin.getLogger().info(loadedCount + " insígnia(s) carregada(s) com sucesso.");
    }

    public Map<String, Badge> getBadges() {
        return badges;
    }
}
