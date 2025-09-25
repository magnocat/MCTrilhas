package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Implementação concreta de um Pet do tipo Papagaio.
 */
public class ParrotPet extends Pet {

    public ParrotPet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
        super(owner, petData, plugin);
    }

    @Override
    public void spawn() {
        Location spawnLocation = owner.getLocation();
        Parrot parrot = (Parrot) owner.getWorld().spawnEntity(spawnLocation, EntityType.PARROT);

        // Define uma cor aleatória para o papagaio
        Parrot.Variant[] variants = Parrot.Variant.values();
        parrot.setVariant(variants[(int) (Math.random() * variants.length)]);

        parrot.setOwner(owner);
        parrot.setTamed(true);
        parrot.setSitting(false);
        parrot.setCustomName(getFormattedName());
        parrot.setCustomNameVisible(true);
        parrot.setPersistent(false);

        this.entity = parrot;
        applyAttributes();

        // Inicia a tarefa de comportamento (seguir)
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
                    this.cancel();
                    return;
                }
                follow();
            }
        }.runTaskTimer(plugin, 0L, 60L);
    }

    @Override
    public void follow() {
        // A IA nativa já faz o papagaio seguir o dono.
    }

    @Override
    public void teleportToOwner() {
        if (entity != null && entity.isValid()) {
            entity.teleport(owner.getLocation());
        }
    }

    @Override
    public void onLevelUp() {
        applyAttributes();
        if (entity != null && entity.isValid()) {
            entity.setCustomName(getFormattedName());
        }
    }

    private void applyAttributes() {
        if (entity == null || !entity.isValid()) return;
        AttributeInstance healthAttribute = ((Parrot) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(6.0 + (petData.getLevel() / 2.0)); // Vida aumenta lentamente
        }
    }

    public void toggleSitOnShoulder() {
        if (entity instanceof Parrot) {
            ((Parrot) entity).setSitting(!((Parrot) entity).isSitting());
        }
    }
}