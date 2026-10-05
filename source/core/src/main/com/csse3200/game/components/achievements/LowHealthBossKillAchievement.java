package com.csse3200.game.components.achievements;

/** Unlocks the first time the final boss is killed while the player is at/under a health fraction. */
public class LowHealthBossKillAchievement extends Achievement {
    private final float maxFraction;

    public LowHealthBossKillAchievement(float maxFraction, String name) {
        super(name);
        this.maxFraction = maxFraction;
    }

    @Override
    public boolean onBossKilledAtLowHealth(float healthFraction) {
        return healthFraction <= maxFraction && unlock();
    }
}