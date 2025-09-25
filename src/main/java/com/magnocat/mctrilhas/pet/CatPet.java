package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Cat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Implementação concreta de um Pet do tipo Gato, com a habilidade de alertar sobre monstros.
 */
public class CatPet extends Pet {

    public CatPet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
        super(owner, petData, plugin);
    }

    @Override
    public void spawn() {
        Location spawnLocation = owner.getLocation();
        Cat cat = (Cat) owner.getWorld().spawnEntity(spawnLocation, EntityType.CAT);

        // Lógica especial para o Admin! 😼
        if (owner.getName().equalsIgnoreCase("MagnoCat")) {
            cat.setCatType(Cat.Type.BLACK);
            petData.setName("§5Bastet"); // Nome especial
        } else {
            // Tipo de gato aleatório para outros jogadores
            Cat.Type[] types = Cat.Type.values();
            cat.setCatType(types[(int) (Math.random() * types.length)]);
        }

        cat.setOwner(owner);
        cat.setTamed(true);
        cat.setSitting(false);
        cat.setCustomName(getFormattedName());
        cat.setCustomNameVisible(true);
        cat.setPersistent(false);

        this.entity = cat;
        applyAttributes();

        // Inicia a tarefa de comportamento (seguir e alertar)
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
                    this.cancel();
                    return;
                }

                // Lógica de alerta de monstros
                Collection<Monster> nearbyMonsters = entity.getWorld().getNearbyEntities(entity.getLocation(), 10, 5, 10, e -> e instanceof Monster).stream().map(e -> (Monster) e).collect(Collectors.toList());
                if (!nearbyMonsters.isEmpty()) {
                    owner.playSound(owner.getLocation(), Sound.ENTITY_CAT_STRAY_AMBIENT, 0.5f, 1.2f);
                    entity.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, entity.getLocation().add(0, 1, 0), 1);
                }

                follow();
            }
        }.runTaskTimer(plugin, 0L, 60L); // Verifica a cada 3 segundos
    }

    @Override
    public void follow() {
        // A IA nativa do gato já o faz seguir o dono.
    }

    @Override
    public void teleportToOwner() {
        // A IA nativa do gato já o teleporta se ele ficar muito longe.
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
        AttributeInstance healthAttribute = ((Cat) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) healthAttribute.setBaseValue(10.0 + (petData.getLevel() - 1));
    }
}