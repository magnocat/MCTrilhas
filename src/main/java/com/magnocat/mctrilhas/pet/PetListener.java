package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Ouve eventos do jogo relacionados aos pets, como ganho de experiência.
 */
public class PetListener implements Listener {

    private final MCTrilhasPlugin plugin;

    public PetListener(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Verifica se a entidade morta é um monstro e se o assassino é um jogador.
        if (!(event.getEntity() instanceof Monster) || event.getEntity().getKiller() == null) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        PetManager petManager = plugin.getPetManager();

        // Verifica se o jogador tem um pet ativo.
        if (petManager.hasActivePet(killer)) {
            // Concede uma quantidade fixa de experiência por monstro derrotado.
            // Isso pode ser expandido para dar XP com base no tipo de monstro.
            int experienceGained = 15;
            petManager.grantExperience(killer, experienceGained);
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        // Verifica se a entidade que sofreu dano é um jogador
        // e se o atacante é um monstro.
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Monster)) {
            return;
        }

        Player owner = (Player) event.getEntity();
        Monster attacker = (Monster) event.getDamager();
        PetManager petManager = plugin.getPetManager();

        // Verifica se o jogador tem um pet ativo.
        if (petManager.hasActivePet(owner)) {
            Pet pet = petManager.getActivePet(owner);
            if (pet != null && pet.getEntity() instanceof Tameable) {
                Tameable petEntity = (Tameable) pet.getEntity();

                // Comanda o pet a atacar o monstro que agrediu o dono.
                // A IA nativa de um lobo domesticado já faz isso, mas esta lógica
                // garante o comportamento e pode ser customizada para outros pets.
                petEntity.setTarget(attacker);
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        // Verifica se o atacante é um jogador e a vítima é um monstro.
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Monster)) {
            return;
        }

        Player owner = (Player) event.getDamager();
        Monster target = (Monster) event.getEntity();
        PetManager petManager = plugin.getPetManager();

        // Verifica se o jogador tem um pet ativo.
        if (petManager.hasActivePet(owner)) {
            Pet pet = petManager.getActivePet(owner);
            if (pet != null && pet.getEntity() instanceof Tameable) {
                Tameable petEntity = (Tameable) pet.getEntity();

                // Comanda o pet a atacar o mesmo alvo do dono.
                petEntity.setTarget(target);
            }
        }
    }
}