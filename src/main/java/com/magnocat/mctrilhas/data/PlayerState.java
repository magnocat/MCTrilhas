package com.magnocat.mctrilhas.data;

import java.util.Collection;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/**
 * Armazena o estado de um jogador (inventário, vida, etc.) antes de um duelo,
 * para que possa ser restaurado posteriormente.
 */
public class PlayerState {

    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final float experience;
    private final int level;
    private final GameMode gameMode;
    private final Location location;
    private final Collection<PotionEffect> potionEffects;

    /**
     * Cria um "snapshot" do estado atual do jogador.
     * @param player O jogador cujo estado será salvo.
     */
    public PlayerState(Player player) {
        // Clona os arrays para criar uma cópia segura, independente do inventário original.
        this.inventoryContents = player.getInventory().getContents().clone();
        this.armorContents = player.getInventory().getArmorContents().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.experience = player.getExp();
        this.level = player.getLevel();
        this.gameMode = player.getGameMode();
        this.location = player.getLocation();
        this.potionEffects = player.getActivePotionEffects();
    }

    /**
     * Restaura o estado salvo para o jogador.
     * A localização do jogador é restaurada externamente, usando o método getLocation().
     * @param player O jogador para quem o estado será restaurado.
     */
    public void restore(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        player.getInventory().clear();
        player.getInventory().setContents(this.inventoryContents);
        player.getInventory().setArmorContents(this.armorContents);

        player.setHealth(this.health);
        player.setFoodLevel(this.foodLevel);
        player.setSaturation(this.saturation);
        player.setExp(this.experience);
        player.setLevel(this.level);
        player.setGameMode(this.gameMode);

        // Limpa todos os efeitos de poção atuais antes de restaurar os antigos
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.addPotionEffects(this.potionEffects);

        player.updateInventory();
    }

    public Location getLocation() {
        return location;
    }
}