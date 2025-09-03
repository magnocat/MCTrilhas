package com.magnocat.mctrilhas.data;

import com.magnocat.mctrilhas.badges.BadgeType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Armazena os dados de um jogador, incluindo insígnias e progresso.
 */
public class PlayerData {

    private final UUID playerUUID;
    private final List<String> earnedBadges;
    private final Map<BadgeType, Double> progressMap;
    private final Set<String> visitedBiomes; // Armazena os biomas únicos visitados.
    private boolean progressMessagesDisabled;
    private long lastDailyRewardTime;

    public PlayerData(UUID playerUUID, List<String> earnedBadges, Map<BadgeType, Double> progressMap, Set<String> visitedBiomes, boolean progressMessagesDisabled, long lastDailyRewardTime) {
        this.playerUUID = playerUUID;
        this.earnedBadges = earnedBadges;
        this.progressMap = progressMap;
        this.visitedBiomes = visitedBiomes;
        this.progressMessagesDisabled = progressMessagesDisabled;
        this.lastDailyRewardTime = lastDailyRewardTime;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public boolean hasBadge(String badgeId) {
        return earnedBadges.contains(badgeId.toLowerCase());
    }

    public double getProgress(BadgeType type) {
        return progressMap.getOrDefault(type, 0.0);
    }

    public void addProgress(BadgeType type, double amount) {
        progressMap.merge(type, amount, Double::sum);
    }

    public List<String> getEarnedBadges() {
        return earnedBadges;
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
}