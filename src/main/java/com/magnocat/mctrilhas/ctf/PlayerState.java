package com.magnocat.mctrilhas.ctf;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Armazena o estado de um jogador antes de entrar em uma partida de CTF.
 */
public class PlayerState {

    private final ItemStack[] inventoryContents;
    private final ItemStack[] armorContents;
    private final GameMode gameMode;
    private final double health;
    private final int foodLevel;
    private final float exp;
    private final int level;

    public PlayerState(Player player) {
        this.inventoryContents = player.getInventory().getContents();
        this.armorContents = player.getInventory().getArmorContents();
        this.gameMode = player.getGameMode();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.exp = player.getExp();
        this.level = player.getLevel();
    }

    /**
     * Restaura o estado salvo para o jogador.
     * @param player O jogador para o qual o estado será restaurado.
     */
    public void restore(Player player) {
        player.getInventory().setContents(inventoryContents);
        player.getInventory().setArmorContents(armorContents);
        player.setGameMode(gameMode);
        player.setHealth(health);
        player.setFoodLevel(foodLevel);
        player.setExp(exp);
        player.setLevel(level);
        player.updateInventory();
    }
}