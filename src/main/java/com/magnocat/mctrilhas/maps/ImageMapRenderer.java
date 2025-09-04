package com.magnocat.mctrilhas.maps;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Renderiza uma imagem estática em um MapView.
 * A imagem é carregada dos recursos do plugin.
 */
public class ImageMapRenderer extends MapRenderer {

    private BufferedImage image;
    private boolean done = false;

    public ImageMapRenderer(String imagePath) {
        try {
            // Carrega a imagem dos recursos do plugin (do arquivo .jar)
            URL url = getClass().getClassLoader().getResource(imagePath);
            if (url != null) {
                this.image = ImageIO.read(url);
            } else {
                // Loga um erro se a imagem não for encontrada nos recursos.
                System.err.println("Não foi possível encontrar a imagem do mapa em: " + imagePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        // Otimização: se a imagem já foi desenhada, não faz mais nada.
        if (done || image == null) {
            return;
        }
        canvas.drawImage(0, 0, image);
        done = true; // Marca como concluído para não renderizar novamente.
    }
}