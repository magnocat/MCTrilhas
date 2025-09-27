package com.magnocat.mctrilhas.utils;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStateManager {
    private final Map<UUID, PlayerState> savedStates = new HashMap<>();

    public void saveState(Player player) {
        savedStates.put(player.getUniqueId(), new PlayerState(player));
    }

    public void restoreState(Player player) {
        PlayerState state = savedStates.remove(player.getUniqueId());
        if (state != null && player.isOnline()) {
            player.teleport(state.location);
            player.getInventory().setContents(state.inventory);
            player.getInventory().setArmorContents(state.armor);
            player.setHealth(state.health);
            player.setFoodLevel(state.foodLevel);
            player.setExp(state.exp);
            player.setLevel(state.level);
            player.setGameMode(state.gameMode);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            player.addPotionEffects(state.potionEffects);
        }
    }

    private static class PlayerState {
        private final Location location;
        private final ItemStack[] inventory, armor;
        private final double health;
        private final int foodLevel, level;
        private final float exp;
        private final GameMode gameMode;
        private final Collection<PotionEffect> potionEffects;

        public PlayerState(Player p) {
            this.location = p.getLocation();
            this.inventory = p.getInventory().getContents();
            this.armor = p.getInventory().getArmorContents();
            this.health = p.getHealth();
            this.foodLevel = p.getFoodLevel();
            this.exp = p.getExp();
            this.level = p.getLevel();
            this.gameMode = p.getGameMode();
            this.potionEffects = p.getActivePotionEffects();
        }
    }
}