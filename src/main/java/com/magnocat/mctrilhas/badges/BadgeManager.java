package com.magnocat.mctrilhas.badges;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

import com.magnocat.mctrilhas.MCTrilhasPlugin;

/**
 * Gerencia o carregamento e a recuperação das definições de insígnias da configuração.
 * Esta classe lê a seção 'badges' do config.yml, cria objetos Badge,
 * e os armazena em um cache em memória para acesso rápido em todo o plugin.
 */
@SuppressWarnings("deprecation")
public class BadgeManager {

    private final MCTrilhasPlugin plugin;
    private final Map<String, Badge> badges = new LinkedHashMap<>();

    /**
     * Constrói um novo BadgeManager.
     * @param plugin A instância principal do plugin.
     */
    public BadgeManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        loadBadgesFromConfig();
    }

    /**
     * Registra todos os listeners relacionados ao sistema de insígnias.
     * Isso centraliza a lógica e torna o módulo de insígnias mais autônomo.
     */
    public void registerBadgeListeners() {
        List<Listener> badgeListeners = Arrays.asList(
                new MiningListener(plugin),
                new LumberjackListener(plugin),
                new CookingListener(plugin),
                new BuilderListener(plugin),
                new FishingListener(plugin),
                new FarmingListener(plugin),
                new CraftingListener(plugin),
                new MobKillListener(plugin),
                new TamingListener(plugin),
                new ExplorerListener(plugin)
        );
        badgeListeners.forEach(listener -> plugin.getServer().getPluginManager().registerEvents(listener, plugin));
    }

    /**
     * Carrega ou recarrega todas as definições de insígnias do arquivo config.yml.
     * Este método limpa o cache de insígnias existente e o preenche novamente analisando
     * a seção 'badges' da configuração. Ele registra avisos para entradas de insígnias
     * malformadas e erros para problemas inesperados.
     */
    public void loadBadgesFromConfig() {
        badges.clear();
        FileConfiguration config = plugin.getConfig();

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
        plugin.getLogger().info(badges.size() + " especialidades foram carregadas.");
    }

    /**
     * Recupera a definição de uma insígnia específica pelo seu ID.
     * A busca não diferencia maiúsculas de minúsculas.
     *
     * @param id O identificador único da insígnia (ex: "mining").
     * @return O objeto {@link Badge} se encontrado, caso contrário, nulo.
     */
    public Badge getBadge(String id) {
        return badges.get(id.toLowerCase());
    }

    /**
     * Recupera uma lista de todas as definições de insígnias carregadas.
     * A ordem das insígnias é preservada a partir do arquivo de configuração.
     *
     * @return Uma {@link List} não modificável de todos os objetos {@link Badge}.
     */
    public List<Badge> getAllBadges() {
        return Collections.unmodifiableList(new ArrayList<>(badges.values()));
    }
}