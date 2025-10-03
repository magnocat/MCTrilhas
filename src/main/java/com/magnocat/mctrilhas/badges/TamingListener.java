package com.magnocat.mctrilhas.badges;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.badges.BadgeType;

/**
 * Listener para rastrear o progresso da insígnia de Domador.
 * <p>
 * Este listener monitora o evento de domesticação de entidades e incrementa o
 * progresso da insígnia {@link BadgeType#DOMADOR} quando um jogador doma um
 * animal.
 */
public class TamingListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public TamingListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        // O "owner" do evento é quem domou o animal.
        // Verificamos se é um jogador.
        if (event.getOwner() instanceof Player) {
            Player player = (Player) event.getOwner();
            plugin.getPlayerDataManager().addProgress(player, BadgeType.DOMADOR, 1);
        }
    }
}