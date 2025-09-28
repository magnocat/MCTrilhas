package com.magnocat.mctrilhas.data;

import com.magnocat.mctrilhas.badges.BadgeType;
import com.magnocat.mctrilhas.duels.PlayerDuelStats;
import com.magnocat.mctrilhas.pet.PetData;
import com.magnocat.mctrilhas.ranks.Rank;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Armazena os dados de um jogador, incluindo insígnias e progresso.
 */
public class PlayerData {

    private final UUID playerUUID;
    private final Map<String, Long> earnedBadgesMap; // Armazena o ID da insígnia e o timestamp da conquista.
    private final Map<BadgeType, Double> progressMap;
    private final Set<String> visitedBiomes; // Armazena os biomas únicos visitados.
    private boolean progressMessagesDisabled;
    private long lastDailyRewardTime;
    private Rank rank;
    private long activePlaytimeTicks; // Armazena o tempo de jogo ativo em ticks.
    private transient Location lastAfkCheckLocation; // Não é salvo no arquivo.
    private List<String> treasureHuntLocations; // Lista de locais para a caça ao tesouro atual (formato "world,x,y,z").
    private int currentTreasureHuntStage; // Estágio atual da caça ao tesouro.
    private int treasureHuntsCompleted; // Contador de caças ao tesouro concluídas.
    private boolean hasReceivedTreasureGrandPrize; // Flag para garantir que o grande prêmio seja dado apenas uma vez.
    private final Set<String> claimedCtfMilestones;
    private PetData petData;
    private String webAccessToken;
    private final PlayerDuelStats duelStats;
    private UUID godfatherUUID; // UUID de quem apadrinhou este jogador

    public PlayerData(UUID playerUUID, Map<String, Long> earnedBadgesMap, Map<BadgeType, Double> progressMap, Set<String> visitedBiomes, boolean progressMessagesDisabled, long lastDailyRewardTime, Rank rank, long activePlaytimeTicks, List<String> treasureHuntLocations, int currentTreasureHuntStage, int treasureHuntsCompleted, boolean hasReceivedTreasureGrandPrize, Set<String> claimedCtfMilestones, PetData petData, String webAccessToken, PlayerDuelStats duelStats) {
        this.playerUUID = playerUUID;
        this.earnedBadgesMap = earnedBadgesMap;
        this.progressMap = progressMap;
        this.visitedBiomes = visitedBiomes;
        this.progressMessagesDisabled = progressMessagesDisabled;
        this.lastDailyRewardTime = lastDailyRewardTime;
        this.rank = rank;
        this.activePlaytimeTicks = activePlaytimeTicks;
        this.lastAfkCheckLocation = null;
        this.treasureHuntLocations = treasureHuntLocations;
        this.currentTreasureHuntStage = currentTreasureHuntStage;
        this.treasureHuntsCompleted = treasureHuntsCompleted;
        this.hasReceivedTreasureGrandPrize = hasReceivedTreasureGrandPrize;
        this.claimedCtfMilestones = claimedCtfMilestones;
        this.petData = petData;
        this.webAccessToken = webAccessToken;
        this.duelStats = duelStats;
        this.godfatherUUID = null; // Padrão é nulo
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public boolean hasBadge(String badgeId) {
        // Faz a verificação ignorando maiúsculas/minúsculas para ser mais robusto.
        return earnedBadgesMap.containsKey(badgeId.toLowerCase());
    }

    public double getProgress(BadgeType type) {
        return progressMap.getOrDefault(type, 0.0);
    }

    public void addProgress(BadgeType type, double amount) {
        progressMap.merge(type, amount, Double::sum);
    }

    public List<String> getEarnedBadges() {
        return new ArrayList<>(earnedBadgesMap.keySet());
    }

    /**
     * Retorna o mapa completo de insígnias com seus timestamps.
     * @return Um mapa de ID da insígnia para o timestamp da conquista.
     */
    public Map<String, Long> getEarnedBadgesMap() {
        return earnedBadgesMap;
    }

    public Map<BadgeType, Double> getProgressMap() {
        return progressMap;
    }

    public Set<String> getVisitedBiomes() {
        return visitedBiomes;
    }

    public boolean areProgressMessagesDisabled() {
        return progressMessagesDisabled;
    }

    public void setProgressMessagesDisabled(boolean disabled) {
        this.progressMessagesDisabled = disabled;
    }

    public long getLastDailyRewardTime() {
        return lastDailyRewardTime;
    }

    public void setLastDailyRewardTime(long lastDailyRewardTime) {
        this.lastDailyRewardTime = lastDailyRewardTime;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public long getActivePlaytimeTicks() {
        return activePlaytimeTicks;
    }

    public void addActivePlaytimeTicks(long ticks) {
        this.activePlaytimeTicks += ticks;
    }

    public Location getLastAfkCheckLocation() {
        return lastAfkCheckLocation;
    }

    public void setLastAfkCheckLocation(Location lastAfkCheckLocation) {
        this.lastAfkCheckLocation = lastAfkCheckLocation;
    }

    public List<String> getTreasureHuntLocations() {
        return treasureHuntLocations;
    }

    public void setTreasureHuntLocations(List<String> treasureHuntLocations) {
        this.treasureHuntLocations = treasureHuntLocations;
    }

    public int getCurrentTreasureHuntStage() {
        return currentTreasureHuntStage;
    }

    public void setCurrentTreasureHuntStage(int currentTreasureHuntStage) {
        this.currentTreasureHuntStage = currentTreasureHuntStage;
    }

    public int getTreasureHuntsCompleted() {
        return treasureHuntsCompleted;
    }

    public void setTreasureHuntsCompleted(int treasureHuntsCompleted) {
        this.treasureHuntsCompleted = treasureHuntsCompleted;
    }

    public boolean hasReceivedTreasureGrandPrize() {
        return hasReceivedTreasureGrandPrize;
    }

    public void setHasReceivedTreasureGrandPrize(boolean hasReceivedTreasureGrandPrize) {
        this.hasReceivedTreasureGrandPrize = hasReceivedTreasureGrandPrize;
    }

    public Set<String> getClaimedCtfMilestones() {
        return claimedCtfMilestones;
    }

    public String getWebAccessToken() {
        return webAccessToken;
    }

    public void setWebAccessToken(String webAccessToken) {
        this.webAccessToken = webAccessToken;
    }

    public PetData getPetData() {
        return petData;
    }

    public void setPetData(PetData petData) {
        this.petData = petData;
    }

    public PlayerDuelStats getDuelStats() {
        return duelStats;
    }

    public UUID getGodfatherUUID() {
        return godfatherUUID;
    }

    public void setGodfatherUUID(UUID godfatherUUID) {
        this.godfatherUUID = godfatherUUID;
    }
}