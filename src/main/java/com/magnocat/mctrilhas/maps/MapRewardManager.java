package com.magnocat.mctrilhas.maps;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.ArrayList;
import java.util.List;

/**
 * Gerencia a criação e entrega de recompensas em formato de mapa.
 */
public class MapRewardManager {

    private final MCTrilhasPlugin plugin;

    public MapRewardManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cria um item de mapa customizado como recompensa para uma insígnia.
     * @param player O jogador que receberá o mapa (usado para obter o mundo).
     * @param badgeConfigKey A chave de configuração da insígnia (ex: "MINING").
     * @return O ItemStack do mapa-prêmio, ou null se a configuração do mapa não for encontrada.
     */
    public ItemStack createMapReward(Player player, String badgeConfigKey) {
        String mapRewardPath = "badges." + badgeConfigKey + ".reward-map";
        ConfigurationSection mapSection = plugin.getBadgeConfigManager().getBadgeConfig().getConfigurationSection(mapRewardPath);

        if (mapSection == null) {
            return null; // Nenhuma recompensa de mapa configurada para esta insígnia.
        }

        String imagePath = mapSection.getString("image");
        if (imagePath == null || imagePath.isEmpty()) {
            plugin.getLogger().warning("A insígnia '" + badgeConfigKey + "' tem uma recompensa de mapa, mas o caminho da imagem está faltando.");
            return null;
        }

        MapView mapView = Bukkit.createMap(player.getWorld());

        mapView.getRenderers().forEach(mapView::removeRenderer);
        mapView.addRenderer(new ImageMapRenderer(imagePath));

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();

        if (mapMeta != null) {
            mapMeta.setMapView(mapView);

            String name = mapSection.getString("name", "&6Troféu: {badgeName}");
            String badgeName = plugin.getBadgeConfigManager().getBadgeConfig().getString("badges." + badgeConfigKey + ".name", badgeConfigKey);
            mapMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name.replace("{badgeName}", badgeName)));

            mapItem.setItemMeta(mapMeta);
        }

        return mapItem;
    }
}