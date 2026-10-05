package com.csse3200.game.services;

import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.events.EventHandler;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tracks all achievements for the current run. Registered once at game start. */
public class AchievementService {
  private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

  private final List<Achievement> achievements = new ArrayList<>();
  private final EventHandler eventHandler = new EventHandler();

  public void register(Achievement achievement) {
    achievements.add(achievement);
  }

  public void notifyEnemyDied(EnemyType type) {
    for (Achievement a : achievements) {
      if (a.onEnemyDied(type)) {
        unlock(a);
      }
    }
  }

  public void notifyPlayerDamaged() {
    for (Achievement a : achievements) {
      a.onPlayerDamaged();
    }
  }

  private void unlock(Achievement a) {
    logger.info("Achievement unlocked: {}", a.getName());
    eventHandler.trigger("achievementUnlocked", a.getName());
  }

  public EventHandler getEvents() {
    return eventHandler;
  }

  public List<Achievement> getAchievements() {
    return achievements;
  }
}
