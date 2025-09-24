package com.magnocat.mctrilhas.duels;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Representa uma única arena de duelo com suas propriedades.
 */
public class DuelArena {

    private final String id;
    private final Location pos1;
    private final Location pos2;
    private final Location spectatorSpawn;

    public DuelArena(String id, Location pos1, Location pos2, Location spectatorSpawn) {
        this.id = id;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.spectatorSpawn = spectatorSpawn;
    }

    public String getId() { return id; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public Location getSpectatorSpawn() { return spectatorSpawn; }

    /**
     * Cria uma DuelArena a partir de uma ConfigurationSection.
     * @param id O ID da arena.
     * @param section A seção de configuração desta arena.
     * @return Um novo objeto DuelArena, ou null se a configuração for inválida.
     */
    public static DuelArena fromConfig(String id, ConfigurationSection section) {
        if (section == null) return null;

        Location pos1 = parseLocation(section.getString("pos1"));
        Location pos2 = parseLocation(section.getString("pos2"));
        Location spec = parseLocation(section.getString("spectator-spawn"));

        if (pos1 == null || pos2 == null || spec == null) {
            Bukkit.getLogger().warning("[Duels] Arena '" + id + "' tem configurações de localização inválidas e não foi carregada.");
            return null;
        }
        return new DuelArena(id, pos1, pos2, spec);
    }

    public static Location parseLocation(String locString) {
        if (locString == null || locString.isEmpty()) return null;
        String[] parts = locString.split(",");
        if (parts.length != 6) return null;
        try {
            World world = Bukkit.getWorld(parts[0].trim());
            if (world == null) return null;
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float yaw = Float.parseFloat(parts[4].trim());
            float pitch = Float.parseFloat(parts[5].trim());
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}