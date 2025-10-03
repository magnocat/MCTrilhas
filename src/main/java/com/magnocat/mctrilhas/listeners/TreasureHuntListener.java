package com.magnocat.mctrilhas.listeners;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;

public class TreasureHuntListener implements Listener {

    private final MCTrilhasPlugin plugin;
    // Usar o quadrado da distância é mais eficiente (evita raiz quadrada). 3*3 = 9.
    private static final double FIND_RADIUS_SQUARED = 9.0;

    public TreasureHuntListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Otimização: verifica se o jogador se moveu para um novo bloco.
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        // Verifica se o jogador tem uma caça ao tesouro ativa.
        if (playerData == null || playerData.getCurrentTreasureHuntStage() == -1) {
            return;
        }

        int currentStage = playerData.getCurrentTreasureHuntStage();
        List<String> locations = playerData.getTreasureHuntLocations();
        if (currentStage >= locations.size()) return; // Validação de segurança.

        String locationString = locations.get(currentStage);
        String[] parts = locationString.split(",");

        try {
            Location targetLocation = new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));

            // Verifica se o jogador está no mesmo mundo e dentro do raio de detecção.
            // Adiciona verificação para o TreasureHuntManager
            if (plugin.getTreasureHuntManager() != null &&
                player.getWorld().equals(targetLocation.getWorld()) && player.getLocation().distanceSquared(targetLocation) <= FIND_RADIUS_SQUARED) {
                // O jogador encontrou o local! Delega a lógica para o manager.
                plugin.getTreasureHuntManager().advanceStage(player);
            }
        } catch (Exception e) {
            // Loga o erro, mas não envia spam para o jogador.
            plugin.getLogger().warning("Não foi possível verificar o local do tesouro para " + player.getName() + " devido a uma string de localização inválida: " + locationString);
        }
    }
}