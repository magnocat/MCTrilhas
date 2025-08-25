package com.magnocat.godmode.storage;

import com.magnocat.godmode.GodModePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages persistent storage for player-placed blocks using Chunk PersistentDataContainers.
 * This ensures that the data survives server restarts.
 */
public class BlockPersistenceManager {

    private final GodModePlugin plugin;
    private final NamespacedKey placedBlocksKey;

    public BlockPersistenceManager(GodModePlugin plugin) {
        this.plugin = plugin;
        // A unique key to identify our data within the chunk's storage.
        this.placedBlocksKey = new NamespacedKey(plugin, "player_placed_blocks");
    }

    /**
     * Marks a block as player-placed in its chunk's persistent data.
     *
     * @param block The block to mark.
     */
    public void markBlockAsPlayerPlaced(Block block) {
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        Set<String> placedBlocks = getPlacedBlocks(container);
        placedBlocks.add(getBlockKey(block));
        savePlacedBlocks(placedBlocks, container);
    }

    /**
     * Checks if a block is marked as player-placed and removes the mark if it is.
     * This is called when a block is broken.
     *
     * @param block The block to check and unmark.
     * @return true if the block was player-placed, false otherwise.
     */
    public boolean isPlayerPlaced(Block block) {
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        if (!container.has(placedBlocksKey, PersistentDataType.STRING)) {
            return false;
        }

        Set<String> placedBlocks = getPlacedBlocks(container);
        String blockKey = getBlockKey(block);

        if (placedBlocks.contains(blockKey)) {
            // The block was player-placed. Remove it from the list to prevent the data from growing indefinitely.
            placedBlocks.remove(blockKey);
            savePlacedBlocks(placedBlocks, container);
            return true;
        }

        return false;
    }

    private Set<String> getPlacedBlocks(PersistentDataContainer container) {
        String existingData = container.getOrDefault(placedBlocksKey, PersistentDataType.STRING, "");
        if (existingData.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(existingData.split(";")));
    }

    private void savePlacedBlocks(Set<String> placedBlocks, PersistentDataContainer container) {
        String dataToSave = String.join(";", placedBlocks);
        container.set(placedBlocksKey, PersistentDataType.STRING, dataToSave);
    }

    // Creates a unique key for a block within its chunk (e.g., "5_64_12").
    private String getBlockKey(Block block) {
        return (block.getX() & 15) + "_" + block.getY() + "_" + (block.getZ() & 15);
    }
}