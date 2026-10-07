package com.csse3200.game.services;

import com.csse3200.game.components.achievements.Achievement;
import com.csse3200.game.components.achievements.AchievementContext;
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
}
