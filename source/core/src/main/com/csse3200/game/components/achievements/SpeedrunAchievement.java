package com.csse3200.game.components.achievements;

/** Unlocks if a dungeon is completed within a time limit (checked at dungeon-complete time). */
public class SpeedrunAchievement extends Achievement {
    private final String dungeonId;
    private final float maxSeconds;
    private float lastKnownTime = Float.MAX_VALUE;

    public SpeedrunAchievement(String dungeonId, float maxSeconds, String name) {
        super(name);
        this.dungeonId = dungeonId;
        this.maxSeconds = maxSeconds;
    }

    @Override
    public boolean onDungeonTimeElapsed(String id, float seconds) {
        if (dungeonId.equals(id)) {
            lastKnownTime = seconds;
        }
        return false; // unlocking decided at onDungeonCompleted, once we know it was actually cleared
    }

    @Override
    public boolean onDungeonCompleted(String completedId) {
        return dungeonId.equals(completedId) && lastKnownTime <= maxSeconds && unlock();
    }
}