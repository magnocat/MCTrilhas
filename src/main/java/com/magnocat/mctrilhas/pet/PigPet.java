package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;

/**
 * Implementação concreta de um Pet do tipo Porco, com a habilidade de coletar itens.
 */
public class PigPet extends Pet {

    public PigPet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
        super(owner, petData, plugin);
    }

    @Override
    public void spawn() {
        Location spawnLocation = owner.getLocation();
        Pig pig = (Pig) owner.getWorld().spawnEntity(spawnLocation, EntityType.PIG);

        pig.setCustomName(getFormattedName());
        pig.setCustomNameVisible(true);
        pig.setPersistent(false);
        pig.setAdult(); // Garante que seja um porco adulto

        this.entity = pig;
        applyAttributes();

        // Inicia a tarefa de comportamento (seguir e coletar)
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
                    this.cancel();
                    return;
                }

                // Lógica de coleta de itens
                Collection<Item> nearbyItems = entity.getWorld().getNearbyEntitiesByType(Item.class, entity.getLocation(), 5);
                if (!nearbyItems.isEmpty()) {
                    Item targetItem = nearbyItems.iterator().next();
                    // Move o porco em direção ao item
                    ((Pig) entity).getPathfinder().moveTo(targetItem.getLocation());

                    // Se o porco estiver perto o suficiente, coleta o item
                    if (entity.getLocation().distanceSquared(targetItem.getLocation()) < 2.25) { // Raio de 1.5 blocos
                        ItemStack itemStack = targetItem.getItemStack();
                        HashMap<Integer, ItemStack> remaining = owner.getInventory().addItem(itemStack);
                        // Se sobrou algum item (inventário cheio), joga de volta no mundo
                        if (!remaining.isEmpty()) {
                            owner.getWorld().dropItem(owner.getLocation(), remaining.get(0));
                        }
                        targetItem.remove();
                    }
                } else {
                    // Se não houver itens, segue o dono
                    follow();
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // Verifica a cada 2 segundos
    }

    @Override
    public void follow() {
        if (entity != null && entity.isValid() && owner.getLocation().distanceSquared(entity.getLocation()) > 16) {
            ((Pig) entity).getPathfinder().moveTo(owner.getLocation(), 1.2); // Velocidade um pouco maior
        }
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
        // No futuro, podemos aumentar a velocidade ou o raio de coleta do porco com o nível.
        AttributeInstance healthAttribute = ((Pig) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) healthAttribute.setBaseValue(10.0 + (petData.getLevel() - 1)); // Vida aumenta com o nível
    }
}