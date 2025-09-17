package com.magnocat.mctrilhas.ctf;

import com.magnocat.mctrilhas.MCTrilhasPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Gerencia o estado e a localização de uma única bandeira no jogo CTF.
 */
public class CTFFlag {

    private final MCTrilhasPlugin plugin;
    private final CTFGame game;
    private final TeamColor teamColor;
    private final Location baseLocation;
    private FlagState state;
    private Location currentLocation;
    private UUID carrier;
    private BukkitTask resetTask;

    public CTFFlag(MCTrilhasPlugin plugin, CTFGame game, TeamColor teamColor, Location baseLocation) {
        this.plugin = plugin;
        this.game = game;
        this.teamColor = teamColor;
        this.baseLocation = baseLocation;
        this.reset();
    }

    public void pickUp(Player player) {
        removeFlagBlock();
        cancelResetTask();
        this.state = FlagState.CARRIED;
        this.carrier = player.getUniqueId();
        this.currentLocation = null; // A localização agora é a do jogador
    }

    public void drop(Location dropLocation) {
        this.state = FlagState.DROPPED;
        this.carrier = null;
        this.currentLocation = dropLocation;
        placeFlagBlock();
        startResetTimer();
    }

    public void reset() {
        removeFlagBlock();
        cancelResetTask();
        this.state = FlagState.AT_BASE;
        this.carrier = null;
        this.currentLocation = baseLocation;
        placeFlagBlock();
    }

    private void startResetTimer() {
        cancelResetTask(); // Garante que não haja tarefas duplicadas
        resetTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (state == FlagState.DROPPED) {
                    reset();
                    game.getScoreboard().updateForAllPlayers();
                    game.playSoundForAll(org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                    game.broadcastMessage(teamColor.getChatColor() + "A bandeira do time " + teamColor.getDisplayName() + " voltou para a base!");
                }
            }
        }.runTaskLater(plugin, game.getArena().getFlagResetSeconds() * 20L);
    }

    private void cancelResetTask() {
        if (resetTask != null) {
            resetTask.cancel();
            resetTask = null;
        }
    }

    /**
     * Coloca o bloco de estandarte no mundo na localização atual da bandeira.
     */
    public void placeFlagBlock() {
        if (currentLocation != null) {
            currentLocation.getBlock().setType(teamColor.getBannerMaterial());
        }
    }

    /**
     * Remove o bloco de estandarte do mundo.
     */
    public void removeFlagBlock() {
        if (currentLocation != null && currentLocation.getBlock().getType().name().endsWith("_BANNER")) {
            currentLocation.getBlock().setType(Material.AIR);
        }
    }

    // --- Getters ---

    public TeamColor getTeamColor() { return teamColor; }
    public Location getBaseLocation() { return baseLocation; }
    public FlagState getState() { return state; }
    public Location getCurrentLocation() { return currentLocation; }
    public UUID getCarrier() { return carrier; }

    // --- Métodos de verificação de estado ---

    public boolean isCarried() { return state == FlagState.CARRIED; }
    public boolean isAtBase() { return state == FlagState.AT_BASE; }
    public boolean isDropped() { return state == FlagState.DROPPED; }
}