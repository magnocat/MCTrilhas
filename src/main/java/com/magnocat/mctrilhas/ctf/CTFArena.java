package com.magnocat.mctrilhas.ctf;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Representa uma arena de Capture The Flag, contendo todas as suas configurações e localizações.
 * Esta classe é um objeto de dados imutável (POJO).
 */
public class CTFArena {

    private final String id;
    private final String name;
    private final int minPlayers;
    private final int maxPlayers;
    private final int scoreToWin;
    private final int gameDurationSeconds;
    private final int countdownSeconds;
    private final int flagResetSeconds;
    private final Location lobbyPoint;
    private final Location redSpawn;
    private final Location blueSpawn;
    private final Location redFlagLocation;
    private final Location blueFlagLocation;

    public CTFArena(String id, String name, int minPlayers, int maxPlayers, int scoreToWin, int gameDurationSeconds, int countdownSeconds, int flagResetSeconds, Location lobbyPoint, Location redSpawn, Location blueSpawn, Location redFlagLocation, Location blueFlagLocation) {
        this.id = id;
        this.name = name;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.scoreToWin = scoreToWin;
        this.gameDurationSeconds = gameDurationSeconds;
        this.countdownSeconds = countdownSeconds;
        this.flagResetSeconds = flagResetSeconds;
        this.lobbyPoint = lobbyPoint;
        this.redSpawn = redSpawn;
        this.blueSpawn = blueSpawn;
        this.redFlagLocation = redFlagLocation;
        this.blueFlagLocation = blueFlagLocation;
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getScoreToWin() {
        return scoreToWin;
    }

    public int getGameDurationSeconds() {
        return gameDurationSeconds;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public int getFlagResetSeconds() {
        return flagResetSeconds;
    }

    public Location getLobbyPoint() {
        return lobbyPoint;
    }

    public Location getRedSpawn() {
        return redSpawn;
    }

    public Location getBlueSpawn() {
        return blueSpawn;
    }

    public Location getRedFlagLocation() {
        return redFlagLocation;
    }

    public Location getBlueFlagLocation() {
        return blueFlagLocation;
    }

    // --- Métodos Utilitários ---

    /**
     * Cria uma instância de CTFArena a partir de uma seção de configuração do YAML.
     *
     * @param id      O ID da arena (a chave no YAML).
     * @param section A ConfigurationSection correspondente a essa arena.
     * @return Uma nova instância de CTFArena, ou null se a configuração for inválida.
     */
    public static CTFArena fromConfig(String id, ConfigurationSection section) {
        String name = section.getString("name", "Arena CTF");
        int minPlayers = section.getInt("min-players", 2);
        int maxPlayers = section.getInt("max-players", 16);
        int scoreToWin = section.getInt("score-to-win", 3);
        int gameDurationSeconds = section.getInt("game-duration-seconds", 600); // Padrão de 10 minutos
        int countdownSeconds = section.getInt("countdown-seconds", 10); // Padrão de 10 segundos
        int flagResetSeconds = section.getInt("flag-reset-seconds", 30); // Padrão de 30 segundos

        Location lobby = parseLocation(section.getString("locations.lobby"));
        Location redSpawn = parseLocation(section.getString("locations.red-spawn"));
        Location blueSpawn = parseLocation(section.getString("locations.blue-spawn"));
        Location redFlag = parseLocation(section.getString("locations.red-flag"));
        Location blueFlag = parseLocation(section.getString("locations.blue-flag"));

        if (lobby == null || redSpawn == null || blueSpawn == null || redFlag == null || blueFlag == null) {
            Bukkit.getLogger().severe("[MCTrilhas-CTF] Falha ao carregar a arena '" + id + "'. Uma ou mais localizações são inválidas.");
            return null;
        }

        return new CTFArena(id, name, minPlayers, maxPlayers, scoreToWin, gameDurationSeconds, countdownSeconds, flagResetSeconds, lobby, redSpawn, blueSpawn, redFlag, blueFlag);
    }

    public static Location parseLocation(String locString) {
        if (locString == null || locString.isEmpty()) return null;
        String[] parts = locString.split(",");
        if (parts.length < 4) return null;
        World world = Bukkit.getWorld(parts[0].trim().replace(".", ","));
        if (world == null) return null;
        try {
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            float yaw = (parts.length > 4) ? Float.parseFloat(parts[4].trim()) : 0f;
            float pitch = (parts.length > 5) ? Float.parseFloat(parts[5].trim()) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}