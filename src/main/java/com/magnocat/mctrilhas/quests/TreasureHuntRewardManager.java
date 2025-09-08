package com.magnocat.mctrilhas.quests;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TreasureHuntRewardManager {

    private final MCTrilhasPlugin plugin;

    public TreasureHuntRewardManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleHuntCompletion(Player player, PlayerData playerData) {
        player.sendTitle(ChatColor.GOLD + "Caça Concluída!", ChatColor.YELLOW + "Você encontrou todos os tesouros!", 10, 70, 20);

        // Concede a recompensa padrão por conclusão.
        grantPerCompletionReward(player);

        // Incrementa o contador de conclusões.
        int newCompletionCount = playerData.getTreasureHuntsCompleted() + 1;
        playerData.setTreasureHuntsCompleted(newCompletionCount);

        // Verifica se o jogador se qualifica para o grande prêmio.
        int requiredCompletions = plugin.getConfig().getInt("treasure-hunt.grand-prize.completions-required", 3);
        if (!playerData.hasReceivedTreasureGrandPrize() && newCompletionCount >= requiredCompletions) {
            grantGrandPrize(player);
            playerData.setHasReceivedTreasureGrandPrize(true);
        }
    }

    private void grantPerCompletionReward(Player player) {
        ConfigurationSection rewardSection = plugin.getConfig().getConfigurationSection("treasure-hunt.per-completion-reward");
        if (rewardSection == null) {
            plugin.getLogger().warning("A seção 'treasure-hunt.per-completion-reward' não foi encontrada no config.yml. Nenhuma recompensa será dada.");
            return;
        }

        // Concede Totens
        double totems = rewardSection.getDouble("reward-totems", 0);
        if (totems > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(player, totems);
            player.sendMessage(ChatColor.GREEN + "Você recebeu " + ChatColor.YELLOW + totems + " Totens" + ChatColor.GREEN + " como recompensa!");
        }

        // Concede o Item
        grantItemFromSection(player, rewardSection, "Você também recebeu um item valioso!");
    }

    private void grantGrandPrize(Player player) {
        ConfigurationSection prizeSection = plugin.getConfig().getConfigurationSection("treasure-hunt.grand-prize");
        if (prizeSection == null) return;

        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "GRANDE PRÊMIO!");
        player.sendMessage(ChatColor.YELLOW + "Por sua dedicação incansável, você forjou um item lendário!");

        grantItemFromSection(player, prizeSection, null);
    }

    private void grantItemFromSection(Player player, ConfigurationSection section, String successMessage) {
        ConfigurationSection itemSection = section.getConfigurationSection("reward-item-data");
        if (itemSection == null) return;

        try {
            Material material = Material.valueOf(itemSection.getString("material", "DIAMOND").toUpperCase());
            ItemStack rewardItem = new ItemStack(material, itemSection.getInt("amount", 1));
            ItemMeta meta = rewardItem.getItemMeta();
            if (meta != null) {
                if (itemSection.contains("name")) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemSection.getString("name")));
                if (itemSection.contains("lore")) {
                    List<String> lore = new ArrayList<>();
                    itemSection.getStringList("lore").forEach(line -> lore.add(ChatColor.translateAlternateColorCodes('&', line)));
                    meta.setLore(lore);
                }
                if (itemSection.isList("enchantments") && !itemSection.getStringList("enchantments").isEmpty()) {
                    // Adiciona um brilho genérico e o esconde, ou usa o encantamento especificado
                    meta.addEnchant(Enchantment.LURE, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                rewardItem.setItemMeta(meta);
            }
            player.getInventory().addItem(rewardItem);
            if (successMessage != null && !successMessage.isEmpty()) {
                player.sendMessage(ChatColor.GREEN + successMessage);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao criar item de recompensa da caça ao tesouro: " + e.getMessage());
        }
    }
}