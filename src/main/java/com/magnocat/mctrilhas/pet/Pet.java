package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Classe base abstrata para todos os tipos de pets.
 */
public abstract class Pet {

    protected Player owner;
    protected LivingEntity entity;
    protected PetData petData;
    protected MCTrilhasPlugin plugin;
    protected BukkitTask task; // Para tarefas de comportamento (seguir, coletar, etc.)

    public Pet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
        this.owner = owner;
        this.petData = petData;
        this.plugin = plugin;
    }

    // Métodos que todos os pets devem implementar
    public abstract void spawn();

    public void despawn() {
        if (task != null && !task.isCancelled()) task.cancel();
        if (entity != null && entity.isValid()) entity.remove();
    }

    public abstract void follow();
    public abstract void teleportToOwner();
    public abstract void onLevelUp();

    /**
     * Formata o nome de exibição do pet, incluindo seu nível.
     * @return O nome formatado.
     */
    protected String getFormattedName() {
        return ChatColor.translateAlternateColorCodes('&', petData.getName()) + ChatColor.GRAY + " [Lvl. " + petData.getLevel() + "]";
    }

    public LivingEntity getEntity() { return entity; }
    public Player getOwner() { return owner; }
}