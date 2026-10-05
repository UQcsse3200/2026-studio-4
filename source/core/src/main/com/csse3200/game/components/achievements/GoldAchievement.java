package com.csse3200.game.components.achievements;

/** Unlocks once total gold collected reaches a target. */
public class GoldAchievement extends Achievement {
    private final int target;

    public GoldAchievement(int target, String name) {
        super(name);
        this.target = target;
    }

    @Override
    public boolean onGoldChanged(int totalGold) {
        return totalGold >= target && unlock();
    }
}