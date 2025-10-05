package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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

                // Habilidade Passiva: Mímico Engraçado
                // A cada 30 segundos, tem uma chance de imitar um animal próximo.
                if (entity.getTicksLived() % (20 * 30) == 0) {
                    List<Animals> nearbyAnimals = entity.getWorld().getNearbyEntities(entity.getLocation(), 10, 10, 10)
                            .stream()
                            .filter(e -> e instanceof Animals && !(e instanceof Parrot))
                            .map(e -> (Animals) e)
                            .collect(Collectors.toList());

                    if (!nearbyAnimals.isEmpty()) {
                        Animals targetAnimal = nearbyAnimals.get(new Random().nextInt(nearbyAnimals.size()));
                        Sound soundToMimic = getAmbientSound(targetAnimal.getType());
                        if (soundToMimic != null) {
                            entity.getWorld().playSound(entity.getLocation(), soundToMimic, 1.0f, 1.5f); // Tom mais agudo
                        }
                    }
                }

                follow();
            }
        }.runTaskTimer(plugin, 0L, 60L); // Verifica a cada 3 segundos
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

    private Sound getAmbientSound(EntityType type) {
        switch (type) {
            case COW: return Sound.ENTITY_COW_AMBIENT;
            case PIG: return Sound.ENTITY_PIG_AMBIENT;
            case SHEEP: return Sound.ENTITY_SHEEP_AMBIENT;
            case CHICKEN: return Sound.ENTITY_CHICKEN_AMBIENT;
            case WOLF: return Sound.ENTITY_WOLF_AMBIENT;
            case CAT: return Sound.ENTITY_CAT_AMBIENT;
            // case ARMADILLO: return Sound.ENTITY_ARMADILLO_AMBIENT; // Desativado temporariamente
            default: return null;
        }
    }

    public void toggleSitOnShoulder() {
        if (entity instanceof Parrot) {
            ((Parrot) entity).setSitting(!((Parrot) entity).isSitting());
        }
    }
}