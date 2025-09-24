package com.magnocat.mctrilhas.pet;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;

/**
 * Implementação concreta de um Pet do tipo Lobo.
 */
public class WolfPet extends Pet {

    public WolfPet(Player owner, PetData petData) {
        super(owner, petData);
    }

    @Override
    public void spawn() {
        Location spawnLocation = owner.getLocation();
        Wolf wolf = (Wolf) owner.getWorld().spawnEntity(spawnLocation, EntityType.WOLF);

        // Configurações básicas do pet
        wolf.setOwner(owner); // O pet pertence ao jogador
        wolf.setTamed(true); // Está domesticado
        wolf.setSitting(false); // Começa em pé
        wolf.setCustomName(ChatColor.translateAlternateColorCodes('&', petData.getName()));
        wolf.setCustomNameVisible(true);
        wolf.setPersistent(false); // Não salva a entidade no mundo ao desligar o servidor

        this.entity = wolf;
    }

    @Override
    public void despawn() {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    @Override
    public void follow() {
        // A IA nativa do lobo já o faz seguir o dono.
        // Podemos adicionar lógicas mais complexas aqui no futuro.
    }

    @Override
    public void teleportToOwner() {
        // A IA nativa do lobo já o teleporta se ele ficar muito longe.
    }
}