package com.magnocat.mctrilhas.integrations;

import com.flowpowered.math.vector.Vector3d;
import com.magnocat.mctrilhas.MCTrilhasPlugin;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Gerencia a integração com o plugin BlueMap.
 */
public class BlueMapManager {

    private final MCTrilhasPlugin plugin;
    private static final String MARKER_SET_ID = "mctrilhas-badges";

    public BlueMapManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        setupMarkerSet();
    }

    /**
     * Garante que o MarkerSet para as insígnias exista em todos os mapas.
     */
    private void setupMarkerSet() {
        BlueMapAPI.getInstance().ifPresent(api -> {
            api.getMaps().forEach(map -> {
                map.getMarkerSets().computeIfAbsent(MARKER_SET_ID, id ->
                        MarkerSet.builder()
                                .label("Insígnias Conquistadas")
                                .toggleable(true) // Permite que os jogadores ocultem/mostrem este conjunto de marcadores
                                .defaultHidden(false) // O conjunto de marcadores é visível por padrão
                                .build()
                );
            });
            plugin.getLogger().info("MarkerSet do MCTrilhas para o BlueMap foi configurado.");
        });
    }

    /**
     * Adiciona um marcador de Ponto de Interesse (POI) no BlueMap no local onde o jogador conquistou uma insígnia.
     * @param player O jogador que conquistou a insígnia.
     * @param badgeConfigKey A chave de configuração da insígnia (ex: "MINING").
     */
    public void addBadgeMarker(Player player, String badgeConfigKey) {
        Optional<BlueMapAPI> apiOptional = BlueMapAPI.getInstance();
        if (!apiOptional.isPresent()) {
            return; // Segurança extra, caso a API se torne indisponível.
        }

        BlueMapAPI api = apiOptional.get();
        Location loc = player.getLocation();
        String badgeName = plugin.getBadgeConfigManager().getBadgeConfig().getString("badges." + badgeConfigKey + ".name", badgeConfigKey);

        api.getWorld(player.getWorld()).ifPresent(blueMapWorld -> {
            blueMapWorld.getMaps().forEach(map -> {
                MarkerSet markerSet = map.getMarkerSets().get(MARKER_SET_ID);
                if (markerSet != null) {
                    String markerId = "badge-" + player.getUniqueId().toString() + "-" + badgeConfigKey;
                    POIMarker marker = POIMarker.builder()
                            .label(player.getName() + " - " + badgeName)
                            .position(new Vector3d(loc.getX(), loc.getY(), loc.getZ()))
                            .build();
                    markerSet.getMarkers().put(markerId, marker);
                }
            });
        });
    }
}