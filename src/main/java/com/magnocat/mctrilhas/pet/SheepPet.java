package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Location;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Implementação concreta de um Pet do tipo Ovelha, com a habilidade de fornecer lã.
 */
public class SheepPet extends Pet {

    private boolean isSheared = false;
    private long lastShearTime = 0;
    private static final long SHEAR_COOLDOWN = 1000 * 60 * 5; // 5 minutos em milissegundos

    public SheepPet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
        super(owner, petData, plugin);
    }

    @Override
    public void spawn() {
        Location spawnLocation = owner.getLocation();
        Sheep sheep = (Sheep) owner.getWorld().spawnEntity(spawnLocation, EntityType.SHEEP);

        sheep.setCustomName(getFormattedName());
        sheep.setCustomNameVisible(true);
        sheep.setPersistent(false);
        sheep.setAdult();
        // A ovelha já nasce com uma cor aleatória
        DyeColor[] colors = DyeColor.values();
        sheep.setColor(colors[(int) (Math.random() * colors.length)]);

        this.entity = sheep;
        applyAttributes();

        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
                    this.cancel();
                    return;
                }

                // Lógica para a lã crescer de volta
                if (isSheared && (System.currentTimeMillis() - lastShearTime > SHEAR_COOLDOWN)) {
                    regrowWool();
                }

                // Habilidade Passiva: Aura Calmante
                // Se o dono estiver com menos da metade da vida e não houver monstros por perto...
                if (owner.getHealth() < owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / 2) {
                    boolean monstersNearby = !owner.getWorld().getNearbyEntities(owner.getLocation(), 10, 5, 10, e -> e instanceof Monster).isEmpty();
                    if (!monstersNearby) {
                        // ...aplica um efeito de regeneração suave.
                        owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, false)); // 5 segundos de Regeneração I
                    }
                }

                follow();
            }
        }.runTaskTimer(plugin, 0L, 40L); // Verifica a cada 2 segundos
    }

    @Override
    public void follow() {
        if (entity != null && entity.isValid() && owner.getLocation().distanceSquared(entity.getLocation()) > 16) {
            ((Sheep) entity).getPathfinder().moveTo(owner.getLocation(), 1.0);
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
        AttributeInstance healthAttribute = ((Sheep) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(8.0 + (petData.getLevel() - 1)); // Vida aumenta com o nível
        }
    }

    public void shear() {
        if (isSheared) {
            owner.sendMessage(org.bukkit.ChatColor.YELLOW + "Sua ovelha já foi tosquiada. Aguarde a lã crescer novamente.");
            return;
        }
        isSheared = true;
        lastShearTime = System.currentTimeMillis();

        // Dropa a lã da cor atual
        DyeColor currentColor = ((Sheep) entity).getColor();
        Material woolType = Material.valueOf(currentColor.name() + "_WOOL");
        entity.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(woolType, 1 + (petData.getLevel() / 10))); // Mais lã com nível

        ((Sheep) entity).setSheared(true);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0f, 1.0f);
        // Muda para uma nova cor aleatória
        ((Sheep) entity).setColor(DyeColor.values()[(int) (Math.random() * DyeColor.values().length)]);
        owner.sendMessage(org.bukkit.ChatColor.GREEN + "Você tosquiou sua ovelha e coletou a lã!");
    }

    private void regrowWool() {
        isSheared = false;
        if (entity != null && entity.isValid()) {
            ((Sheep) entity).setSheared(false);
            owner.sendMessage(org.bukkit.ChatColor.AQUA + "A lã da sua ovelha cresceu novamente!");
        }
    }
}