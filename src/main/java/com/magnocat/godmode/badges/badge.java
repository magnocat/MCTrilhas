package com.magnocat.godmode.badges;

public class Badge {
    private final String id;
    private final String name;
    private final String description;
    private final int rewardTotems;
    private final String rewardItem;
    private final int rewardAmount;
    private final String rewardRegion;

    public Badge(String id, String name, String description, int rewardTotems, String rewardItem, int rewardAmount, String rewardRegion) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rewardTotems = rewardTotems;
        this.rewardItem = rewardItem;
        this.rewardAmount = rewardAmount;
        this.rewardRegion = rewardRegion;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getRewardTotems() { return rewardTotems; }
    public String getRewardItem() { return rewardItem; }
    public int getRewardAmount() { return rewardAmount; }
    public String getRewardRegion() { return rewardRegion; }
}
