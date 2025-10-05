package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;

/**
 * Implementação concreta de um Pet do tipo Vaca, com a habilidade de fornecer leite e purificar efeitos negativos.
 */
public class CowPet extends Pet {

    private long lastMilkTime = 0;
    private static final long MILK_COOLDOWN = 1000 * 60 * 3; // 3 minutos em milissegundos
    private static final List<PotionEffectType> NEGATIVE_EFFECTS = Arrays.asList(
            PotionEffectType.BLINDNESS, PotionEffectType.CONFUSION, PotionEffectType.POISON,
            PotionEffectType.SLOW, PotionEffectType.WEAKNESS, PotionEffectType.WITHER
    );

    public CowPet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
        super(owner, petData, plugin);
    }

    @Override
    public void spawn() {
        Location spawnLocation = owner.getLocation();
        Cow cow = (Cow) owner.getWorld().spawnEntity(spawnLocation, EntityType.COW);

        cow.setCustomName(getFormattedName());
        cow.setCustomNameVisible(true);
        cow.setPersistent(false);
        cow.setAdult();

        this.entity = cow;
        applyAttributes();

        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
                    this.cancel();
                    return;
                }

                // Habilidade Passiva: Aura Purificante
                for (PotionEffect effect : owner.getActivePotionEffects()) {
                    if (NEGATIVE_EFFECTS.contains(effect.getType())) {
                        owner.removePotionEffect(effect.getType());
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_COW_AMBIENT, 1.0f, 1.2f);
                        owner.sendMessage(ChatColor.AQUA + "Sua vaca purificou você do efeito de " + effect.getType().getName().toLowerCase() + "!");
                        break; // Remove um efeito por vez
                    }
                }

                follow();
            }
        }.runTaskTimer(plugin, 0L, 100L); // Verifica a cada 5 segundos
    }

    @Override
    public void follow() {
        if (entity != null && entity.isValid() && owner.getLocation().distanceSquared(entity.getLocation()) > 16) {
            ((Cow) entity).getPathfinder().moveTo(owner.getLocation(), 1.0);
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
        AttributeInstance healthAttribute = ((Cow) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(10.0 + (petData.getLevel() - 1)); // Vida aumenta com o nível
        }
    }

    public void milk() {
        if (System.currentTimeMillis() - lastMilkTime < MILK_COOLDOWN) {
            long remaining = (MILK_COOLDOWN - (System.currentTimeMillis() - lastMilkTime)) / 1000;
            owner.sendMessage(ChatColor.YELLOW + "Sua vaca precisa descansar. Tente novamente em " + remaining + " segundos.");
            return;
        }
        lastMilkTime = System.currentTimeMillis();
        owner.getInventory().addItem(new ItemStack(Material.MILK_BUCKET));
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_COW_MILK, 1.0f, 1.0f);
        owner.sendMessage(ChatColor.GREEN + "Você ordenhou sua vaca e recebeu um balde de leite!");
    }
}