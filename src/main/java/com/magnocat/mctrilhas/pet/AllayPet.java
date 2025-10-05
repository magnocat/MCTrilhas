package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Allay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;

/**
 * Implementação concreta de um Pet do tipo Allay, com a habilidade de coletar itens específicos.
 */
public class AllayPet extends Pet {

    private Material targetItemType = null; // O tipo de item que o Allay deve coletar.

    public AllayPet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
        super(owner, petData, plugin);
    }

    @Override
    public void spawn() {
        Location spawnLocation = owner.getLocation();
        Allay allay = (Allay) owner.getWorld().spawnEntity(spawnLocation, EntityType.ALLAY);

        allay.setCustomName(getFormattedName());
        allay.setCustomNameVisible(true);
        allay.setPersistent(false);

        this.entity = allay;
        applyAttributes();

        // Inicia a tarefa de comportamento (seguir e coletar)
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
                    this.cancel();
                    return;
                }

                // Se um item alvo foi definido, procura por ele.
                if (targetItemType != null) {
                    Collection<Item> nearbyItems = entity.getWorld().getNearbyEntitiesByType(Item.class, entity.getLocation(), 10); // Raio de 10 blocos
                    boolean foundItems = false;
                    for (Item targetItem : nearbyItems) {
                        if (targetItem.isValid() && !targetItem.isDead() && targetItem.getItemStack().getType() == targetItemType) {
                            foundItems = true;
                            ((Allay) entity).getPathfinder().moveTo(targetItem.getLocation());
                            ItemStack itemStack = targetItem.getItemStack();
                            HashMap<Integer, ItemStack> remaining = owner.getInventory().addItem(itemStack);
                            if (!remaining.isEmpty()) {
                                owner.getWorld().dropItem(owner.getLocation(), remaining.get(0));
                            }
                            targetItem.remove();
                        }
                    }
                    if (!foundItems) {
                        follow();
                    }
                } else {
                    follow();
                }

                // Habilidade Passiva: Harmonia Musical
                // Se estiver perto de um note block tocando, dá velocidade ao dono.
                for (Block block : entity.getWorld().getNearbyEntities(entity.getLocation(), 5, 5, 5).stream()
                        .map(e -> e.getLocation().getBlock())
                        .filter(b -> b.getType() == Material.NOTE_BLOCK)
                        .collect(java.util.stream.Collectors.toSet())) {
                    if (block.getBlockData() instanceof org.bukkit.block.data.type.NoteBlock && ((org.bukkit.block.data.type.NoteBlock) block.getBlockData()).isPowered()) {
                        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0)); // 5s de Velocidade I
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // Verifica a cada 2 segundos
    }

    @Override
    public void follow() {
        if (entity != null && entity.isValid() && owner.getLocation().distanceSquared(entity.getLocation()) > 16) {
            ((Allay) entity).getPathfinder().moveTo(owner.getLocation(), 1.2);
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
        AttributeInstance healthAttribute = ((Allay) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(20.0 + (petData.getLevel() - 1) * 2.0);
        }
    }

    public void setTargetItemType(Material type) {
        this.targetItemType = type;
    }
}