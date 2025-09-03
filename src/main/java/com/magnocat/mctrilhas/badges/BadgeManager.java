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
        Set<String> badgeIds = config.getKeys(false);

        for (String id : badgeIds) {
            try {
                String name = config.getString(id + ".name");
                String description = config.getString(id + ".description");
                BadgeType type = BadgeType.valueOf(config.getString(id + ".type", "").toUpperCase());
                double requirement = config.getDouble(id + ".requirement");
                String icon = config.getString(id + ".icon", "BARRIER");

                if (name == null || description == null) {
                    plugin.getLogger().warning("A insígnia '" + id + "' está faltando o nome ou a descrição e não será carregada.");
                    continue;
                }

                Badge badge = new Badge(id, name, description, type, requirement, icon);
                badges.put(id.toLowerCase(), badge);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Tipo de insígnia inválido para '" + id + "'. Verifique o campo 'type' no badges.yml. Erro: " + e.getMessage());
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