package com.magnocat.mctrilhas.maps;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import com.magnocat.mctrilhas.badges.Badge;
import com.magnocat.mctrilhas.MCTrilhasPlugin;

/**
 * Gerencia a criação e entrega de recompensas em formato de mapa.
 */
public class MapRewardManager {

    private final MCTrilhasPlugin plugin;

    /**
     * Construtor do gerenciador de recompensas de mapa.
     * @param plugin A instância principal do plugin.
     */
    public MapRewardManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cria um item de mapa customizado como recompensa para uma insígnia.
     *
     * @param player O jogador que receberá o mapa (usado para obter o mundo).
     * @param badgeId O ID da insígnia (ex: "MINING").
     * @return O ItemStack do mapa-prêmio, ou null se a configuração do mapa não for encontrada.
     */
    public ItemStack createMapReward(Player player, String badgeId) {
        String mapRewardPath = "badges." + badgeId + ".reward-map";
        ConfigurationSection mapSection = plugin.getConfig().getConfigurationSection(mapRewardPath);

        if (mapSection == null) {
            return null; // Nenhuma recompensa de mapa configurada para esta insígnia.
        }

        String imagePath = mapSection.getString("image");
        if (imagePath == null || imagePath.isEmpty()) {
            plugin.getLogger().warning("A insígnia '" + badgeId + "' tem uma recompensa de mapa, mas o caminho da imagem está faltando.");
            return null;
        }

        MapView mapView = Bukkit.createMap(player.getWorld());

        mapView.getRenderers().forEach(mapView::removeRenderer);
        // CORREÇÃO: Usa o construtor atualizado do ImageMapRenderer.
        mapView.addRenderer(new ImageMapRenderer(plugin, imagePath));

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();

        if (mapMeta != null) {
            mapMeta.setMapView(mapView);

            String name = mapSection.getString("name", "&6Troféu: {badgeName}");
            // MELHORIA: Usa o BadgeManager como fonte única da verdade para o nome da insígnia.
            Badge badge = plugin.getBadgeManager().getBadge(badgeId);
            String badgeName = (badge != null) ? badge.name() : badgeId;

            // Substitui ambos os placeholders: {badgeName} e {player}
            String finalName = name.replace("{badgeName}", badgeName).replace("{player}", player.getName());

            mapMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', finalName));

            mapItem.setItemMeta(mapMeta);
        }

        return mapItem;
    }

    /**
     * Restaura o renderizador de imagem para um mapa existente.
     * <p>
     * Este método é chamado na inicialização do plugin para garantir que os mapas
     * em quadros de itens não percam sua imagem após um reinício do servidor.
     *
     * @param mapId O ID do mapa a ser restaurado.
     * @param badgeId O ID da insígnia associada ao mapa, para encontrar a imagem correta.
     */
    public void restoreMapRenderer(int mapId, String badgeId) {
        MapView mapView = Bukkit.getMap(mapId);
        if (mapView == null) {
            // O mapa pode ter sido excluído ou é inválido.
            return;
        }

        String imagePath = plugin.getConfig().getString("badges." + badgeId + ".reward-map.image");
        if (imagePath == null || imagePath.isEmpty()) {
            return; // Imagem não encontrada na configuração.
        }

        // Limpa renderizadores antigos e adiciona o novo para garantir que a imagem correta seja exibida.
        mapView.getRenderers().forEach(mapView::removeRenderer);
        mapView.addRenderer(new ImageMapRenderer(plugin, imagePath));
    }

    /**
     * Restaura os renderizadores para todos os mapas-troféu de todos os jogadores.
     * Este método varre a pasta de dados dos jogadores de forma assíncrona,
     * encontra os IDs de mapas salvos e agenda a restauração do renderizador
     * para cada um no thread principal.
     */
    public void restoreAllMapRenderers() {
        plugin.logInfo("Iniciando restauração dos mapas-troféu...");

        // A varredura dos arquivos é feita de forma assíncrona para não atrasar o boot.
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            File playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
            File[] playerFiles = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));

            if (playerFiles == null) {
                plugin.logInfo("Nenhum dado de jogador encontrado para restaurar mapas.");
                return;
            }

            // Usamos um array atômico para contar em um ambiente multithread.
            java.util.concurrent.atomic.AtomicInteger restoredCount = new java.util.concurrent.atomic.AtomicInteger(0);

            for (File playerFile : playerFiles) {
                FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(playerFile);
                if (!config.isConfigurationSection("badge-map-ids")) {
                    continue;
                }

                ConfigurationSection mapIdsSection = config.getConfigurationSection("badge-map-ids");
                for (String badgeId : mapIdsSection.getKeys(false)) {
                    int mapId = mapIdsSection.getInt(badgeId);
                    plugin.getServer().getScheduler().runTask(plugin, () -> restoreMapRenderer(mapId, badgeId));
                    restoredCount.incrementAndGet();
                }
            }

            if (restoredCount.get() > 0) {
                plugin.logInfo(restoredCount.get() + " renderizadores de mapas-troféu foram restaurados com sucesso.");
            }
        });
    }
}