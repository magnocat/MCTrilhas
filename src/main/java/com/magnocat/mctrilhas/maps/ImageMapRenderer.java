package com.magnocat.mctrilhas.maps;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Renderiza uma imagem estática em um {@link MapView}.
 * <p>
 * A imagem é carregada dos recursos internos do plugin e desenhada no mapa
 * apenas uma vez para otimizar a performance.
 */
public class ImageMapRenderer extends MapRenderer {

    private final MCTrilhasPlugin plugin;
    private BufferedImage image;
    private boolean done = false;

    /**
     * Construtor do renderizador de imagem.
     * @param plugin A instância principal do plugin, usada para logging.
     * @param imagePath O caminho para a imagem dentro dos recursos do plugin (ex: "maps/minha_imagem.png").
     */
    public ImageMapRenderer(MCTrilhasPlugin plugin, String imagePath) {
        this.plugin = plugin;
        try {
            // Carrega a imagem dos recursos do plugin (do arquivo .jar)
            InputStream imageStream = plugin.getResource(imagePath);
            if (imageStream != null) {
                this.image = ImageIO.read(imageStream);
            } else {
                // Loga um erro se a imagem não for encontrada nos recursos.
                plugin.getLogger().severe("Não foi possível encontrar a imagem do mapa em: " + imagePath);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Ocorreu um erro de I/O ao carregar a imagem do mapa: " + imagePath, e);
        }
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (done || image == null) {
            return;
        }
        canvas.drawImage(0, 0, image);
        done = true; // Otimização: marca como concluído para não renderizar novamente.
    }
}