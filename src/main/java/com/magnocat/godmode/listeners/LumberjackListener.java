package com.magnocat.godmode.listeners;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.magnocat.godmode.GodModePlugin;
import com.magnocat.godmode.badges.BadgeType;

public class LumberjackListener implements Listener {

    private final GodModePlugin plugin;

    // Caches para performance
    private final Set<Tag<Material>> trackedBlockTags = new HashSet<>();
    private final Set<Material> trackedMaterials = new HashSet<>();

    public LumberjackListener(GodModePlugin plugin) {
        this.plugin = plugin;
        loadTrackedItems();
    }

    /**
     * Loads the materials and tags to be tracked from the config.yml.
     * This allows server administrators to customize what counts for the lumberjack badge.
     * This method should be called on plugin startup and from your reload command.
     */
    public void loadTrackedItems() {
        // Limpa os caches para garantir dados novos ao recarregar
        trackedBlockTags.clear();
        trackedMaterials.clear();

        // Usa Tag.LOGS como padrão se a config não existir, para manter compatibilidade
        List<String> itemsToTrack = plugin.getConfig().getStringList("badges.lumberjack.tracked-items");
        if (itemsToTrack == null || itemsToTrack.isEmpty()) {
            trackedBlockTags.add(Tag.LOGS);
            plugin.getLogger().info("LumberjackListener: Nenhum item customizado encontrado. Usando a tag LOGS como padrão.");
            return;
        }

        for (String item : itemsToTrack) {
            if (item.toUpperCase().startsWith("TAG:")) {
                String tagName = item.substring(4).toLowerCase();
                // O registro de tags do Bukkit usa o namespace 'minecraft' para tags vanilla
                Tag<Material> tag = plugin.getServer().getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(tagName), Material.class);
                if (tag != null) {
                    trackedBlockTags.add(tag);
                } else {
                    plugin.getLogger().warning("[LumberjackListener] Tag de bloco inválida no config.yml: " + tagName);
                }
            } else {
                try {
                    Material material = Material.valueOf(item.toUpperCase());
                    trackedMaterials.add(material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[LumberjackListener] Material inválido no config.yml: " + item);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        boolean shouldIncrement = false;
        if (trackedMaterials.contains(block.getType())) {
            shouldIncrement = true;
        } else {
            for (Tag<Material> tag : trackedBlockTags) {
                if (tag.isTagged(block.getType())) {
                    shouldIncrement = true;
                    break;
                }
            }
        }

        if (shouldIncrement) {
            plugin.getPlayerDataManager().addProgress(player, BadgeType.LUMBERJACK, 1);
        }
    }
}