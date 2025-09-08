package com.magnocat.mctrilhas.quests;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreasureHuntManager {

    private final MCTrilhasPlugin plugin;

    public TreasureHuntManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void startHunt(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Não foi possível carregar seus dados. Tente novamente.");
            return;
        }

        // 1. Verifica se a caça ao tesouro está habilitada no config.
        if (!plugin.getConfig().getBoolean("treasure-hunt.enabled", false)) {
            player.sendMessage(ChatColor.RED + "A caça ao tesouro está desativada no momento.");
            return;
        }

        // 2. Verifica se o jogador já tem uma caça ativa.
        if (playerData.getCurrentTreasureHuntStage() != -1) {
            player.sendMessage(ChatColor.RED + "Você já tem uma caça ao tesouro em andamento! Use /tesouro pista para continuar ou /tesouro cancelar para desistir.");
            return;
        }

        // 3. Requer que o jogador tenha a insígnia de Explorador.
        if (!playerData.hasBadge("EXPLORER")) {
            player.sendMessage(ChatColor.RED + "Você precisa conquistar a 'Insígnia de Explorador' para iniciar uma caça ao tesouro!");
            return;
        }

        // 4. Carrega, valida e sorteia os locais do config.
        List<String> allLocations = plugin.getTreasureLocationsManager().getAllLocations();
        int stagesRequired = plugin.getConfig().getInt("treasure-hunt.stages-required", 3);

        if (allLocations.isEmpty()) {
            player.sendMessage(ChatColor.RED + "A caça ao tesouro não pode ser iniciada pois não há locais configurados.");
            plugin.logSevere("O arquivo 'treasure_locations.yml' está vazio ou não foi encontrado. A caça ao tesouro está desativada.");
            return;
        }

        if (allLocations.size() < stagesRequired) {
            player.sendMessage(ChatColor.RED + "Não há locais de tesouro suficientes configurados para iniciar uma nova caça.");
            plugin.logWarn("A caça ao tesouro requer " + stagesRequired + " locais, mas apenas " + allLocations.size() + " estão configurados no treasure_locations.yml.");
            return;
        }

        Collections.shuffle(allLocations);
        List<String> selectedLocations = new ArrayList<>(allLocations.subList(0, stagesRequired));

        // 5. Salva os dados no jogador e envia a mensagem de início.
        playerData.setTreasureHuntLocations(selectedLocations);
        playerData.setCurrentTreasureHuntStage(0); // Inicia no estágio 0 (primeiro local).

        player.sendMessage(ChatColor.GREEN + "Uma nova Caça ao Tesouro foi iniciada!");
        player.sendMessage(ChatColor.YELLOW + "Você precisará encontrar " + stagesRequired + " locais secretos.");
        player.sendMessage(ChatColor.AQUA + "Use o comando " + ChatColor.WHITE + "/tesouro pista" + ChatColor.AQUA + " para receber uma bússola que aponta para o primeiro local.");
    }

    public void giveClue(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Não foi possível carregar seus dados. Tente novamente.");
            return;
        }

        if (playerData.getCurrentTreasureHuntStage() == -1) {
            player.sendMessage(ChatColor.RED + "Você não tem uma caça ao tesouro em andamento. Use /tesouro iniciar para começar uma!");
            return;
        }

        int currentStage = playerData.getCurrentTreasureHuntStage();
        List<String> locations = playerData.getTreasureHuntLocations();

        if (currentStage >= locations.size()) {
            player.sendMessage(ChatColor.RED + "Ocorreu um erro com sua caça ao tesouro. Por favor, contate um administrador.");
            plugin.logSevere("Erro na caça ao tesouro do jogador " + player.getName() + ": estágio atual (" + currentStage + ") é inválido para a lista de locais (tamanho " + locations.size() + ").");
            cancelHunt(player);
            return;
        }

        String locationString = locations.get(currentStage);
        String[] parts = locationString.split(",");

        try {
            Location targetLocation = new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
            String description = (parts.length > 4) ? parts[4] : "Um local misterioso...";

            ItemStack compass = new ItemStack(Material.COMPASS);
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + "Bússola do Tesouro");
                meta.setLore(List.of(ChatColor.GRAY + "Aponta para o próximo local.", "", ChatColor.YELLOW + "Pista: " + ChatColor.ITALIC + description));
                meta.setLodestone(targetLocation);
                meta.setLodestoneTracked(false); // Faz a bússola funcionar sem um bloco de Lodestone
                meta.addEnchant(Enchantment.LURE, 1, true); // Apenas para o efeito de brilho
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); // Esconde o encantamento
                compass.setItemMeta(meta);
            }

            player.getInventory().addItem(compass);
            player.sendMessage(ChatColor.GREEN + "Você recebeu uma bússola mágica!");
            player.sendMessage(ChatColor.YELLOW + "Siga-a para encontrar o próximo local do tesouro.");

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Ocorreu um erro ao ler o local do tesouro. Por favor, contate um administrador.");
            plugin.logSevere("Erro ao processar local de tesouro '" + locationString + "': " + e.getMessage());
        }
    }

    public void cancelHunt(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(ChatColor.RED + "Não foi possível carregar seus dados. Tente novamente.");
            return;
        }

        if (playerData.getCurrentTreasureHuntStage() == -1) {
            player.sendMessage(ChatColor.RED + "Você não tem nenhuma caça ao tesouro para cancelar.");
            return;
        }

        playerData.setTreasureHuntLocations(new ArrayList<>());
        playerData.setCurrentTreasureHuntStage(-1);

        player.sendMessage(ChatColor.YELLOW + "Você abandonou a caça ao tesouro atual.");
    }

    public void advanceStage(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (playerData == null || playerData.getCurrentTreasureHuntStage() == -1) {
            return; // Sem caça ativa, não faz nada.
        }

        // Remove a bússola antiga antes de dar a mensagem de sucesso.
        removeTreasureCompasses(player);

        int currentStage = playerData.getCurrentTreasureHuntStage();
        int totalStages = playerData.getTreasureHuntLocations().size();

        // Efeitos de feedback para o jogador
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5);

        int nextStage = currentStage + 1;

        if (nextStage >= totalStages) {
            // Caça concluída!
            plugin.getTreasureHuntRewardManager().handleHuntCompletion(player, playerData);
            // Reseta os dados da caça
            playerData.setTreasureHuntLocations(new ArrayList<>());
            playerData.setCurrentTreasureHuntStage(-1);
        } else {
            // Estágio concluído, mas não é o final
            playerData.setCurrentTreasureHuntStage(nextStage);
            player.sendMessage(ChatColor.GREEN + "Você encontrou o local do tesouro! " + ChatColor.YELLOW + (totalStages - nextStage) + " restante(s).");
            player.sendMessage(ChatColor.AQUA + "Use /tesouro pista para a próxima localização.");
        }
    }

    /**
     * Remove todas as instâncias da "Bússola do Tesouro" do inventário do jogador.
     * @param player O jogador a ter o inventário limpo.
     */
    private void removeTreasureCompasses(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item != null && item.getType() == Material.COMPASS && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Bússola do Tesouro")) {
                    player.getInventory().remove(item);
                }
            }
        }
    }
}