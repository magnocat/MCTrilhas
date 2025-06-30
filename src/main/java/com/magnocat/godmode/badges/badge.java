package com.magnocat.godmode.badges;

public class Badge {
    private final String id;
    private final String name;
    private final String description;
    private final int rewardTotems;
    private final String rewardItem;
    private final int rewardAmount;
    private final String rewardRegion;
    private final int requiredProgress;

    public Badge(String id, String name, String description, int rewardTotems, String rewardItem, int rewardAmount, String rewardRegion, int requiredProgress) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rewardTotems = rewardTotems;
        this.rewardItem = rewardItem;
        this.rewardAmount = rewardAmount;
        this.rewardRegion = rewardRegion;
        this.requiredProgress = requiredProgress;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getRewardTotems() { return rewardTotems; }
    public String getRewardItem() { return rewardItem; }
    public int getRewardAmount() { return rewardAmount; }
    public String getRewardRegion() { return rewardRegion; }
    public int getRequiredProgress() { return requiredProgress; }
}
