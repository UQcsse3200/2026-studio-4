package com.csse3200.game.components.achievements;

import com.csse3200.game.components.Component;
import com.csse3200.game.entities.Entity;
import com.csse3200.game.services.AchievementService;
import com.csse3200.game.services.ServiceLocator;

public class AchievementProgressComponent extends Component {
  private static final String DAMAGE_TAKEN = "damageTaken";
  public static final String GOLD_PICKED_UP = "goldPickedUp";

  @Override
  public void create() {
    Entity player = entity;
    AchievementService achievements = ServiceLocator.getAchievementService();

    if (achievements == null) {
      return;
    }

    player
        .getEvents()
        .addListener(
            DAMAGE_TAKEN,
            (Entity attacker, Integer lost, Integer remaining) -> {
              AchievementContext context = new AchievementContext();
              context.playerDamaged = true;
              achievements.update(context);
            });

    player
        .getEvents()
        .addListener(
            GOLD_PICKED_UP,
            (Integer amount) -> {
              AchievementContext context = new AchievementContext();
              context.goldCollected = amount;
              achievements.update(context);
            });
  }
}
