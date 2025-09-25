package com.magnocat.mctrilhas.pet;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import com.magnocat.mctrilhas.ranks.Rank;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.persistence.PersistentDataType;

/**
 * Gerencia todos os pets ativos no servidor.
 */
public class PetManager implements Listener {

    private final MCTrilhasPlugin plugin;
    // Mapeia o UUID do jogador para a instância do seu pet ativo.
    private final Map<UUID, Pet> activePets = new HashMap<>();

    public PetManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startHappinessDecayTask();
    }

    /**
     * Invoca um pet para um jogador.
     * @param player O jogador que está invocando o pet.
     * @param petType O tipo de pet a ser invocado (ex: "lobo").
     */
    public void summonPet(Player player, String petType) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Não foi possível carregar seus dados.");
            return;
        }

        // 1. Verifica se o jogador tem o ranque mínimo
        if (playerData.getRank().ordinal() < Rank.ESCOTEIRO.ordinal()) {
            player.sendMessage(ChatColor.RED + "Você precisa ser do ranque " + Rank.ESCOTEIRO.getDisplayName() + " ou superior para ter um pet.");
            return;
        }

        // 2. Verifica se o pet já está invocado
        if (activePets.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Você já tem um pet invocado. Libere-o primeiro com /scout pet liberar.");
            return;
        }

        // 3. Verifica se o jogador já possui um pet
        PetData petData = playerData.getPetData();
        if (petData == null || !petData.isOwned()) {
            player.sendMessage(ChatColor.RED + "Você ainda não adquiriu um pet. Use /scout pet loja para comprar um.");
            return;
        } else {
            // Se o jogador já tem um pet, apenas muda o tipo
            petData.setType(petType);
        }

        Pet pet = createPetInstance(player, petData);
        if (pet == null) {
            player.sendMessage(ChatColor.RED + "O tipo de pet '" + petType + "' é inválido ou ainda não foi implementado.");
            return;
        }

        pet.spawn();
        activePets.put(player.getUniqueId(), pet);
        player.sendMessage(ChatColor.GREEN + "Você invocou seu " + petType + "!");
    }

    /**
     * Processa a compra de um pet por um jogador.
     * @param player O jogador que está comprando.
     * @param petType O tipo de pet a ser comprado.
     */
    public void purchasePet(Player player, String petType) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Não foi possível carregar seus dados.");
            return;
        }

        // 1. Verifica se o jogador tem o ranque mínimo
        if (playerData.getRank().ordinal() < Rank.ESCOTEIRO.ordinal()) {
            player.sendMessage(ChatColor.RED + "Você precisa ser do ranque " + Rank.ESCOTEIRO.getDisplayName() + " ou superior para ter um pet.");
            return;
        }

        // 2. Verifica se o jogador já possui um pet
        PetData petData = playerData.getPetData();
        if (petData != null && petData.isOwned()) {
            player.sendMessage(ChatColor.YELLOW + "Você já possui um pet! Use /scout pet invocar <tipo> para trocá-lo.");
            return;
        }

        // 3. Lógica de custo
        Economy econ = plugin.getEconomy();
        double cost = 50000;
        if (econ == null || econ.getBalance(player) < cost) {
            player.sendMessage(ChatColor.RED + "Você precisa de " + cost + " Totens para adquirir seu primeiro pet.");
            return;
        }

        EconomyResponse r = econ.withdrawPlayer(player, cost);
        if (!r.transactionSuccess()) {
            player.sendMessage(ChatColor.RED + "Ocorreu um erro na transação. Tente novamente.");
            return;
        }

        // 4. Cria ou atualiza os dados do pet
        if (petData == null) {
            petData = new PetData(petType, petType, 1, 0, false, true, 100.0);
            playerData.setPetData(petData);
        } else {
            petData.setOwned(true);
        }

        player.sendMessage(ChatColor.GREEN + "Parabéns! Você adquiriu seu primeiro pet por " + cost + " Totens!");
        player.sendMessage(ChatColor.GREEN + "Use " + ChatColor.YELLOW + "/scout pet invocar " + petType + ChatColor.GREEN + " para chamá-lo!");
        player.closeInventory();
    }

    /**
     * Abre a GUI da loja de pets para um jogador.
     * @param player O jogador.
     */
    public void openShop(Player player) {
        new PetShopMenu(plugin).open(player);
    }

    /**
     * Libera (despawn) o pet de um jogador.
     * @param player O jogador cujo pet será liberado.
     */
    public void releasePet(Player player) {
        Pet pet = activePets.remove(player.getUniqueId());
        if (pet != null) {
            pet.despawn();
            player.sendMessage(ChatColor.YELLOW + "Seu pet foi guardado com segurança.");
        } else {
            player.sendMessage(ChatColor.RED + "Você não tem nenhum pet invocado.");
        }
    }

    /**
     * Renomeia o pet de um jogador, aplicando custos se necessário.
     * @param player O jogador.
     * @param newName O novo nome para o pet.
     */
    public void renamePet(Player player, String newName) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getPetData() == null) {
            player.sendMessage(ChatColor.RED + "Você ainda não tem um pet para nomear.");
            return;
        }

        PetData petData = playerData.getPetData();

        if (!petData.hasCustomName()) {
            // Primeira nomeação é gratuita
            petData.setName(newName);
            petData.setHasCustomName(true);
            player.sendMessage(ChatColor.GREEN + "Você nomeou seu pet de '" + newName + "'!");
        } else {
            // Renomear tem um custo
            Economy econ = plugin.getEconomy();
            double cost = 5000;
            if (econ == null || econ.getBalance(player) < cost) {
                player.sendMessage(ChatColor.RED + "Você precisa de " + cost + " Totens para renomear seu pet.");
                return;
            }

            EconomyResponse r = econ.withdrawPlayer(player, cost);
            if (!r.transactionSuccess()) {
                player.sendMessage(ChatColor.RED + "Ocorreu um erro na transação. Tente novamente.");
                return;
            }

            petData.setName(newName);
            player.sendMessage(ChatColor.GREEN + "Você gastou " + cost + " Totens e renomeou seu pet para '" + newName + "'!");
        }

        // Se o pet estiver ativo, atualiza o nome da entidade em tempo real
        Pet activePet = activePets.get(player.getUniqueId());
        if (activePet != null && activePet.getEntity() != null) {
            activePet.getEntity().setCustomName(activePet.getFormattedName());
        }
    }

    /**
     * Alimenta o pet ativo de um jogador.
     * @param player O jogador que está alimentando o pet.
     */
    public void feedPet(Player player) {
        Pet activePet = activePets.get(player.getUniqueId());
        if (activePet == null) {
            player.sendMessage(ChatColor.RED + "Você precisa ter seu pet invocado para alimentá-lo.");
            return;
        }

        PetData petData = activePet.petData;
        Material requiredFood = getFoodForPet(petData.getType());

        if (requiredFood == null) {
            player.sendMessage(ChatColor.GRAY + "Este tipo de pet não parece comer nada especial.");
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() != requiredFood) {
            player.sendMessage(ChatColor.RED + "Você precisa estar segurando " + requiredFood.name().toLowerCase().replace('_', ' ') + " para alimentar seu " + petData.getType() + ".");
            return;
        }

        if (petData.getHappiness() >= 100) {
            player.sendMessage(ChatColor.GREEN + "Seu pet já está perfeitamente feliz!");
            return;
        }

        itemInHand.setAmount(itemInHand.getAmount() - 1);
        petData.setHappiness(petData.getHappiness() + 25); // Increase happiness by 25

        player.sendMessage(ChatColor.GREEN + "Você alimentou seu pet! A felicidade dele aumentou.");
        activePet.showAffection();
    }

    private Material getFoodForPet(String petType) {
        switch (petType.toLowerCase()) {
            case "lobo": return Material.BONE;
            case "gato": return Material.COD;
            case "porco": return Material.CARROT;
            case "papagaio": return Material.WHEAT_SEEDS;
            default: return null;
        }
    }

    @EventHandler
    public void onPetInteractionMenuClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().startsWith(PetInteractionMenu.INVENTORY_TITLE_PREFIX)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        NamespacedKey key = new NamespacedKey(plugin, "pet_action");
        String action = clickedItem.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (action != null) {
            player.closeInventory();
            switch (action) {
                case "rename": player.sendMessage(ChatColor.YELLOW + "Para renomear seu pet, use o comando: /scout pet nome <novo nome>"); break;
                case "release": releasePet(player); break;
                case "toggle_shoulder":
                    Pet pet = getActivePet(player);
                    if (pet instanceof ParrotPet) {
                        ((ParrotPet) pet).toggleSitOnShoulder();
                        player.sendMessage(ChatColor.GREEN + "Você deu uma ordem ao seu papagaio!");
                    }
                    break;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(PetShopMenu.INVENTORY_TITLE)) { // Este é o listener da LOJA
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "pet_type");
        String petType = clickedItem.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

        if (petType != null) {
            // Confirmação antes da compra
            player.sendMessage(ChatColor.YELLOW + "Você está prestes a comprar o pet " + petType + " por 50.000 Totens.");
            player.sendMessage(ChatColor.YELLOW + "Clique novamente no item para confirmar a compra.");

            // Para simplificar, a compra é feita no clique. Uma GUI de confirmação seria o próximo passo.
            purchasePet(player, petType);
        }
    }

    /**
     * Concede experiência ao pet de um jogador e verifica se ele subiu de nível.
     * @param owner O dono do pet.
     * @param amount A quantidade de experiência a ser concedida.
     */
    public void grantExperience(Player owner, double amount) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(owner.getUniqueId());
        if (playerData == null || playerData.getPetData() == null) return;

        PetData petData = playerData.getPetData();
        if (petData.getLevel() >= PetData.MAX_LEVEL) return; // Não ganha mais XP no nível máximo.

        petData.setExperience(petData.getExperience() + amount);
        owner.sendMessage(ChatColor.DARK_AQUA + "+ " + (int)amount + " XP para seu pet!");

        // Verifica se o pet subiu de nível
        int xpToNextLevel = petData.getExperienceToNextLevel();
        if (petData.getExperience() >= xpToNextLevel) {
            levelUp(owner, petData);
        }
    }

    /**
     * Processa o level up de um pet.
     * @param owner O dono do pet.
     * @param petData Os dados do pet.
     */
    private void levelUp(Player owner, PetData petData) {
        petData.setLevel(petData.getLevel() + 1);
        petData.setExperience(petData.getExperience() - petData.getExperienceToNextLevel()); // Mantém o XP excedente

        owner.sendMessage(ChatColor.GOLD + "Parabéns! Seu pet subiu para o nível " + petData.getLevel() + "!");

        // Aplica os novos atributos ao pet se ele estiver ativo
        Pet activePet = activePets.get(owner.getUniqueId());
        if (activePet != null) {
            activePet.onLevelUp();
        }
    }

    public boolean hasActivePet(Player player) {
        return activePets.containsKey(player.getUniqueId());
    }

    public Pet getActivePet(Player player) {
        return activePets.get(player.getUniqueId());
    }

    public Pet getPetByEntity(org.bukkit.entity.Entity entity) {
        for (Pet pet : activePets.values()) {
            if (pet.getEntity().equals(entity)) {
                return pet;
            }
        }
        return null;
    }

    /**
     * Cria a instância correta do pet com base no tipo.
     */
    private Pet createPetInstance(Player owner, PetData petData) {
        switch (petData.getType().toLowerCase()) {
            case "lobo":
                return new WolfPet(owner, petData, plugin);
            case "porco":
                return new PigPet(owner, petData, plugin);
            case "gato":
                return new CatPet(owner, petData, plugin);
            case "papagaio":
                return new ParrotPet(owner, petData, plugin);
            default:
                return null;
        }
    }

    /**
     * Libera (despawn) o pet de um jogador quando ele desconecta.
     */
    public void releasePetOnQuit(Player player) {
        Pet pet = activePets.remove(player.getUniqueId());
        if (pet != null) {
            pet.despawn();
        }
    }

    /**
     * Inicia uma tarefa que diminui periodicamente a felicidade de todos os pets ativos.
     */
    private void startHappinessDecayTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activePets.isEmpty()) return;

                for (Pet pet : activePets.values()) {
                    PetData data = pet.petData;
                    // Decrease happiness by a small amount, but not below 20 from natural decay.
                    if (data.getHappiness() > 20) {
                        data.setHappiness(data.getHappiness() - 0.5);
                        pet.updateSpeed(); // Atualiza a velocidade com base na nova felicidade
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60); // Every minute
    }
}