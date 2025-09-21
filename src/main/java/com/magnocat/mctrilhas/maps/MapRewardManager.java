package com.magnocat.mctrilhas.maps;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
}