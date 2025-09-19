package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import com.magnocat.mctrilhas.data.PlayerCTFStats;
import com.magnocat.mctrilhas.utils.ItemFactory;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class CTFMilestoneManager {

    private final MCTrilhasPlugin plugin;
    private final Map<String, List<CTFMilestone>> milestones = new HashMap<>();

    public CTFMilestoneManager(MCTrilhasPlugin plugin) {
        this.plugin = plugin;
        loadMilestones();
    }

    private void loadMilestones() {
        File milestonesFile = new File(plugin.getDataFolder(), "ctf_milestones.yml");
        if (!milestonesFile.exists()) {
            plugin.saveResource("ctf_milestones.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(milestonesFile);

        ConfigurationSection milestonesSection = config.getConfigurationSection("milestones");
        if (milestonesSection == null) return;

        for (String statType : milestonesSection.getKeys(false)) {
            List<CTFMilestone> milestoneList = new ArrayList<>();
            ConfigurationSection statSection = milestonesSection.getConfigurationSection(statType);
            if (statSection == null) continue;

            // Iterar sobre as chaves numéricas (0, 1, 2...) de cada tipo de estatística
            for (String key : statSection.getKeys(false)) {
                ConfigurationSection milestoneSection = statSection.getConfigurationSection(key);
                if (milestoneSection == null) continue;

                String id = milestoneSection.getString("id");
                String name = milestoneSection.getString("name");
                int required = milestoneSection.getInt("required");
                ConfigurationSection itemSection = milestoneSection.getConfigurationSection("reward-item");
                if (id != null && name != null && itemSection != null) {
                    milestoneList.add(new CTFMilestone(id, name, required, itemSection));
                }
            }
            // Ordena por requisito, do menor para o maior
            milestoneList.sort(Comparator.comparingInt(CTFMilestone::getRequired));
            this.milestones.put(statType.toLowerCase(), milestoneList);
        }
        plugin.getLogger().info("[CTF] " + milestones.values().stream().mapToInt(List::size).sum() + " marcos históricos (milestones) carregados.");
    }

    /**
     * Verifica e concede recompensas de marco histórico para um jogador com base em suas estatísticas.
     * @param player O jogador.
     * @param stats As estatísticas permanentes do jogador.
     */
    public void checkAndGrantMilestones(Player player, PlayerCTFStats stats) {
        checkStatMilestones(player, "kills", stats.getKills());
        checkStatMilestones(player, "wins", stats.getWins());
        checkStatMilestones(player, "flagCaptures", stats.getFlagCaptures());
    }

    private void checkStatMilestones(Player player, String statType, int currentValue) {
        List<CTFMilestone> milestoneTrack = milestones.get(statType);
        if (milestoneTrack == null) return;

        for (CTFMilestone milestone : milestoneTrack) {
            if (currentValue >= milestone.getRequired() && !plugin.getPlayerDataManager().hasClaimedCtfMilestone(player.getUniqueId(), milestone.getId())) {
                grantMilestoneReward(player, milestone);
                plugin.getPlayerDataManager().addClaimedCtfMilestone(player.getUniqueId(), milestone.getId());
            }
        }
    }

    private void grantMilestoneReward(Player player, CTFMilestone milestone) {
        ItemStack rewardItem = ItemFactory.createFromConfig(milestone.getRewardItemSection());
        if (rewardItem != null) {
            player.getInventory().addItem(rewardItem);
        } else {
            plugin.getLogger().severe("Erro ao criar item de recompensa para o marco histórico '" + milestone.getId() + "'. Verifique a configuração em ctf_milestones.yml.");
        }

        player.sendMessage(ChatColor.GOLD + "---------------------------------");
        player.sendMessage(ChatColor.AQUA + "  Marco Histórico Alcançado!");
        player.sendMessage(" ");
        player.sendMessage(ChatColor.WHITE + "  Você alcançou: " + ChatColor.YELLOW + milestone.getName());
        player.sendMessage(ChatColor.WHITE + "  Você recebeu um item especial como recompensa!");
        player.sendMessage(ChatColor.GOLD + "---------------------------------");
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
    }

    /**
     * Classe interna para representar um único marco histórico.
     */
    private static class CTFMilestone {
        private final String id;
        private final String name;
        private final int required;
        private final ConfigurationSection rewardItemSection;

        public CTFMilestone(String id, String name, int required, ConfigurationSection rewardItemSection) {
            this.id = id;
            this.name = name;
            this.required = required;
            this.rewardItemSection = rewardItemSection;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public int getRequired() {
            return required;
        }

        public ConfigurationSection getRewardItemSection() {
            return rewardItemSection;
        }
    }
}