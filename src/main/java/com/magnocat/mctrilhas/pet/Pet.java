package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.Particle;
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
     * Spawns heart particles to show affection.
     */
    public void showAffection() {
        if (entity != null && entity.isValid()) {
            entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5);
        }
    }

    /**
     * Ajusta a velocidade do pet com base na sua felicidade.
     */
    protected void updateSpeed() {
        if (entity == null || !entity.isValid()) return;

        double happiness = petData.getHappiness();
        double baseSpeed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
        double speedModifier = 1.0;

        if (happiness < 30) speedModifier = 0.75; // 25% mais lento se infeliz

        entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(baseSpeed * speedModifier);
    }

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