package com.magnocat.mctrilhas.land;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class ClaimToolListener implements Listener {

    private final LandManager landManager;

    public ClaimToolListener(MCTrilhasPlugin plugin) {
        this.landManager = plugin.getLandManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null || event.getItem().getType() != Material.GOLDEN_SHOVEL) {
            return;
        }

        // Garante que o evento seja disparado apenas uma vez pela mão principal
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        event.setCancelled(true); // Impede que a pá cave o bloco

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            landManager.setPos1(player, event.getClickedBlock().getLocation());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            landManager.setPos2(player, event.getClickedBlock().getLocation());
        }
    }
}