package com.csse3200.game.components.achievements;

import java.util.EnumSet;

public class AchievementsFactory {

    public static Achievement build(AchievementConfig c) {
        return switch (c.type) {
            case "dungeonClear" -> new DungeonClearAchievement(c.dungeonId, c.name);
            case "dungeonReached" -> new DungeonReachedAchievement(c.dungeonId, c.name);
            case "enemyKillCount" -> new TypeKillAchievement(c.enemyType, c.target, c.name);
            case "enemySet" -> new EnemySetAchievement(EnumSet.copyOf(c.enemyTypes), c.name);
            case "killStreak" -> new KillStreakAchievement(c.target, c.name);
            case "singleHitDamage" -> new SingleHitDamageAchievement(c.target, c.name);
            case "lowHealthBossKill" -> new LowHealthBossKillAchievement(c.healthFraction, c.name);
            case "speedrun" -> new SpeedrunAchievement(c.dungeonId, c.maxSeconds, c.name);
            case "gold" -> new GoldAchievement(c.target, c.name);
            default -> throw new IllegalArgumentException("Unknown achievement type: " + c.type);
        };
    }
}
