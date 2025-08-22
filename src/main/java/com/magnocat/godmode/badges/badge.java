package com.magnocat.godmode.badges;

/**
 * Um record imutável que representa uma insígnia no plugin.
 * Records geram automaticamente construtores, getters, equals(), hashCode() e toString().
 */
public record Badge(
        String id,
        String name,
        String description,
        int rewardTotems,
        String rewardItem,
        int rewardAmount,
        String rewardRegion,
        int requiredProgress
) {}
