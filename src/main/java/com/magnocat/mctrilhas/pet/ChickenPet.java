package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Implementação concreta de um Pet do tipo Galinha, com a habilidade de botar ovos e suavizar quedas.
 */
public class ChickenPet extends Pet {

    private long lastEggTime = 0;
    private static final long EGG_COOLDOWN = 1000 * 60 * 5; // 5 minutos em milissegundos

    public ChickenPet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
        super(owner, petData, plugin);
    }

    @Override
    public void spawn() {
        Location spawnLocation = owner.getLocation();
        Chicken chicken = (Chicken) owner.getWorld().spawnEntity(spawnLocation, EntityType.CHICKEN);

        chicken.setCustomName(getFormattedName());
        chicken.setCustomNameVisible(true);
        chicken.setPersistent(false);
        chicken.setAdult();

        this.entity = chicken;
        applyAttributes();

        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
                    this.cancel();
                    return;
                }

                // Habilidade Passiva: Queda Suave
                if (isPlayerFalling()) {
                    owner.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, true, false));
                }

                follow();
            }
        }.runTaskTimer(plugin, 0L, 10L); // Verifica a cada meio segundo
    }

    private boolean isPlayerFalling() {
        // Verifica se o jogador está no ar e com velocidade vertical negativa (caindo)
        return !owner.isOnGround() && owner.getVelocity().getY() < -0.5;
    }

    @Override
    public void follow() {
        if (entity != null && entity.isValid() && owner.getLocation().distanceSquared(entity.getLocation()) > 16) {
            ((Chicken) entity).getPathfinder().moveTo(owner.getLocation(), 1.0);
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
        AttributeInstance healthAttribute = ((Chicken) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(4.0 + (petData.getLevel() / 2.0)); // Vida aumenta lentamente
        }
    }

    public void layEgg() {
        if (System.currentTimeMillis() - lastEggTime < EGG_COOLDOWN) {
            long remaining = (EGG_COOLDOWN - (System.currentTimeMillis() - lastEggTime)) / 1000;
            owner.sendMessage(ChatColor.YELLOW + "Sua galinha precisa de um tempo. Tente novamente em " + remaining + " segundos.");
            return;
        }
        lastEggTime = System.currentTimeMillis();
        entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.EGG));
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
        owner.sendMessage(ChatColor.GREEN + "Sua galinha botou um ovo!");
    }
}