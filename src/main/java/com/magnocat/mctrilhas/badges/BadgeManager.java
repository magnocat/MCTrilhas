package com.magnocat.mctrilhas.badges;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the loading and retrieval of badge definitions from badges.yml.
 */
@SuppressWarnings("deprecation")
public class BadgeManager {

    private final MCTrilhasPlugin plugin;
    private final Map<String, Badge> badges = new LinkedHashMap<>();

    /**
     * Constructs a new BadgeManager.
     * @param plugin The main plugin instance.
     */
    public BadgeManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        loadBadgesFromConfig();
    }

    /**
     * Carrega ou recarrega todas as definições de insígnias do arquivo badges.yml.
     */
    public void loadBadgesFromConfig() {
        badges.clear();
        FileConfiguration config = plugin.getBadgeConfigManager().getBadgeConfig();

        // Garante que a seção 'badges' exista antes de tentar lê-la.
        if (!config.isConfigurationSection("badges")) {
            plugin.getLogger().warning("A seção 'badges' não foi encontrada no config.yml. Nenhuma insígnia será carregada.");
            return;
        }

        // Pega as chaves apenas de dentro da seção 'badges'.
        Set<String> badgeIds = config.getConfigurationSection("badges").getKeys(false);

        for (String id : badgeIds) {
            String path = "badges." + id;
            try {
                // O tipo da insígnia agora é o próprio ID da chave.
                // Isso também serve para filtrar chaves que não são insígnias (como 'use-gui').
                BadgeType type = BadgeType.valueOf(id.toUpperCase());

                String name = config.getString(path + ".name");
                String description = config.getString(path + ".description");
                double requirement = config.getDouble(path + ".required-progress");
                String icon = config.getString(path + ".reward-item-data.material", "BARRIER");

                if (name == null || description == null) {
                    plugin.getLogger().warning("A insígnia '" + id + "' está faltando o nome ou a descrição e não será carregada.");
                    continue;
                }

                Badge badge = new Badge(id, name, description, type, requirement, icon);
                badges.put(id.toLowerCase(), badge);
            } catch (IllegalArgumentException e) {
                // Ignora chaves que não são tipos de insígnia válidos (ex: 'use-gui').
            } catch (Exception e) {
                plugin.getLogger().severe("Ocorreu um erro inesperado ao carregar a insígnia '" + id + "': " + e.getMessage());
            }
        }
        plugin.getLogger().info(badges.size() + " insígnias foram carregadas.");
    }

    public Badge getBadge(String id) {
        return badges.get(id.toLowerCase());
    }

    public List<Badge> getAllBadges() {
        return Collections.unmodifiableList(new ArrayList<>(badges.values()));
    }
}