package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.DyeColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Implementação concreta de um Pet do tipo Lobo.
 */
public class WolfPet extends Pet {

    private static final double BASE_HEALTH = 20.0; // Vida base no nível 1
    private static final double HEALTH_PER_LEVEL = 2.0; // Vida extra por nível
    private static final double BASE_DAMAGE = 4.0; // Dano base no nível 1
    private static final double DAMAGE_PER_LEVEL = 0.5; // Dano extra por nível

    public WolfPet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
        super(owner, petData, plugin);
    }

    @Override
    public void spawn() {
        Location spawnLocation = owner.getLocation();
        Wolf wolf = (Wolf) owner.getWorld().spawnEntity(spawnLocation, EntityType.WOLF);

        // Configurações básicas do pet
        wolf.setOwner(owner); // O pet pertence ao jogador
        wolf.setTamed(true); // Está domesticado
        wolf.setSitting(false); // Começa em pé
        wolf.setCollarColor(DyeColor.values()[(int) (Math.random() * DyeColor.values().length)]); // Cor da coleira aleatória!
        wolf.setCustomName(getFormattedName());
        wolf.setCustomNameVisible(true);
        wolf.setPersistent(false); // Não salva a entidade no mundo ao desligar o servidor

        applyAttributes();
        this.entity = wolf;

        // Inicia a tarefa de comportamento (seguir)
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
                    this.cancel();
                    return;
                }

                // Habilidade Passiva: Uivo do Líder
                // A cada 2 minutos, o lobo pode uivar para dar um buff de força.
                if (entity.getTicksLived() % (20 * 120) == 0) {
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
                    owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 0)); // 5s de Força I para o dono
                    ((Wolf) entity).addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 0)); // e para o lobo
                }

                follow();
            }
        }.runTaskTimer(plugin, 0L, 20L); // Verifica a cada 1 segundo
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

    @Override
    public void onLevelUp() {
        applyAttributes();
        if (entity != null && entity.isValid()) {
            entity.setCustomName(getFormattedName());
        }
    }

    /**
     * Aplica os atributos (vida, dano) ao lobo com base em seu nível.
     */
    private void applyAttributes() {
        if (entity == null || !entity.isValid()) return;

        int level = petData.getLevel();
        double maxHealth = BASE_HEALTH + ((level - 1) * HEALTH_PER_LEVEL);
        double attackDamage = BASE_DAMAGE + ((level - 1) * DAMAGE_PER_LEVEL);

        AttributeInstance healthAttribute = ((Wolf) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) healthAttribute.setBaseValue(maxHealth);

        AttributeInstance damageAttribute = ((Wolf) entity).getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttribute != null) damageAttribute.setBaseValue(attackDamage);
    }
}