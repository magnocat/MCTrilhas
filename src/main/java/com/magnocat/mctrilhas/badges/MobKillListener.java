package com.magnocat.mctrilhas.badges;

import com.magnocat.mctrilhas.MCTrilhasPlugin;

import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listener para rastrear o progresso da insígnia de Caçador.
 * <p>
 * Este listener monitora o evento de morte de entidades e incrementa o
 * progresso da insígnia {@link BadgeType#HUNTER} quando um jogador derrota
 * um monstro hostil.
 */
public class MobKillListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public MobKillListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;

        Player killer = event.getEntity().getKiller();
        if (event.getEntity() instanceof Monster) {
            plugin.getPlayerDataManager().addProgress(killer, BadgeType.HUNTER, 1);
        }
    }
}