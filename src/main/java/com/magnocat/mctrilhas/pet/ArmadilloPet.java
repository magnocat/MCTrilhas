// package com.magnocat.mctrilhas.pet;
//
// import com.magnocat.mctrilhas.MCTrilhasPlugin;
// import org.bukkit.ChatColor;
// import org.bukkit.Material;
// import org.bukkit.Location;
// import org.bukkit.Sound;
// import org.bukkit.attribute.Attribute;
// import org.bukkit.attribute.AttributeInstance;
// import org.bukkit.entity.Armadillo;
// import org.bukkit.entity.EntityType;
// import org.bukkit.entity.Player;
// import org.bukkit.inventory.ItemStack;
// import org.bukkit.potion.PotionEffect;
// import org.bukkit.potion.PotionEffectType;
// import org.bukkit.scheduler.BukkitRunnable;
//
// /**
//  * Implementação concreta de um Pet do tipo Tatu, com habilidades defensivas.
//  */
// public class ArmadilloPet extends Pet {
//
//     private long lastRollTime = 0;
//     private static final long ROLL_COOLDOWN = 1000 * 60 * 2; // 2 minutos em milissegundos
//
//     public ArmadilloPet(Player owner, PetData petData, MCTrilhasPlugin plugin) {
//         super(owner, petData, plugin);
//     }
//
//     @Override
//     public void spawn() {
//         Location spawnLocation = owner.getLocation();
//         Armadillo armadillo = (Armadillo) owner.getWorld().spawnEntity(spawnLocation, EntityType.ARMADILLO);
//
//         armadillo.setCustomName(getFormattedName());
//         armadillo.setCustomNameVisible(true);
//         armadillo.setPersistent(false);
//         armadillo.setOwner(owner);
//         armadillo.setTamed(true);
//
//         this.entity = armadillo;
//         applyAttributes();
//
//         this.task = new BukkitRunnable() {
//             @Override
//             public void run() {
//                 if (entity == null || !entity.isValid() || owner == null || !owner.isOnline()) {
//                     this.cancel();
//                     return;
//                 }
//
//                 // Habilidade Passiva: Aura Protetora
//                 owner.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 0, true, false)); // Resistência I
//
//                 // Nova Habilidade Passiva: Escavador Eficiente
//                 ItemStack itemInHand = owner.getInventory().getItemInMainHand();
//                 Material itemType = itemInHand.getType();
//                 if (itemType.name().endsWith("_PICKAXE") || itemType.name().endsWith("_SHOVEL")) {
//                     owner.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 0, true, false)); // Pressa I por 5s
//                 }
//
//                 follow();
//             }
//         }.runTaskTimer(plugin, 0L, 40L); // Verifica a cada 2 segundos
//     }
//
//     @Override
//     public void follow() {
//         if (entity != null && entity.isValid() && owner.getLocation().distanceSquared(entity.getLocation()) > 16) {
//             ((Armadillo) entity).getPathfinder().moveTo(owner.getLocation(), 1.0);
//         }
//     }
//
//     @Override
//     public void teleportToOwner() {
//         if (entity != null && entity.isValid()) {
//             entity.teleport(owner.getLocation());
//         }
//     }
//
//     @Override
//     public void onLevelUp() {
//         applyAttributes();
//         if (entity != null && entity.isValid()) {
//             entity.setCustomName(getFormattedName());
//         }
//     }
//
//     private void applyAttributes() {
//         if (entity == null || !entity.isValid()) return;
//         AttributeInstance healthAttribute = ((Armadillo) entity).getAttribute(Attribute.GENERIC_MAX_HEALTH);
//         if (healthAttribute != null) {
//             healthAttribute.setBaseValue(16.0 + (petData.getLevel() - 1) * 1.5); // Vida aumenta com o nível
//         }
//     }
//
//     /**
//      * Ativa a habilidade de "Enrolar", tornando o Tatu muito resistente por um curto período.
//      */
//     public void rollUp() {
//         if (System.currentTimeMillis() - lastRollTime < ROLL_COOLDOWN) {
//             long remaining = (ROLL_COOLDOWN - (System.currentTimeMillis() - lastRollTime)) / 1000;
//             owner.sendMessage(ChatColor.YELLOW + "Seu tatu ainda está se recuperando. Tente novamente em " + remaining + " segundos.");
//             return;
//         }
//         lastRollTime = System.currentTimeMillis();
//
//         if (entity instanceof Armadillo) {
//             Armadillo armadillo = (Armadillo) entity;
//             armadillo.setRolling(true);
//             armadillo.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 4, true, false)); // Resistência V por 5s
//             entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ARMADILLO_ROLL, 1.0f, 1.0f);
//             owner.sendMessage(ChatColor.GREEN + "Seu tatu se enrolou para se proteger!");
//
//             // Agenda para desenrolar após 5 segundos
//             new BukkitRunnable() {
//                 @Override
//                 public void run() {
//                     if (armadillo.isValid()) {
//                         armadillo.setRolling(false);
//                     }
//                 }
//             }.runTaskLater(plugin, 100L);
//         }
//     }
// }