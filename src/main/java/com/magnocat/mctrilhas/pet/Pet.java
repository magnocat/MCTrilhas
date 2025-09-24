package com.magnocat.mctrilhas.pet;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Classe base abstrata para todos os tipos de pets.
 */
public abstract class Pet {

    protected Player owner;
    protected LivingEntity entity;
    protected PetData petData;

    public Pet(Player owner, PetData petData) {
        this.owner = owner;
        this.petData = petData;
    }

    // Métodos que todos os pets devem implementar
    public abstract void spawn();
    public abstract void despawn();
    public abstract void follow();
    public abstract void teleportToOwner();

    public LivingEntity getEntity() { return entity; }
    public Player getOwner() { return owner; }
}