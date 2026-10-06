package com.csse3200.game.services;

import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.rooms.configs.EnemySpawnConfig.EnemyType;
import com.csse3200.game.events.EventHandler;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AchievementService {
  private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

  private final List<Achievement> achievements = new ArrayList<>();
  private final EventHandler eventHandler = new EventHandler();

  public void register(Achievement achievement) {
    achievements.add(achievement);
  }

  public void notifyEnemyDied(EnemyType type) {
    dispatch(a -> a.onEnemyDied(type));
  }

  public void notifyPlayerDamaged() {
    dispatch(Achievement::onPlayerDamaged);
  }

  public void notifyDungeonCompleted(String dungeonId) {
    dispatch(a -> a.onDungeonCompleted(dungeonId));
  }

  public void notifyDungeonEntered(String dungeonId) {
    dispatch(a -> a.onDungeonEntered(dungeonId));
  }

  public void notifyDungeonTimeElapsed(String dungeonId, float seconds) {
    dispatch(a -> a.onDungeonTimeElapsed(dungeonId, seconds));
  }

  public void notifyGoldChanged(int totalGold) {
    dispatch(a -> a.onGoldChanged(totalGold));
  }

  private void dispatch(java.util.function.Predicate<Achievement> hook) {
    for (Achievement a : achievements) {
      if (hook.test(a)) {
        logger.info("Achievement unlocked: {}", a.getName());
        eventHandler.trigger("achievementUnlocked", a.getName());
      }
    }
  }

  public EventHandler getEvents() {
    return eventHandler;
  }

  public List<Achievement> getAchievements() {
    return achievements;
  }
}
