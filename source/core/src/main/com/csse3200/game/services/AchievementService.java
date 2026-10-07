package com.csse3200.game.services;

import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementContext;
import com.csse3200.game.events.EventHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AchievementService {
  private static final Logger logger = LoggerFactory.getLogger(AchievementService.class);

  private final List<Achievement> achievements = new ArrayList<>();
  private final EventHandler eventHandler = new EventHandler();

  public void register(Achievement achievement) {
    achievements.add(achievement);
  }

  public void update(AchievementContext context) {
    for (Achievement a : achievements) {
      if (a.update(context)) {
        dispatch(a);
      }
    }
  }

  private void dispatch(Achievement a) {
    logger.info("Achievement unlocked: {}", a.getName());
    eventHandler.trigger("achievementUnlocked", a.getName());
  }

  public EventHandler getEvents() {
    return eventHandler;
  }

  public List<Achievement> getAchievements() {
    return achievements;
  }

  public List<String> getUnlockedAchievementNames() {
    return achievements.stream().filter(Achievement::isUnlocked).map(Achievement::getName).toList();
  }

  public Map<String, Float> getAchievementProgress() {
    Map<String, Float> progress = new HashMap<>();

    for (Achievement achievement : achievements) {
      progress.put(achievement.getName(), achievement.getProgress());
    }

    return progress;
  }

  public void restoreState(List<String> unlockedNames, Map<String, Float> savedProgress) {

    if (unlockedNames == null) {
      unlockedNames = List.of();
    }

    if (savedProgress == null) {
      savedProgress = Map.of();
    }

    for (Achievement achievement : achievements) {
      boolean unlocked = unlockedNames.contains(achievement.getName());
      float progress = savedProgress.getOrDefault(achievement.getName(), 0f);

      achievement.restoreState(unlocked, progress);
    }
  }
}
